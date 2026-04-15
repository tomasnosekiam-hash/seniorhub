import * as admin from "firebase-admin";

/**
 * Tokeny FCM všech správců zařízení kromě `excludeUid` (prázdný řetězec = nikoho nevynechat).
 */
export async function getAdminFcmTokens(
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
