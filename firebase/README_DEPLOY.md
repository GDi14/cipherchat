# Firebase Backend & Cloud Function Deployment Guide

This document outlines the exact steps to configure your Firebase project and deploy the push notification Cloud Function for CipherChat.

---

## 1. Firebase Project Creation
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or **Create a project**).
3. Name your project (e.g. `cipher-chat-private`).
4. (Optional) Disable Google Analytics if not needed, then click **Create project**.

---

## 2. Register Android App & Add `google-services.json`
1. On your Firebase Project Overview page, click the **Android** icon (`+ Add app`).
2. Enter the exact Android package name from the app:
   - **Android package name:** `com.aistudio.cipherchat.qvfpmx`
   - **App nickname (optional):** `CipherChat`
3. Click **Register app**.
4. Download the `google-services.json` file.
5. Place `google-services.json` inside the `/app/` directory of your project:
   ```
   app/google-services.json
   ```
6. Click **Next** through the remaining steps in the console.

---

## 3. Enable Cloud Firestore
1. In the Firebase console left sidebar, navigate to **Build** -> **Firestore Database**.
2. Click **Create database**.
3. Choose a location closest to your group of friends (e.g. `us-central1` or `asia-east1`).
4. Select **Start in production mode** (or test mode).
5. Open the **Rules** tab in Firestore, copy the contents of `firebase/firestore.rules`, paste them into the console editor, and click **Publish**.

---

## 4. Enable Firebase Storage (Blaze Plan Required)
> **IMPORTANT NOTE:** Firebase Storage requires the project to be on the **Blaze (Pay-as-you-go)** plan. It cannot be enabled on the free Spark plan. However, for a small private circle of friends, monthly usage typically remains well within Firebase's free tier thresholds (5 GB free storage, 1 GB/day download).

1. In the bottom-left corner of the Firebase console, upgrade the billing plan from **Spark** to **Blaze**.
2. In the left sidebar, navigate to **Build** -> **Storage**.
3. Click **Get started**, choose your Cloud Storage bucket location, and finish setup.
4. Open the **Rules** tab in Storage, paste the contents of `firebase/storage.rules`, and click **Publish**.
5. *Graceful fallback note:* If Storage is not yet enabled, CipherChat will gracefully display an informative banner ("File sharing isn't set up yet — enable Firebase Storage in the console") when attempting to share files, without crashing or hindering end-to-end encrypted text messages.

---

## 5. Enable Firebase Cloud Messaging (FCM)
1. In the Firebase console, click the gear icon (⚙️) next to **Project Overview** -> **Project settings**.
2. Navigate to the **Cloud Messaging** tab.
3. Ensure **Firebase Cloud Messaging API (V1)** is enabled. If disabled, click the three-dots menu to enable it in Google Cloud Console.

---

## 6. Deploy the Cloud Function

The Cloud Function triggers automatically whenever a new encrypted message is written to Firestore and dispatches a high-priority FCM notification to the recipient without revealing any message contents.

### Prerequisites
- Node.js 18+ installed on your development machine.
- Firebase CLI installed:
  ```bash
  npm install -g firebase-tools
  ```

### Deployment Steps
1. Log in to your Firebase account via terminal:
   ```bash
   firebase login
   ```
2. Navigate to the `firebase/` directory in this project:
   ```bash
   cd firebase
   ```
3. Initialize functions project (if not already linked):
   ```bash
   firebase use --add
   ```
   Select your Firebase project from the list and give it an alias (e.g. `default`).
4. Install function dependencies:
   ```bash
   cd functions
   npm install
   cd ..
   ```
5. Deploy the function to Firebase:
   ```bash
   firebase deploy --only functions
   ```
6. Once deployed, verify in the Firebase console under **Build** -> **Functions** that `sendEncryptedMessageNotification` is listed and active.
