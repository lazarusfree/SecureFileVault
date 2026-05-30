# Secure File Vault v2.0

A standalone Java application for secure file encryption and management. The project is designed for the BIS 20404 Cryptography group project brief: it demonstrates confidentiality, integrity, and authenticity through a working GUI system, user authentication, authenticated file encryption, ownership enforcement, and audit logging.

## Features

* **Secure Authentication:** User passwords are hashed using BCrypt before being stored. No plain text passwords are ever saved.
* **Authenticated File Encryption:** Files are encrypted with `AES/GCM/NoPadding` using 256-bit keys, random per-file IVs, per-file derived keys, and authenticated metadata.
* **Integrity Verification:** GCM authentication tags detect tampered ciphertext or metadata before decryption succeeds. The database also stores a SHA-256 digest of encrypted output for audit review.
* **Authenticity and Ownership:** Users can only decrypt files they personally encrypted. The logged-in user ID is also bound into the encrypted file as authenticated metadata.
* **Modern UI:** Built with Java Swing and FlatLaf for a clean, professional dark mode experience.
* **Admin Dashboard:** A dedicated view for administrators to monitor users, encrypted file records, cryptographic algorithms, integrity hashes, and audit logs.
* **Interactive Assistant:** "Moonpie", the vault guardian, provides real-time feedback and guidance within the application.

## Tech Stack

* **Language:** Java (OpenJDK 21)
* **Database:** SQLite
* **Security:** JBCrypt, Java Cryptography Architecture, AES-256-GCM, PBKDF2-HMAC-SHA256, SHA-256
* **UI:** FlatLaf (Swing Look and Feel)

## How to Run

1.  Open the project in **IntelliJ IDEA**.
2.  Allow Maven to resolve dependencies.
3.  Run `src/main/java/com/securityproject/Main.java`.
4.  The database `securevault.db` will be created automatically on startup.

**Building a JAR:**
Please refer to the `build_guide.md` file included in this repository for detailed instructions on how to build a standalone executable JAR using IntelliJ IDEA.

## Credentials

### Standard User
You can register a new user at the login screen.

### Admin User
The default admin account is created in the database on first startup:
* **Username:** admin
* **Password:** admin123

For a real deployment, change this default password immediately. It is kept here only so the project can be demonstrated quickly.

## Project Structure

* `db/` - Handles SQLite connection, user management, and audit logging.
* `utils/` - Contains `SecurityUtils` for encryption/hashing and `Cowsay` for the assistant.
* `ui/` - Contains all GUI classes (`LoginScreen`, `Dashboard`, `AdminDashboard`).

## Security Implementation

* **Confidentiality:** Files are encrypted using AES-256-GCM. The application stores a local master key in `vault.key`, then derives a separate per-file AES key using PBKDF2-HMAC-SHA256 and a random salt.
* **Integrity:** AES-GCM validates a 128-bit authentication tag during decryption. If the encrypted file or authenticated metadata is modified, decryption fails.
* **Authenticity:** The authenticated metadata binds the encrypted file to the owner user ID, original file name, file size, and creation time. The application checks both the database ownership record and the authenticated metadata before decryption.
* **Accountability:** The `audit_logs` table records login, logout, encryption, decryption, and blocked access events.
* **Access Control:** The application enforces ownership checks so users cannot decrypt files belonging to other users.

## Rubric Alignment

* **Application:** A local secure file vault for protecting personal or organizational files on a shared computer.
* **Security services:** Confidentiality through AES-256-GCM encryption, integrity through GCM tag verification and SHA-256 records, and authenticity through BCrypt login plus authenticated owner metadata.
* **Cryptographic technique:** Java Cryptography Architecture with AES-GCM, PBKDF2-HMAC-SHA256 key derivation, SecureRandom IV/salt generation, SHA-256 hashing, and BCrypt password hashing.
* **Program workflow:** Register or log in, choose a file, encrypt it into `.enc` format, record ownership and audit data, then decrypt only after ownership and authentication checks pass.
* **Demo focus:** Show registration, login, encryption, admin file inventory, successful decryption by the owner, and failed decryption attempt from another user.
