import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { getAdminFcmTokens } from "./adminFcmTokens";
import {
  CHANNEL_ID_EMERGENCY_INCIDENT,
  CHANNEL_ID_MESSAGES,
} from "./fcmConstants";

admin.initializeApp();

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
    const senderUid = String(snap.data()?.senderUid ?? "");
    const deviceAuthUid = String(deviceSnap.get("deviceAuthUid") ?? "");
    const isFromTablet =
      Boolean(senderUid && deviceAuthUid && senderUid === deviceAuthUid);
    if (isFromTablet) {
      logger.info("Skip FCM to tablet: message created by the device (e.g. Firestore SMS fallback)", {
        deviceId,
      });
    } else {
      const token = deviceSnap.get("fcmRegistrationToken");
      if (!token || typeof token !== "string") {
        logger.warn("No fcmRegistrationToken on device", { deviceId });
      } else {
        try {
          await admin.messaging().send({
            token,
            android: {
              priority: "high",
              notification: {
                title: "Nový vzkaz",
                body: preview,
                channelId: CHANNEL_ID_MESSAGES,
              },
            },
            data: {
              type: "new_message",
              deviceId,
              messageId: event.params.messageId as string,
            },
          });
        } catch (e) {
          logger.error("FCM send failed (tablet)", e);
        }
      }
    }

    if (isFromTablet) {
      return;
    }

    const adminTokens = await getAdminFcmTokens(deviceId, senderUid);
    if (adminTokens.length === 0) {
      return;
    }
    const label = deviceSnap.get("deviceLabel");
    const deviceLabel =
      typeof label === "string" && label.trim().length > 0 ? label.trim() : deviceId;
    await Promise.all(
      adminTokens.map((tok) =>
        admin
          .messaging()
          .send({
            token: tok,
            android: {
              priority: "high",
              notification: {
                title: `Vzkaz · ${deviceLabel}`,
                body: preview,
                channelId: CHANNEL_ID_MESSAGES,
              },
            },
            data: {
              type: "new_message_admin_copy",
              deviceId,
              messageId: event.params.messageId as string,
            },
          })
          .catch((e) => logger.error("FCM send failed (admin)", e)),
      ),
    );
  },
);

/** FCM správcům při novém dokumentu v `devices/.../incidents` (zdroj v poli `source` je jen metadata). */
export const notifyAdminsOnDeviceIncident = onDocumentCreated(
  {
    document: "devices/{deviceId}/incidents/{incidentId}",
    region: "europe-west1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const deviceId = event.params.deviceId as string;
    const deviceSnap = await admin.firestore().doc(`devices/${deviceId}`).get();
    const label = deviceSnap.get("deviceLabel");
    const deviceLabel =
      typeof label === "string" && label.trim().length > 0 ? label.trim() : deviceId;
    const phone = String(snap.data()?.dialedPhone ?? "").trim();
    const preview =
      phone.length > 80 ? `${phone.slice(0, 77)}…` : phone || "—";

    const adminTokens = await getAdminFcmTokens(deviceId, "");
    if (adminTokens.length === 0) {
      logger.warn("No admin FCM tokens for device incident", { deviceId });
      return;
    }

    await Promise.all(
      adminTokens.map((tok) =>
        admin
          .messaging()
          .send({
            token: tok,
            android: {
              priority: "high",
              notification: {
                title: `Nouze · ${deviceLabel}`,
                body: `Incident → ${preview}`,
                channelId: CHANNEL_ID_EMERGENCY_INCIDENT,
              },
            },
            data: {
              type: "device_incident",
              deviceId,
              incidentId: event.params.incidentId as string,
            },
          })
          .catch((e) => logger.error("FCM send failed (device incident)", e)),
      ),
    );
  },
);
