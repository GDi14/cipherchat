-- ==============================================================================
-- ShieldChat: End-to-End Encrypted Messenger SQL Schema for Supabase
-- Run this script in the Supabase SQL Editor.
-- ==============================================================================

-- 1. Profiles Table (Temporary Device-UUID Auth & Permanent User Store)
create table if not exists public.profiles (
    id uuid primary key,
    device_id text not null unique,
    display_name text not null,
    tag text not null unique,
    avatar_path text null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

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
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

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

-- ==============================================================================
-- Realtime Setup
-- Enable Postgres Changes on the tables needed for live updates.
-- ==============================================================================
alter publication supabase_realtime add table public.profiles;
alter publication supabase_realtime add table public.friend_requests;
alter publication supabase_realtime add table public.conversations;
alter publication supabase_realtime add table public.conversation_members;
alter publication supabase_realtime add table public.messages;
alter publication supabase_realtime add table public.message_receipts;
alter publication supabase_realtime add table public.group_sender_keys;
alter publication supabase_realtime add table public.typing_status;

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
-- Note: In this temporary UUID auth stage, requests use the Supabase Anon Key.
-- When full Supabase Auth + Google ID Token is activated, auth.uid() is enforced.
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

-- Permissive policies for anon key during temp auth; partitioned by table integrity
create policy "Allow read profiles" on public.profiles for select using (true);
create policy "Allow insert profiles" on public.profiles for insert with check (true);
create policy "Allow update profiles" on public.profiles for update using (true);

create policy "Allow prekeys read" on public.prekeys for select using (true);
create policy "Allow prekeys upsert" on public.prekeys for all using (true);

create policy "Allow friend_requests read" on public.friend_requests for select using (true);
create policy "Allow friend_requests insert" on public.friend_requests for insert with check (true);
create policy "Allow friend_requests update" on public.friend_requests for update using (true);

create policy "Allow conversations read" on public.conversations for select using (true);
create policy "Allow conversations insert" on public.conversations for insert with check (true);

create policy "Allow conversation_members read" on public.conversation_members for select using (true);
create policy "Allow conversation_members insert" on public.conversation_members for insert with check (true);

create policy "Allow messages read" on public.messages for select using (true);
create policy "Allow messages insert" on public.messages for insert with check (true);
create policy "Allow messages update" on public.messages for update using (true);

create policy "Allow receipts read" on public.message_receipts for select using (true);
create policy "Allow receipts upsert" on public.message_receipts for all using (true);

create policy "Allow group_sender_keys read" on public.group_sender_keys for select using (true);
create policy "Allow group_sender_keys upsert" on public.group_sender_keys for all using (true);

create policy "Allow typing read" on public.typing_status for select using (true);
create policy "Allow typing upsert" on public.typing_status for all using (true);

-- Storage RLS:
create policy "Allow storage select chat-media" on storage.objects for select using (bucket_id = 'chat-media');
create policy "Allow storage insert chat-media" on storage.objects for insert with check (bucket_id = 'chat-media');
create policy "Allow storage select avatars" on storage.objects for select using (bucket_id = 'avatars');
create policy "Allow storage insert avatars" on storage.objects for insert with check (bucket_id = 'avatars');
