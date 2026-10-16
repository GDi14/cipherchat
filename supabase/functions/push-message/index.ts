import { createClient } from "npm:@supabase/supabase-js@2";
import { importPKCS8, SignJWT } from "npm:jose@5";

const TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

type MessageRow = {
  id: string;
  conversation_id: string;
  sender_id: string;
  recipient_id: string | null;
};

type DatabaseWebhook = {
  type: string;
  table: string;
  schema: string;
  record: MessageRow;
};

async function getFcmAccessToken(serviceAccount: {
  client_email: string;
  private_key: string;
}): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const key = await importPKCS8(serviceAccount.private_key, "RS256");
  const assertion = await new SignJWT({ scope: FCM_SCOPE })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(serviceAccount.client_email)
    .setSubject(serviceAccount.client_email)
    .setAudience(TOKEN_ENDPOINT)
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(key);

  const response = await fetch(TOKEN_ENDPOINT, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) throw new Error(`FCM OAuth failed (${response.status})`);
  const result = await response.json();
  return result.access_token as string;
}

Deno.serve(async (request) => {
  const expectedSecret = Deno.env.get("DATABASE_WEBHOOK_SECRET");
  if (!expectedSecret || request.headers.get("x-webhook-secret") !== expectedSecret) {
    return new Response("Unauthorized", { status: 401 });
  }

  let event: DatabaseWebhook;
  try {
    event = await request.json();
  } catch {
    return new Response("Invalid JSON", { status: 400 });
  }
  if (event.type !== "INSERT" || event.schema !== "public" || event.table !== "messages") {
    return Response.json({ ignored: true });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const serviceAccountJson = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON");
  if (!supabaseUrl || !serviceRoleKey || !serviceAccountJson) {
    return new Response("Push service is not configured", { status: 503 });
  }

  try {
    const admin = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const row = event.record;
    const { data: senderProfile, error: senderError } = await admin
      .from("profiles")
      .select("display_name")
      .eq("id", row.sender_id)
      .maybeSingle();
    if (senderError) throw senderError;
    const senderName = (senderProfile?.display_name ?? "New message").trim().slice(0, 80) || "New message";

    let recipientIds: string[];
    if (row.recipient_id) {
      recipientIds = [row.recipient_id];
    } else {
      const { data, error } = await admin
        .from("conversation_members")
        .select("user_id")
        .eq("conversation_id", row.conversation_id)
        .neq("user_id", row.sender_id);
      if (error) throw error;
      recipientIds = (data ?? []).map((member) => member.user_id as string);
    }
    if (recipientIds.length === 0) {
      console.warn("Push skipped: message has no recipient profiles");
      return Response.json({ sent: 0, failed: 0, reason: "no_recipients" });
    }

    const { data: devices, error: deviceError } = await admin
      .from("device_tokens")
      .select("fcm_token")
      .in("user_id", recipientIds);
    if (deviceError) throw deviceError;
    if (!devices?.length) {
      console.warn("Push skipped: recipient profiles have no registered FCM devices");
      return Response.json({ sent: 0, failed: 0, reason: "no_registered_devices" });
    }

    const serviceAccount = JSON.parse(serviceAccountJson);
    const accessToken = await getFcmAccessToken(serviceAccount);
    const endpoint = `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`;
    const results = await Promise.all(devices.map(async ({ fcm_token }) => {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: fcm_token,
            // Keep push content generic so encrypted message text never leaves the app.
            // The notification payload lets Android display it while the process is
            // stopped; data remains available for routing/catch-up when tapped.
            notification: { title: senderName, body: "Sent you a message" },
            data: {
              conversation_id: row.conversation_id,
              message_id: row.id,
              sender_name: senderName,
              kind: "new_message",
            },
            android: {
              priority: "HIGH",
              ttl: "3600s",
              notification: { channel_id: "messages" },
            },
          },
        }),
      });
      if (!response.ok) {
        const details = await response.text();
        console.error("FCM send failed (" + response.status + ")", details);
        if (/UNREGISTERED|registration.token.not.registered/i.test(details)) {
          const { error: deleteError } = await admin
            .from("device_tokens")
            .delete()
            .eq("fcm_token", fcm_token);
          if (deleteError) console.error("Could not remove invalid FCM token", deleteError);
        }
      }
      return response.ok;
    }));

    const sent = results.filter(Boolean).length;
    const failed = results.length - sent;
    console.info(`FCM delivery result: ${sent} sent, ${failed} failed`);
    return Response.json({ sent, failed });
  } catch (error) {
    console.error("Push delivery failed", error);
    return new Response("Push delivery failed", { status: 500 });
  }
});
