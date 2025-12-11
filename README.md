# Secure File Vault

A standalone Java application for a Software Security group project. This app allows users to register, log in securely, and encrypt/decrypt local files. It also tracks every user action in an audit log for security accountability.

## 🚀 Features

* **Secure Authentication:** User passwords are hashed using **BCrypt** (salted automatically) before being stored in SQLite. No plain text passwords are ever saved.
* **AES Encryption:** Uses the Java Cryptography Architecture (JCA) to encrypt files with AES-256.
* **Audit Logging:** Automatically records login attempts, file encryption, and decryption events with timestamps.
* **GUI Dashboard:** Built with Java Swing for easy file selection and management.

## 🛠️ Tech Stack

* **Language:** Java (OpenJDK 25)
* **Database:** SQLite (via JDBC)
* **Security:** JBCrypt (Password Hashing), Java Native Crypto (AES)
* **IDE:** IntelliJ IDEA

## ⚙️ How to Run

1.  Clone this repository or download the ZIP.
2.  Open the project in **IntelliJ IDEA**.
3.  Allow Maven to download the dependencies (SQLite driver, JBCrypt).
4.  Run `src/main/java/com/securityproject/Main.java`.
5.  The database `securevault.db` will be created automatically on the first run.

## 🧪 Test Credentials

You can register a new user, or use this pre-registered account if the database file is included:

* **Username:** rezza
* **Password:** mySuperSecretPass

## 📂 Project Structure

* `db/` - Handles SQLite connection and audit logging.
* `utils/` - Contains the `SecurityUtils` class for AES encryption logic and BCrypt hashing.
* `ui/` - Contains the Login screen and Dashboard GUI.

## 📝 Security Implementation Details

* **Confidentiality:** Files are encrypted using a generated AES secret key stored locally (`vault.key`).
* **Integrity:** Passwords are checked using `BCrypt.checkpw()` to prevent rainbow table attacks.
* **Accountability:** The `audit_logs` table creates a permanent record of who did what and when.