# Alpha release setup

This build keeps Supabase as the database, authentication provider, realtime transport, and object store. Firebase is used only for Android push delivery (FCM). Compose screens added for alpha use Material 3 Expressive components.

## Google sign-in and Supabase Auth

1. Register the Android app in Firebase with package `com.gd.cipherchat`. In Google Cloud Console / Google Auth Platform, create/configure a **Web application** OAuth client and an **Android** OAuth client for that package. Add the SHA-1 fingerprint for the alpha/release signing key (and debug key only if you plan to install debug builds) to the Android client. Credential Manager uses the Web client ID as its server client ID.
2. In Supabase Dashboard → Authentication → Sign In / Providers → Google, enable Google and enter the Web client ID and client secret. Add the Android client ID as an additional allowed client ID if the dashboard field supports multiple IDs. In Google Cloud's Web client, add this redirect URI: `https://tlmxxzrgmbeojejnwebz.supabase.co/auth/v1/callback`. Keep the Google client secret in Supabase only.
3. In Firebase Console, register/verify Android package `com.gd.cipherchat` and ensure `app/google-services.json` belongs to that Firebase project and includes the Web OAuth client ID. Download the config for the new package and replace the current `app/google-services.json` before building. Do not put a Google client secret in the Android app.
4. Apply `supabase_schema.sql` to the existing project. The app's stable `profiles.id` UUID remains the ID used by chat, crypto, friends, and Room; `auth_user_id` links it to Supabase Auth.
5. If preserving old profiles, confirm each tester's identity and link the profile's `auth_user_id` to that tester's verified `auth.users.id` using a privileged admin workflow. Since you want to wipe local test data, you can skip this migration after deleting the old profiles and Auth users.
6. New users sign in with Google first, then register a display name/tag. The app creates the profile linked to the verified Supabase Auth UUID.

## Push notifications

1. Enable Firebase Cloud Messaging for the same Android Firebase project. Android testers need working Google Play services and network access.
2. Create/download a Firebase service account JSON with permission to send FCM messages for that project. In Supabase Dashboard → Edge Functions → Secrets, set:
   - `DATABASE_WEBHOOK_SECRET`: a high-entropy random value shared only with the database webhook.
   - `FCM_SERVICE_ACCOUNT_JSON`: the Firebase service-account JSON for a service account allowed to send FCM messages. Keep it server-side and out of git/client builds.
3. From the repository root, install/use the Supabase CLI, then run:
   ```powershell
   supabase login
   supabase link --project-ref tlmxxzrgmbeojejnwebz
   supabase functions deploy push-message --no-verify-jwt
   ```
4. In Supabase Dashboard → Database → Webhooks, create an `INSERT` webhook for `public.messages`. Use URL `https://tlmxxzrgmbeojejnwebz.supabase.co/functions/v1/push-message` and custom header `x-webhook-secret` with the same value as `DATABASE_WEBHOOK_SECRET`. The function excludes the sender and sends only a conversation ID; it never sends message contents.
5. Apply the SQL schema so `device_tokens` exists and owner-scoped RLS is active. On Android 13+, users must enable notification permission in App settings.

## Wipe the current alpha/test data

This clears server-side app records and media while keeping the Supabase project, schema, buckets, policies, and Edge Function. The configured project ref in this checkout is `tlmxxzrgmbeojejnwebz`; confirm that exact ref in Dashboard → Project Settings → General before running anything.

1. Stop the test app on all devices.
2. In Dashboard → Storage, open both `chat-media` and `avatars` and delete every object. Do this through Storage so the file objects are removed correctly.
3. In Dashboard → SQL Editor, first run the updated `supabase_schema.sql` from this repository. It creates any newer tables/policies and now tolerates tables already being in the Realtime publication.
4. In SQL Editor, run this to delete all app database rows while preserving the table definitions:
   ```sql
   truncate table
     public.device_tokens,
     public.typing_status,
     public.group_sender_keys,
     public.message_receipts,
     public.messages,
     public.conversation_members,
     public.conversations,
     public.friend_requests,
     public.prekeys,
     public.profiles
   cascade;
   ```
5. In Dashboard → Authentication → Users, delete the existing local-test Auth accounts too. Truncating the app tables does not delete Auth accounts.
6. On each Android test device, clear the app's storage or uninstall/reinstall it. This removes its Room cache, old profile UUID, and local encryption keys so the fresh onboarding path runs. Clearing server data alone does not reset device-local data.

The truncate command is irreversible. Confirm the project ref and that no tester data needs to be kept before executing it. It does not delete unrelated project tables, logs, backups, or deployed functions.

## Alpha update APKs

The updater reads published releases from `GDI14/cipherchat` and chooses the first non-draft release that has an APK asset. It includes prereleases so alpha testers can receive them. Name tags with semantic versions: `v1.0.1` for a regular release, or `v1.1.0-alpha.1`, `v1.1.0-alpha.2` for staged alpha builds. Set the Android `versionName` to the same version without the leading `v` (for example, `1.1.0-alpha.2`) and increment `versionCode` for every APK. Attach exactly one installable APK asset to each published release; a clear name such as `CypherChat-v1.1.0-alpha.2.apk` works. Draft releases are ignored. The updater compares major/minor/patch first, then prerelease identifiers, and treats a final version as newer than its prerelease.

The project is configured to sign release/alpha APKs with `alpha-upload-key.jks`; its local password and alias live in ignored `alpha-signing.properties`. Back up both securely and never commit or share them. Add this key's SHA-1 to the Firebase Android app/OAuth client. Give every tester an APK signed with this key; Android rejects future updates signed with a different key. Build, publish the GitHub Release, then testers can open **Settings → Application updates → Check for updates**, review release notes, download the APK, and confirm Android's package-installer prompts. Installation still depends on enabling **Install unknown apps** for CypherChat. The updater cannot silently install an APK.

## Before inviting testers

- Confirm Supabase URL/publishable key point at the intended alpha project and apply the schema there.
- Finish OAuth configuration and link existing testers' profiles before distributing the new build.
- Deploy and exercise the database webhook/Edge Function with a test account pair.
- Configure the alpha signing key in the release environment; never commit its keystore or passwords.
- Review the app's privacy/terms text and the generic lock-screen notification wording.
