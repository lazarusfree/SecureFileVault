# Secure File Vault v2.0

A standalone Java application for secure file encryption and management. Built for the BIS 20404 Cryptography group project, it demonstrates **confidentiality**, **integrity**, and **authenticity** through a working GUI system with user authentication, authenticated file encryption, ownership enforcement, and audit logging.

## Features

- **Secure Authentication:** BCrypt password hashing (12 log rounds). No plaintext passwords ever stored.
- **Authenticated File Encryption:** AES-256-GCM with per-file derived keys (PBKDF2-HMAC-SHA256, 120k iterations), random 12-byte IVs, random 16-byte salts, and authenticated metadata bound as AAD.
- **Integrity Verification:** GCM 128-bit authentication tags detect tampered ciphertext or metadata before decryption succeeds. SHA-256 digest stored in database for audit.
- **Ownership Enforcement:** Users can only decrypt files they personally encrypted. Owner ID is cryptographically bound into the file's authenticated metadata.
- **Modern UI:** Java Swing with FlatLaf dark theme.
- **Admin Dashboard:** Tabbed view showing users, encrypted file inventory (with algorithms and integrity hashes), and audit logs.
- **Interactive Assistant:** "Moonpie" the vault guardian provides real-time feedback via cowsay-style ASCII art.

## Tech Stack

| Category        | Technology                                |
|-----------------|-------------------------------------------|
| Language        | Java (OpenJDK 21)                         |
| Build Tool      | Apache Maven 3.x                          |
| Database        | SQLite (via sqlite-jdbc 3.45)             |
| UI Framework    | Swing + FlatLaf 3.4 (dark theme)          |
| Password Hash   | JBCrypt 0.4 (12 log rounds)               |
| Encryption      | AES-256-GCM (JCA/JCE)                     |
| Key Derivation  | PBKDF2-HMAC-SHA256 (120,000 iterations)   |
| Hashing         | SHA-256 (JCA)                             |
| Logging         | SLF4J Simple 2.0                          |
| Packaging       | Maven Shade Plugin (uber JAR)             |

## Architecture

The application follows a **layered architecture** with dependency injection:

```
src/main/java/com/securityproject/
├── App.java                       Entry point, bootstraps FlatLaf and AppContext
├── AppContext.java                DI container — wires all components together
├── config/
│   └── VaultConfig.java           Centralized configuration constants
├── model/                         Domain records
│   ├── User.java                  (id, username, passwordHash, role)
│   ├── VaultFile.java             (id, userId, username, paths, algorithm, hash, timestamp)
│   ├── AuditLog.java              (id, userId, username, action, timestamp)
│   └── PasswordStrength.java      Validation result record
├── exception/                     Typed exception hierarchy
│   ├── VaultException.java        Base runtime exception
│   ├── AuthenticationException.java
│   ├── FileAccessDeniedException.java
│   └── CryptoOperationException.java
├── repository/                    Data access layer
│   ├── ConnectionProvider.java    Functional interface for SQLite connections
│   ├── UserRepository.java        Interface
│   ├── FileRepository.java        Interface
│   ├── AuditRepository.java       Interface
│   └── impl/                      SQLite implementations
│       ├── SqliteUserRepository.java
│       ├── SqliteFileRepository.java
│       └── SqliteAuditRepository.java
├── service/                       Business logic layer
│   ├── AuthService.java           Authentication + registration
│   ├── EncryptionService.java     AES-256-GCM encrypt/decrypt with ownership checks
│   └── KeyService.java            Master key lifecycle (load/generate)
├── db/
│   └── DatabaseInitializer.java   Schema creation + migrations
├── util/
│   ├── PasswordUtil.java          BCrypt hashing + password validation
│   └── Cowsay.java                ASCII art speech bubble generator
└── ui/                            Presentation layer
    ├── LoginScreen.java           Login/register form
    ├── Dashboard.java             User dashboard (encrypt/decrypt/logout)
    └── AdminDashboard.java        Admin panel (users, files, audit logs)
```

### Design Patterns

- **Dependency Injection:** `AppContext` creates and wires all components; UI classes receive services via the context.
- **Repository Pattern:** Data access is abstracted behind interfaces, enabling testability and swappable storage backends.
- **Service Layer:** Business logic (auth, encryption, auditing) is centralized in services, not scattered across UI code.
- **Typed Exceptions:** Custom exception hierarchy replaces generic `Exception` throws for better error handling.

## How to Run

1. Open the project in **IntelliJ IDEA** or any Java IDE.
2. Allow Maven to resolve dependencies.
3. Run `src/main/java/com/securityproject/App.java`.
4. The database `securevault.db` and master key `vault.key` are created automatically.

### Building a JAR

```bash
mvn clean package
java -jar target/filevault-1.0-SNAPSHOT.jar
```

See `build_guide.md` for detailed IntelliJ instructions.

## Credentials

### Standard User

Register a new user at the login screen. Passwords require at least 8 characters with 3 of 4 complexity classes (uppercase, lowercase, digits, special characters).

### Admin User

Created automatically on first startup:
- **Username:** `admin`
- **Password:** Override via `-Dsfv.admin.password=yourpassword` or `SFV_ADMIN_PASSWORD` env var. Defaults to `admin123` with a warning.

> Change the default password immediately. The default exists only for quick demonstration.

## Security Implementation

| Service         | Mechanism                                                                 |
|-----------------|---------------------------------------------------------------------------|
| Confidentiality | AES-256-GCM with per-file derived keys (master key → PBKDF2 → file key)  |
| Integrity       | GCM authentication tag (128-bit) + SHA-256 hash stored in database        |
| Authenticity    | Owner ID cryptographically bound into AAD; ownership verified at decrypt  |
| Accountability  | `audit_logs` table records login, logout, encrypt, decrypt, blocked access|
| Access Control  | Database-level ownership check + cryptographic owner binding in file AAD  |

### Encryption Flow

1. Random 16-byte salt + 12-byte IV generated per file
2. File key derived from master key via PBKDF2-HMAC-SHA256 (120,000 iterations)
3. Authenticated metadata created: `owner=<id>;name=<base64>;bytes=<size>;created=<timestamp>`
4. File encrypted with AES-256-GCM, metadata passed as AAD
5. Structured header written: `[SFV2 magic][version][salt_len][iv_len][aad_len][salt][iv][aad][ciphertext]`

### Decryption Flow

1. Header parsed and validated (magic, version, size sanity checks)
2. Owner ID extracted from AAD and verified against logged-in user
3. Database ownership record cross-checked
4. GCM decryption with AAD verification — any tampering causes `AEADBadTagException`

## Rubric Alignment

- **Application:** Local secure file vault for protecting files on shared computers.
- **Security Services:** Confidentiality (AES-256-GCM), integrity (GCM tag + SHA-256), authenticity (BCrypt login + owner metadata).
- **Cryptographic Technique:** JCA with AES-GCM, PBKDF2-HMAC-SHA256, SecureRandom, SHA-256, BCrypt.
- **Program Workflow:** Register → Login → Encrypt (`.enc`) → Decrypt (ownership + integrity verified) → Admin audit review.
- **Demo Focus:** Registration, login, encryption, admin file inventory, successful decryption by owner, blocked decryption by non-owner.
