import { createClient } from "npm:@supabase/supabase-js@2";
import { importPKCS8, SignJWT } from "npm:jose@5";

const TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

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
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`FCM OAuth failed (${response.status}): ${errorText}`);
  }
  const result = await response.json();
  return result.access_token as string;
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
      },
    });
  }

  try {
    const authHeader = req.headers.get('Authorization') || '';
    const body = await req.json();
    const { callee_id, call_id, caller_name, caller_id: fallbackCallerId } = body;

    console.log(`[initiate-call] Incoming call request for callee: ${callee_id}, callId: ${call_id}`);

    if (!callee_id || !call_id) {
      return new Response(JSON.stringify({ error: "Missing callee_id or call_id" }), { status: 400 });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

    // Support both FCM_SERVICE_ACCOUNT_JSON and FIREBASE_SERVICE_ACCOUNT env var names
    const serviceAccountRaw = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON") || Deno.env.get("FIREBASE_SERVICE_ACCOUNT");
    if (!serviceAccountRaw) {
      console.error("[initiate-call] Neither FCM_SERVICE_ACCOUNT_JSON nor FIREBASE_SERVICE_ACCOUNT secret is configured.");
      return new Response(JSON.stringify({ error: "FCM Service account not configured on server" }), { status: 500 });
    }

    const serviceAccount = JSON.parse(serviceAccountRaw);

    const supabase = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });

    let callerId = fallbackCallerId;
    const { data: { user } } = await supabase.auth.getUser();
    if (user) {
      callerId = user.id;
    }

    if (!callerId) {
      console.error("[initiate-call] Unauthorized: No caller ID identified.");
      return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 });
    }

    const supabaseAdmin = createClient(supabaseUrl, serviceRoleKey);

    const { data: tokens, error: tokenError } = await supabaseAdmin
      .from('device_tokens')
      .select('fcm_token')
      .eq('user_id', callee_id);

    if (tokenError || !tokens || tokens.length === 0) {
      console.warn(`[initiate-call] No FCM device tokens found in DB for callee_id: ${callee_id}`);
      return new Response(JSON.stringify({ error: "No registered devices found for callee" }), { status: 404 });
    }

    console.log(`[initiate-call] Found ${tokens.length} token(s) for callee: ${callee_id}`);

    const accessToken = await getFcmAccessToken(serviceAccount);
    const endpoint = `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`;

    const results = await Promise.all(tokens.map(async ({ fcm_token }) => {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: fcm_token,
            data: {
              type: "incoming_call",
              caller_id: callerId,
              caller_name: caller_name || "Someone",
              call_id: call_id,
            },
            android: {
              priority: "HIGH",
              ttl: "0s",
            },
          },
        }),
      });

      if (!response.ok) {
        const details = await response.text();
        console.error(`[initiate-call] FCM send failed (${response.status}):`, details);
      }
      return response.ok;
    }));

    const sent = results.filter(Boolean).length;
    const failed = results.length - sent;
    console.log(`[initiate-call] Final delivery result: ${sent} sent, ${failed} failed`);

    return new Response(JSON.stringify({ success: true, sent, failed }), {
      headers: { "Content-Type": "application/json", 'Access-Control-Allow-Origin': '*' },
    });
  } catch (error) {
    console.error("[initiate-call] Error processing call push:", error.message);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { 'Access-Control-Allow-Origin': '*' },
    });
  }
});
