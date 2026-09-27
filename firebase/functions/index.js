/**
 * CipherChat Firebase Cloud Function
 * Triggers on new encrypted message in Firestore and delivers privacy-preserving FCM push notification.
 * Deploy with: firebase deploy --only functions
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendEncryptedMessageNotification = functions.firestore
  .document("messages/{messageId}")
  .onCreate(async (snap, context) => {
    const message = snap.data();
    if (!message) {
      console.log("No message data");
      return null;
    }

    const recipientId = message.recipientId;
    const senderId = message.senderId;
    const messageId = context.params.messageId;

    if (!recipientId) {
      console.log("No recipientId in message");
      return null;
    }

    try {
      // 1. Fetch recipient's registered FCM device token
      const recipientDoc = await admin.firestore().collection("users").doc(recipientId).get();
      if (!recipientDoc.exists) {
        console.log(`Recipient ${recipientId} does not exist in users collection`);
        return null;
      }

      const fcmToken = recipientDoc.data().fcmToken;
      if (!fcmToken) {
        console.log(`No FCM token registered for recipient ${recipientId}`);
        return null;
      }

      // 2. Fetch sender's display name (optional, content itself is strictly encrypted)
      let senderName = "A friend";
      try {
        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
        if (senderDoc.exists && senderDoc.data().displayName) {
          senderName = senderDoc.data().displayName;
        }
      } catch (err) {
        console.log("Could not fetch sender displayName", err);
      }

      // 3. Build privacy-preserving notification payload (Zero content leaked)
      const payload = {
        token: fcmToken,
        notification: {
          title: "CipherChat",
          body: `New encrypted message from ${senderName}`
        },
        data: {
          messageId: messageId,
          senderId: senderId,
          type: "ENCRYPTED_MESSAGE"
        },
        android: {
          priority: "high",
          notification: {
            channelId: "cipher_messages_channel",
            priority: "high",
            defaultSound: true,
            defaultVibrateTimings: true
          }
        }
      };

      const response = await admin.messaging().send(payload);
      console.log(`Successfully sent FCM push notification for message ${messageId}:`, response);
      return response;
    } catch (error) {
      console.error(`Error sending push notification to ${recipientId}:`, error);
      return null;
    }
  });
