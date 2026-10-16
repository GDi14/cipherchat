-- Each account currently has a single active encryption device. Bind the published
-- Signal bundle to that device so clients can detect when private keys were lost.
alter table public.prekeys add column if not exists device_id text;

update public.prekeys as key_bundle
set device_id = profile.device_id
from public.profiles as profile
where profile.id = key_bundle.user_id
  and key_bundle.device_id is null;

alter table public.prekeys alter column device_id set not null;
