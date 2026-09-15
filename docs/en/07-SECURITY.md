# FlowFinance — Security Notes

This document describes the current security posture of the app. A dedicated audit report (titled *Relatório de Auditoria de Segurança*) lives at `docs/security-audit/relatorio-auditoria-seguranca.pdf` (regenerable via `gerar_relatorio.py`).

## 1. Threat model at a glance

- **No account, no server, no network.** Data never leaves the device except for user-initiated file sharing (backup `.flowbackup`, spreadsheet `.xls`, CSV, PNG charts).
- **Sensitive data**: financial transactions, categories, user name, currency/preferences — all in an unencrypted SQLite DB and a DataStore file on device.
- **Exposure comes from:** physical device access, Android Auto Backup / cloud backups, anyone who receives a backup file.

## 2. Cryptography (`util/CryptoUtils.kt`)

Two independent key paths:

### a) Password-based backup encryption (user-facing export/import)
```
PBKDF2WithHmacSHA256(password, salt 16B, 65536 iters) → AES-256 key
AES/GCM/NoPadding (tag 128-bit, IV 12B)
output = Base64( salt ‖ iv ‖ ciphertext )
```
- `encrypt(data, password)` / `decrypt(encryptedDataString, password)`.
- Used by `SettingsViewModel.exportBackup()` / `importBackup()`.
- GCM provides **authenticated encryption** — corrupted/tampered backups fail on decrypt (GCM tag verification).

### b) Device-bound encryption (automatic backups — no user password)
- AES-256/GCM key generated in the **AndroidKeyStore** under alias `flowfinance_backup_key`.
- `KeyGenParameterSpec`: PURPOSE_ENCRYPT|DECRYPT, GCM, no padding, 256-bit. Key is **non-exportable** (harware-backed where the device supports it).
- `encryptWithKeystoreKey(data): ByteArray` returns `iv ‖ ciphertext`; `decryptWithKeystoreKey(data)` splits IV.
- Used by `BackupWorker` (weekly automatic CSV backup).

### Security parameters
| Parameter | Value |
|-----------|-------|
| KDF | PBKDF2WithHmacSHA256 |
| Iterations | 65 536 |
| Salt | 16 bytes (SecureRandom) |
| Cipher | AES/GCM/NoPadding |
| Key size | 256 bits |
| IV | 12 bytes (SecureRandom) |
| GCM tag | 128 bits |

## 3. Backups

- **Manual export/import** (`SettingsViewModel`): Gson-serialized `BackupPayload` (transactions + categories + user prefs) encrypted with the user password; wrapped in `BackupFile { metadata, encryptedData }`; written to a `.flowbackup` file and shared via FileProvider.
- **Import flow**: open document → `getBackupMetadata` reads only the unencrypted metadata (version / date, shown in the import dialog) → user enters password → `importBackup` validates shape, decrypts, and **replaces** all current data (warned in the UI).
- **Automatic weekly backup** (`BackupWorker`): CSV + AndroidKeyStore encryption, written to app-internal `filesDir` (not user-visible). CSV field escaping neutralizes spreadsheet formula injection (`=`, `+`, `-`, `@` → prefixed with `'`).

## 4. Android Auto Backup

`res/xml/backup_rules.xml` + `res/xml/data_extraction_rules.xml` govern cloud backups:
- The **Room database (`flowfinance_db`)** and the **DataStore file** are **excluded** from Android auto-backup (commit `b36615d`), preventing plaintext financial data from being copied to cloud backups.

## 5. Sharing & file permissions

- `FileProvider` configured with authority `${applicationId}.provider` and path map `res/xml/file_paths.xml`.
- Recent commit `4375570` narrowed the FileProvider external scope (internal files/external-files sharing only).
- Shares are fire-and-forget via `ACTION_SEND`; receivers get content URIs with read grant.

## 6. Notifications

- Permission `POST_NOTIFICATIONS` requested at runtime on Android 13+ (`MainActivity.requestPermissionLauncher`); `NotificationWorker`/`NotificationHelper` check `checkSelfPermission` before showing.
- Channel `flow_finance_channel` ("Lembretes e Alertas", IMPORTANCE_DEFAULT) created on API 26+.
- PendingIntents are **FLAG_IMMUTABLE**.

## 7. Hardening / obfuscation (release build)

- Release build: `isMinifyEnabled = true`, `isShrinkResources = true`, ProGuard `app/proguard-rules.pro`.
- Keep rules:
  - Room entity fields (`com.flowfinance.app.data.local.entity.**`) — for DAO mapping + Gson.
  - Backup/Gson model fields (`data.local.model.**`, `data.preferences.UserData`).
  - `keepattributes Signature, *Annotation*` and `TypeToken` keep for Gson.
- Note: Gson can be addressed with R8 full mode in future; current rules keep the needed members.

## 8. Data-clearing

- `SettingsScreen` "Clear Data": destructive action guarded by an `AlertDialog` that requires the user to type the confirmation phrase `i am sure` (case-insensitive) before the red confirm button is enabled.
- `clearAllData()` in `SettingsViewModel` deletes all custom categories + all transactions (default categories preserved).

## 9. Known weaknesses / recommendations (summary)

- **Database at rest is unencrypted.** All financial data is plaintext SQLite. Consider SQLCipher or Android Keystore-backed disk encryption if the app targets higher-risk users.
- **No app lock / biometrics.** Anyone with the unlocked device reads everything.
- **PBKDF2 iterations (65536)** are moderate; for a stronger posture prefer ≥ 200k for new password-based backups, or move to Argon2id (not available in stock Android until API-level libs are added).
- **`dbVersion` inconsistency**: `BackupMetadata.dbVersion` writes `3` while `AppDatabase` is at version 2 (recurring-transactions WIP will need reconciliation).
- **Screenshots/screen capture** allowed by default — sensitive devices may want `FLAG_SECURE`.
- **Single-user assumption**: export/import replace-all semantics can lose data if used carelessly.
- Recurring feature (WIP) will surface more DB version churn — keep applying explicit migrations rather than destructive fallback.