# Firebase setup — Ganpati Vargani Manager

## Manual steps (required)

1. Create a Firebase project at https://console.firebase.google.com
2. Add an Android app with package names:
   - `com.ganpati.vargani` (release)
   - `com.ganpati.vargani.debug` (debug) — required because debug builds use `applicationIdSuffix = ".debug"`
3. Download the real `google-services.json` and replace `app/google-services.json`
   - The file must include **both** package names under `client`.
   - If you only added the release package, either add a second Android app in Firebase for `.debug`, or temporarily remove `applicationIdSuffix` in `app/build.gradle.kts`.
4. Enable **Authentication → Sign-in method → Email/Password → Enable** (required)
   - Without this, signup/login shows: **CONFIGURATION_NOT_FOUND**
   - Also open Google Cloud Console for project `ganpati-vargani-manager` → APIs & Services → enable **Identity Toolkit API** if prompted
5. Create a **Cloud Firestore** database (production mode), then deploy rules from `firestore.rules`
   - In Console: Firestore → Rules → paste `firestore.rules` → Publish
   - Or: `firebase deploy --only firestore:rules`
   - Re-publish rules after updates (`settings.viewersEnabled` gates viewer add/edit/delete only; viewing stays allowed)
6. Enable **Storage**, then deploy rules from `storage.rules`
7. Create composite indexes if Firebase Console prompts for them (usually after first query):
   - `payments`: `committeeId` ASC + `receiptNo` ASC
   - `members`: `committeeId` ASC + `name` ASC

### Deploy rules (CLI)

```bash
firebase deploy --only firestore:rules,storage
```

### Create a Viewer user

**In the app (recommended):** Admin → Settings → Manage users → Create viewer (max 2).

**Or manually in Console:**

1. Authentication → Add user (email/password)
2. Copy the UID
3. In Firestore `users/{uid}` create:

```json
{
  "name": "Viewer Name",
  "email": "viewer@example.com",
  "mobile": "",
  "role": "VIEWER",
  "committeeId": "<same-as-admin-committeeId>",
  "createdAt": 0
}
```

Sign-up in the app creates an **Admin** + new **committee** + default **settings**.

## Collections

| Collection | Purpose |
|------------|---------|
| `users` | Auth profiles + role + committeeId |
| `committees` | Festival committee / org |
| `members` | Donor directory (auto-filled from payments) |
| `payments` | Donations (vargani) |
| `expenses` | Outgoing expenses |
| `events` | Festival events (API ready) |
| `settings` | Per-committee app settings (`doc id` = committeeId) |

## Storage

`committees/{committeeId}/qr/qr_code.jpg` — payment QR image

## Offline

Firestore persistence is enabled in `FirebaseModule` (local cache + sync).
