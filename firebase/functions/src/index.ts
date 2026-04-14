import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { onDocumentCreated } from "firebase-functions/v2/firestore";

admin.initializeApp();

const CHANNEL_ID = "family_messages";

export const notifyTabletOnNewMessage = onDocumentCreated(
  {
    document: "devices/{deviceId}/messages/{messageId}",
    region: "europe-west1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const deviceId = event.params.deviceId as string;
    const body = String(snap.data()?.body ?? "").trim();
    const preview =
      body.length > 120 ? `${body.slice(0, 117)}…` : body || "Nový vzkaz";

    const deviceSnap = await admin.firestore().doc(`devices/${deviceId}`).get();
    const token = deviceSnap.get("fcmRegistrationToken");
    if (!token || typeof token !== "string") {
      logger.warn("No fcmRegistrationToken on device", { deviceId });
      return;
    }

    try {
      await admin.messaging().send({
        token,
        android: {
          priority: "high",
          notification: {
            title: "Nový vzkaz",
            body: preview,
            channelId: CHANNEL_ID,
          },
        },
        data: {
          type: "new_message",
          deviceId,
          messageId: event.params.messageId as string,
        },
      });
    } catch (e) {
      logger.error("FCM send failed", e);
    }
  },
);
