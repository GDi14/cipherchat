import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import admin from "npm:firebase-admin@11.10.1"

// 1. Initialize Firebase Admin (Using the JSON you downloaded in Step 1)
const firebaseServiceAccount = JSON.parse(Deno.env.get('FIREBASE_SERVICE_ACCOUNT') || '{}')
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(firebaseServiceAccount)
  })
}

serve(async (req) => {
  try {
    // 2. Parse the Webhook payload from Postgres
    const payload = await req.json()
    const message = payload.record // The newly inserted row in public.messages

    // If there is no recipient_id (e.g. Group chats), you'd query conversation_members here.
    // For this example, we assume 1-on-1 direct messages using recipient_id.
    const recipientId = message.recipient_id
    if (!recipientId) {
       return new Response(JSON.stringify({ error: "No recipient_id" }), { status: 200 })
    }

    // 3. Initialize Supabase client to query the device tokens
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '' // Service role bypasses RLS to read tokens
    )

    // 4. Fetch the recipient's FCM tokens
    const { data: tokens, error } = await supabase
      .from('device_tokens')
      .select('fcm_token')
      .eq('user_id', recipientId)

    if (error || !tokens || tokens.length === 0) {
      return new Response(JSON.stringify({ message: "No devices found for user" }), { status: 200 })
    }

    // 5. Construct the Data Payload (MUST MATCH ChatFCMService.kt EXACTLY)
    const fcmPayload = {
      data: {
        message_id: message.id,
        conversation_id: message.conversation_id,
        sender_id: message.sender_id,
        recipient_id: message.recipient_id || "",
        message_type: message.message_type.toString(),
        encrypted_body: message.encrypted_body,
        created_at: message.created_at
      }
    }

    // 6. Send the push notification to all of the user's devices
    const fcmTokens = tokens.map(t => t.fcm_token)
    const response = await admin.messaging().sendMulticast({
      tokens: fcmTokens,
      data: fcmPayload.data,
      // CRITICAL: We do NOT include a "notification" block here. 
      // If we did, the OS would intercept it and show an empty notification.
    })

    return new Response(JSON.stringify({ success: true, firebaseResponse: response }), {
      headers: { "Content-Type": "application/json" },
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500 })
  }
})