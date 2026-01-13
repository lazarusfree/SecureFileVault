# Secure File Vault v1.2

A standalone Java application for secure file encryption and management. This app allows users to register, log in securely, and encrypt/decrypt local files using AES-256. It features a modern dark UI, strict file ownership enforcement, and detailed audit logging.

## Features

* **Secure Authentication:** User passwords are hashed using BCrypt before being stored. No plain text passwords are ever saved.
* **Intelligent File Ownership:** Users can only decrypt files that they personally encrypted. Unauthorized access attempts are blocked and logged.
* **AES Encryption:** Uses the Java Cryptography Architecture (JCA) to encrypt files with AES-256.
* **Modern UI:** Built with Java Swing and FlatLaf for a clean, professional dark mode experience.
* **Admin Dashboard:** A dedicated view for administrators to monitor all registered users and review audit logs.
* **Interactive Assistant:** "Moonpie", the vault guardian, provides real-time feedback and guidance within the application.

## Tech Stack

* **Language:** Java (OpenJDK 21)
* **Database:** SQLite
* **Security:** JBCrypt, Java Native Crypto (AES)
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
To access the Admin Dashboard, use the following credentials:
* **Username:** admin
* **Password:** admin123

## Project Structure

* `db/` - Handles SQLite connection, user management, and audit logging.
* `utils/` - Contains `SecurityUtils` for encryption/hashing and `Cowsay` for the assistant.
* `ui/` - Contains all GUI classes (`LoginScreen`, `Dashboard`, `AdminDashboard`).

## Security Implementation

* **Confidentiality:** Files are encrypted using a generated AES secret key stored locally (`vault.key`).
* **Integrity:** Passwords are verified using BCrypt to prevent rainbow table attacks.
* **Accountability:** The `audit_logs` table creates a permanent record of all user actions, including login events and file operations.
* **Access Control:** The application enforces ownership checks to ensure users cannot decrypt files belonging to others.
