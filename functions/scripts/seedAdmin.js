/**
 * Creates (or repairs) the default IwaleCar administrator in Firebase.
 *
 * What it does:
 *   1. Creates the Firebase Auth user admin@email.com (or reuses it if it exists)
 *   2. Sets the custom claims { role: "Administrator", admin: true }  <- Firestore rules rely on these
 *   3. Writes the users/{uid} profile with role "Administrator"
 *
 * Usage (from the functions/ folder):
 *   1. Download a service-account key from Firebase Console > Project settings > Service accounts
 *   2. export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccountKey.json
 *   3. node scripts/seedAdmin.js
 *
 * Optional overrides: ADMIN_EMAIL, ADMIN_PASSWORD
 * Note: sign out and back in on the app after running it so the new claims are picked up.
 */
const admin = require("firebase-admin");

const EMAIL = process.env.ADMIN_EMAIL || "admin@email.com";
const PASSWORD = process.env.ADMIN_PASSWORD || "2400@hope24";

async function main() {
  admin.initializeApp();
  const auth = admin.auth();

  let user;
  try {
    user = await auth.getUserByEmail(EMAIL);
    await auth.updateUser(user.uid, { password: PASSWORD, emailVerified: true });
    console.log(`Updated existing auth user ${user.uid}`);
  } catch (e) {
    if (e.code !== "auth/user-not-found") throw e;
    user = await auth.createUser({ email: EMAIL, password: PASSWORD, emailVerified: true, displayName: "System Administrator" });
    console.log(`Created auth user ${user.uid}`);
  }

  await auth.setCustomUserClaims(user.uid, { role: "Administrator", admin: true });

  await admin.firestore().collection("users").doc(user.uid).set({
    uid: user.uid,
    fullName: "System Administrator",
    firstName: "System",
    lastName: "Administrator",
    phoneNumber: "999234567",
    email: EMAIL,
    role: "Administrator",
    requestedRole: "Administrator",
    primaryLocation: "Lilongwe HQ",
    bio: "",
    createdAt: Date.now(),
  }, { merge: true });

  console.log(`Default administrator ready: ${EMAIL}`);
}

main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
