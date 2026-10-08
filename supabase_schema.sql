-- ==============================================================================
-- ShieldChat: End-to-End Encrypted Messenger SQL Schema for Supabase
-- Run this script in the Supabase SQL Editor.
-- ==============================================================================

-- 1. Profiles Table. Profile UUIDs stay stable; Supabase Auth identities are linked separately.
create table if not exists public.profiles (
    id uuid primary key,
    device_id text not null unique,
    auth_user_id uuid unique references auth.users(id) on delete cascade,
    display_name text not null,
    tag text not null unique,
    avatar_path text null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.profiles add column if not exists auth_user_id uuid references auth.users(id) on delete cascade;
create unique index if not exists idx_profiles_auth_user on public.profiles(auth_user_id) where auth_user_id is not null;

create index if not exists idx_profiles_tag on public.profiles(lower(tag));
create index if not exists idx_profiles_device on public.profiles(device_id);

-- 2. Prekeys Table (Signal Protocol Public Key Material for X3DH)
-- Private keys remain exclusively in device Android KeyStore / EncryptedSharedPreferences!
create table if not exists public.prekeys (
    user_id uuid primary key references public.profiles(id) on delete cascade,
    identity_key text not null,               -- Base64 encoded public identity key
    registration_id int not null,             -- Signal registration ID
    signed_prekey_id int not null,            -- Signed prekey ID
    signed_prekey text not null,               -- Base64 encoded signed prekey
    signed_prekey_signature text not null,     -- Base64 signature
    one_time_prekeys jsonb not null default '[]'::jsonb, -- Array of { id: int, key: base64 }
    updated_at timestamptz not null default now()
);

-- 3. Friend Requests Table
create table if not exists public.friend_requests (
    id uuid primary key default gen_random_uuid(),
    sender_id uuid not null references public.profiles(id) on delete cascade,
    receiver_id uuid not null references public.profiles(id) on delete cascade,
    status text not null check (status in ('pending', 'accepted', 'declined')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(sender_id, receiver_id)
);

create index if not exists idx_friend_req_sender on public.friend_requests(sender_id);
create index if not exists idx_friend_req_receiver on public.friend_requests(receiver_id);
create index if not exists idx_friend_req_status on public.friend_requests(status);

-- 4. Conversations Table (Direct & Group)
create table if not exists public.conversations (
    id uuid primary key default gen_random_uuid(),
    type text not null check (type in ('direct', 'group')),
    title text null,
    created_by uuid null references public.profiles(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.conversations add column if not exists created_by uuid references public.profiles(id) on delete set null;

-- 5. Conversation Members Table
create table if not exists public.conversation_members (
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    joined_at timestamptz not null default now(),
    primary key (conversation_id, user_id)
);

create index if not exists idx_conv_members_user on public.conversation_members(user_id);

-- 6. Messages Table (E2EE Ciphertext Relaying)
-- Plaintext is NEVER stored or relayed through Supabase.
create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    sender_id uuid not null references public.profiles(id) on delete cascade,
    recipient_id uuid null references public.profiles(id) on delete cascade, -- for direct messages
    message_type int not null default 3,       -- 2: PREKEY_TYPE (X3DH init), 3: WHISPER_TYPE (Double Ratchet), 7: SENDERKEY_TYPE (Groups)
    encrypted_body text not null,              -- Base64 libsignal ciphertext
    reply_to_id uuid null references public.messages(id) on delete set null,
    status text not null default 'sent' check (status in ('sending', 'sent', 'delivered', 'read')),
    created_at timestamptz not null default now()
);

create index if not exists idx_messages_conv on public.messages(conversation_id, created_at desc);
create index if not exists idx_messages_recipient on public.messages(recipient_id);

-- 7. Message Receipts Table (Delivery / Read Tracking)
create table if not exists public.message_receipts (
    message_id uuid not null references public.messages(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    status text not null check (status in ('delivered', 'read')),
    updated_at timestamptz not null default now(),
    primary key (message_id, user_id)
);

-- 8. Group Sender Keys Distribution Table
-- Stores 1:1 encrypted SenderKeyDistributionMessages so group members establish group sender keys.
create table if not exists public.group_sender_keys (
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    sender_id uuid not null references public.profiles(id) on delete cascade,
    recipient_id uuid not null references public.profiles(id) on delete cascade,
    encrypted_distribution_message text not null,
    iteration int not null default 0,
    updated_at timestamptz not null default now(),
    primary key (conversation_id, sender_id, recipient_id)
);

-- 9. Typing Status Table
create table if not exists public.typing_status (
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    is_typing boolean not null default false,
    updated_at timestamptz not null default now(),
    primary key (conversation_id, user_id)
);

-- FCM tokens stay private to the owner; only a server-side Edge Function reads all recipients.
create table if not exists public.device_tokens (
    device_id text primary key,
    user_id uuid not null references public.profiles(id) on delete cascade,
    fcm_token text not null unique,
    updated_at timestamptz not null default now()
);

-- ==============================================================================
-- Realtime Setup
-- Enable Postgres Changes on the tables needed for live updates.
-- ==============================================================================
do $$
declare
    realtime_table text;
begin
    foreach realtime_table in array array[
        'profiles', 'friend_requests', 'conversations', 'conversation_members',
        'messages', 'message_receipts', 'group_sender_keys', 'typing_status'
    ] loop
        if not exists (
            select 1 from pg_publication_tables
            where pubname = 'supabase_realtime'
              and schemaname = 'public'
              and tablename = realtime_table
        ) then
            execute format('alter publication supabase_realtime add table public.%I', realtime_table);
        end if;
    end loop;
end $$;

-- ==============================================================================
-- Storage Bucket Setup (Private E2EE Media & Avatars)
-- ==============================================================================
insert into storage.buckets (id, name, public)
values ('chat-media', 'chat-media', false)
on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do nothing;

-- ==============================================================================
-- Row Level Security (RLS) Policies
-- All client table access uses Supabase Auth JWTs and policies resolve the stable profile UUID.
-- ==============================================================================
alter table public.profiles enable row level security;
alter table public.prekeys enable row level security;
alter table public.friend_requests enable row level security;
alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.messages enable row level security;
alter table public.message_receipts enable row level security;
alter table public.group_sender_keys enable row level security;
alter table public.typing_status enable row level security;
alter table public.device_tokens enable row level security;

-- Resolve the authenticated user to the app's stable profile UUID without exposing the mapping.
create or replace function public.current_profile_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
  select id from public.profiles where auth_user_id = auth.uid() limit 1
$$;

create or replace function public.is_conversation_member(target_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.conversation_members
    where conversation_id = target_conversation_id
      and user_id = public.current_profile_id()
  )
$$;

create or replace function public.is_conversation_creator(target_conversation_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.conversations
    where id = target_conversation_id
      and created_by = public.current_profile_id()
  )
$$;

revoke all on function public.current_profile_id() from public, anon;
revoke all on function public.is_conversation_member(uuid) from public, anon;
revoke all on function public.is_conversation_creator(uuid) from public, anon;
grant execute on function public.current_profile_id() to authenticated;
grant execute on function public.is_conversation_member(uuid) to authenticated;
grant execute on function public.is_conversation_creator(uuid) to authenticated;

-- Remove the former anon-access policies when this script is reapplied to an alpha project.
drop policy if exists "Allow read profiles" on public.profiles;
drop policy if exists "Allow insert profiles" on public.profiles;
drop policy if exists "Allow update profiles" on public.profiles;
drop policy if exists "Allow prekeys read" on public.prekeys;
drop policy if exists "Allow prekeys upsert" on public.prekeys;
drop policy if exists "Allow friend_requests read" on public.friend_requests;
drop policy if exists "Allow friend_requests insert" on public.friend_requests;
drop policy if exists "Allow friend_requests update" on public.friend_requests;
drop policy if exists "Allow conversations read" on public.conversations;
drop policy if exists "Allow conversations insert" on public.conversations;
drop policy if exists "Allow conversation_members read" on public.conversation_members;
drop policy if exists "Allow conversation_members insert" on public.conversation_members;
drop policy if exists "Allow messages read" on public.messages;
drop policy if exists "Allow messages insert" on public.messages;
drop policy if exists "Allow messages update" on public.messages;
drop policy if exists "Allow receipts read" on public.message_receipts;
drop policy if exists "Allow receipts upsert" on public.message_receipts;
drop policy if exists "Allow group_sender_keys read" on public.group_sender_keys;
drop policy if exists "Allow group_sender_keys upsert" on public.group_sender_keys;
drop policy if exists "Allow typing read" on public.typing_status;
drop policy if exists "Allow typing upsert" on public.typing_status;

drop policy if exists "profiles authenticated read" on public.profiles;
drop policy if exists "profiles owner insert" on public.profiles;
drop policy if exists "profiles owner update" on public.profiles;
create policy "profiles authenticated read" on public.profiles for select to authenticated using (true);
create policy "profiles owner insert" on public.profiles for insert to authenticated
  with check (auth_user_id = auth.uid());
create policy "profiles owner update" on public.profiles for update to authenticated
  using (auth_user_id = auth.uid()) with check (auth_user_id = auth.uid());

drop policy if exists "prekeys authenticated read" on public.prekeys;
drop policy if exists "prekeys owner write" on public.prekeys;
create policy "prekeys authenticated read" on public.prekeys for select to authenticated using (true);
create policy "prekeys owner write" on public.prekeys for all to authenticated
  using (user_id = public.current_profile_id()) with check (user_id = public.current_profile_id());

drop policy if exists "friend requests participants read" on public.friend_requests;
drop policy if exists "friend requests sender insert" on public.friend_requests;
drop policy if exists "friend requests recipient update" on public.friend_requests;
create policy "friend requests participants read" on public.friend_requests for select to authenticated
  using (sender_id = public.current_profile_id() or receiver_id = public.current_profile_id());
create policy "friend requests sender insert" on public.friend_requests for insert to authenticated
  with check (sender_id = public.current_profile_id());
create policy "friend requests recipient update" on public.friend_requests for update to authenticated
  using (receiver_id = public.current_profile_id()) with check (receiver_id = public.current_profile_id());

drop policy if exists "conversations members read" on public.conversations;
drop policy if exists "conversations authenticated create" on public.conversations;
create policy "conversations members read" on public.conversations for select to authenticated
  using (public.is_conversation_member(id));
create policy "conversations authenticated create" on public.conversations for insert to authenticated
  with check (created_by = public.current_profile_id());

drop policy if exists "members participants read" on public.conversation_members;
drop policy if exists "members own or inviter insert" on public.conversation_members;
create policy "members participants read" on public.conversation_members for select to authenticated
  using (public.is_conversation_member(conversation_id) or user_id = public.current_profile_id());
create policy "members own or inviter insert" on public.conversation_members for insert to authenticated
  with check (user_id = public.current_profile_id() or public.is_conversation_member(conversation_id)
    or public.is_conversation_creator(conversation_id));

drop policy if exists "messages conversation members read" on public.messages;
drop policy if exists "messages sender insert" on public.messages;
create policy "messages conversation members read" on public.messages for select to authenticated
  using (public.is_conversation_member(conversation_id));
create policy "messages sender insert" on public.messages for insert to authenticated
  with check (sender_id = public.current_profile_id() and public.is_conversation_member(conversation_id));

drop policy if exists "receipts conversation members read" on public.message_receipts;
drop policy if exists "receipts owner write" on public.message_receipts;
create policy "receipts conversation members read" on public.message_receipts for select to authenticated
  using (exists (select 1 from public.messages m where m.id = message_id and public.is_conversation_member(m.conversation_id)));
create policy "receipts owner write" on public.message_receipts for all to authenticated
  using (user_id = public.current_profile_id()) with check (user_id = public.current_profile_id());

drop policy if exists "sender keys participants read" on public.group_sender_keys;
drop policy if exists "sender keys sender write" on public.group_sender_keys;
create policy "sender keys participants read" on public.group_sender_keys for select to authenticated
  using (public.is_conversation_member(conversation_id));
create policy "sender keys sender write" on public.group_sender_keys for all to authenticated
  using (sender_id = public.current_profile_id()) with check (sender_id = public.current_profile_id());

drop policy if exists "typing participants read" on public.typing_status;
drop policy if exists "typing owner write" on public.typing_status;
create policy "typing participants read" on public.typing_status for select to authenticated
  using (public.is_conversation_member(conversation_id));
create policy "typing owner write" on public.typing_status for all to authenticated
  using (user_id = public.current_profile_id()) with check (user_id = public.current_profile_id());

drop policy if exists "device tokens owner all" on public.device_tokens;
create policy "device tokens owner all" on public.device_tokens for all to authenticated
  using (user_id = public.current_profile_id()) with check (user_id = public.current_profile_id());

revoke all on public.profiles, public.prekeys, public.friend_requests, public.conversations,
  public.conversation_members, public.messages, public.message_receipts, public.group_sender_keys,
  public.typing_status, public.device_tokens from anon;
grant select, insert, update on public.profiles to authenticated;
grant select, insert, update on public.prekeys to authenticated;
grant select, insert, update on public.friend_requests to authenticated;
grant select, insert on public.conversations to authenticated;
grant select, insert on public.conversation_members to authenticated;
grant select, insert on public.messages to authenticated;
grant select, insert, update on public.message_receipts to authenticated;
grant select, insert, update on public.group_sender_keys to authenticated;
grant select, insert, update, delete on public.typing_status to authenticated;
grant select, insert, update, delete on public.device_tokens to authenticated;

-- Storage RLS:
drop policy if exists "Allow storage select chat-media" on storage.objects;
drop policy if exists "Allow storage insert chat-media" on storage.objects;
drop policy if exists "Allow storage select avatars" on storage.objects;
drop policy if exists "Allow storage insert avatars" on storage.objects;
create policy "Authenticated members read chat media" on storage.objects for select to authenticated
  using (bucket_id = 'chat-media' and public.is_conversation_member(split_part(name, '/', 1)::uuid));
create policy "Authenticated members upload chat media" on storage.objects for insert to authenticated
  with check (bucket_id = 'chat-media'
    and public.is_conversation_member(split_part(name, '/', 1)::uuid)
    and split_part(name, '/', 2) = public.current_profile_id()::text);
create policy "Authenticated users read avatars" on storage.objects for select to authenticated
  using (bucket_id = 'avatars');
create policy "Authenticated users upload own avatar" on storage.objects for insert to authenticated
  with check (bucket_id = 'avatars' and name like public.current_profile_id()::text || '\_%');
