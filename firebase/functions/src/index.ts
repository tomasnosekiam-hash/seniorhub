import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { onDocumentCreated } from "firebase-functions/v2/firestore";

admin.initializeApp();

const CHANNEL_ID = "family_messages";

async function getAdminFcmTokens(
  deviceId: string,
  excludeUid: string,
): Promise<string[]> {
  const joins = await admin
    .firestore()
    .collection("deviceAdmins")
    .where("deviceId", "==", deviceId)
    .get();
  const tokens: string[] = [];
  for (const j of joins.docs) {
    const uid = String(j.get("uid") ?? "");
    if (!uid || uid === excludeUid) {
      continue;
    }
    const tokenSnap = await admin
      .firestore()
      .collection("users")
      .doc(uid)
      .collection("fcmTokens")
      .get();
    for (const t of tokenSnap.docs) {
      const tok = String(t.get("token") ?? "");
      if (tok) {
        tokens.push(tok);
      }
    }
  }
  return tokens;
}

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
                channelId: CHANNEL_ID,
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

export const notifyAdminsOnMatejIncident = onDocumentCreated(
  {
    document: "devices/{deviceId}/incidents/{incidentId}",
    region: "europe-west1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const source = String(snap.data()?.source ?? "").trim();
    if (source !== "matej_emergency") return;

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
      logger.warn("No admin FCM tokens for Matej incident", { deviceId });
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
                body: `Matěj: nouzové volání → ${preview}`,
                channelId: CHANNEL_ID,
              },
            },
            data: {
              type: "matej_incident",
              deviceId,
              incidentId: event.params.incidentId as string,
            },
          })
          .catch((e) => logger.error("FCM send failed (Matej incident)", e)),
      ),
    );
  },
);
