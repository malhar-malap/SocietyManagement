# Jaswanti Residency - Housing Society Management System

A full-stack housing society management web application for **Jaswanti Residency** (C Wing, 80 rooms).

> **Product scope and development roadmap:** See [PROJECT_GUIDE.md](PROJECT_GUIDE.md) for the agreed MVP, Resident/Admin/Master Admin responsibilities, security and ledger rules, phased roadmap, and instructions for continuing development with Antigravity.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React, a small in-house CSS design system (`src/styles`), Lucide React, Recharts, Axios |
| Backend | Spring Boot 4, Spring Security, JWT |
| Database | MySQL + Flyway migrations |
| Email | Gmail SMTP (JavaMailSender) |

## Project Structure

```
HousingSocietyManagement/
├── backend/JaswantiResidency/   # Spring Boot backend
└── jaswantiresidency/           # React frontend
```

## Getting Started

### Backend
1. Copy `.env.example` to `.env` in `backend/JaswantiResidency/` and fill in real values (see [Configuration](#configuration) below).
2. Run: `cd backend/JaswantiResidency && ./mvnw spring-boot:run`

### Frontend
```bash
cd jaswantiresidency
npm install
npm start
```

### Master Admin Initial Bootstrap
The `MASTER_ADMIN` role has no public registration endpoint and no default seeded credentials. To initialize the first Master Admin, configure the following environment variables in your deployment or `.env` file before starting the backend:

```env
BOOTSTRAP_MASTER_ADMIN_ENABLED=true
BOOTSTRAP_MASTER_ADMIN_EMAIL=masteradmin@jaswanti.com
BOOTSTRAP_MASTER_ADMIN_NAME="Master Administrator"
BOOTSTRAP_MASTER_ADMIN_PASSWORD=your_temporary_strong_password
```

Start the backend once with these variables present to create the account. Once initialized, remove the variables or set `BOOTSTRAP_MASTER_ADMIN_ENABLED=false`. Bootstrap fails closed if any variable is missing, if the email is already in use, or if an active Master Admin already exists.

## Configuration

All secrets and environment-specific values are loaded from `backend/JaswantiResidency/.env`. A template with placeholder values is provided at `.env.example`.

> **⚠️ Never commit `.env` to version control.** It is already in `.gitignore`.

| Variable | Description | Example / Notes |
|---|---|---|
| `DB_URL` | JDBC URL for the MySQL database | `jdbc:mysql://localhost:3306/jaswanti_residency?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata` |
| `DB_USER` | MySQL username | `root` or a dedicated service account |
| `DB_PASSWORD` | MySQL password | |
| `JWT_SECRET` | Base64-encoded signing key for JWT tokens. **Must be at least 64 bytes (512 bits)** before encoding. | Generate with: `openssl rand -base64 64` |
| `MAIL_USERNAME` | Gmail address used for sending verification and reset emails | `yourapp@gmail.com` |
| `MAIL_PASSWORD` | Gmail App Password (not your Gmail login password). [Create one here](https://myaccount.google.com/apppasswords). | 16-character app-specific password |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins for CORS | `https://portal.example.com,http://localhost:3000` (default: `http://localhost:3000,http://localhost:5173`) |
| `SERVER_FORWARD_HEADERS_STRATEGY` | Set to `framework` when deployed behind a reverse proxy so login rate limiting and audit logging resolve the real client IP. The app deliberately ignores unparsed/raw `X-Forwarded-For` headers. | `framework` |

### Frontend Build-Time Configuration

- `REACT_APP_API_BASE_URL`: Base URL for backend API requests, configured at **build** time (default: `http://localhost:8080/api`).
  ```bash
  REACT_APP_API_BASE_URL=https://api.example.com/api npm run build
  ```

### Generating a JWT Secret

The backend validates JWT secrets at startup and refuses to start if `JWT_SECRET` is missing, not valid base64, decodes to fewer than 32 bytes, or equals the known public test value. Generate a production key with:

```bash
# Generate a cryptographically secure 64-byte key, base64-encoded:
openssl rand -base64 64

# Or with Python:
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(64)).decode())"
```

Copy the output into your `.env` as the `JWT_SECRET` value.

### Flyway Database Migrations

The application uses [Flyway](https://flywaydb.org/) to manage database schema versioning. Migrations are located in `backend/JaswantiResidency/src/main/resources/db/migration/`.

- **Fresh database**: Flyway runs all migrations (V1 → latest) automatically on first startup.
- **Existing database** (previously managed by Hibernate `ddl-auto=update`): Flyway baselines at V14 and runs V15+ onwards, so the seeded-account retirement (V15) and schema alignment (V16) also reach existing installs. V15 and V16 are written to be safe on a Hibernate-built schema.
- **Testing migrations against MySQL**: H2 cannot run the MySQL-specific migrations, so `MySqlMigrationIntegrationTests` boots the app with the real configuration against a disposable MySQL server. It is skipped unless you pass one: `./mvnw test -Dtest=MySqlMigrationIntegrationTests -Dmysql.it.url=jdbc:mysql://127.0.0.1:3399/ -Dmysql.it.user=root`. Never point it at a real or shared database.
- **Never edit** an already-applied migration. Schema changes always go in a new migration file with the next version number.

## Development Credentials

> **Note:** The seeded admin and default resident accounts are deactivated by migration V15. On fresh databases, use the Master Admin bootstrap above to create the first administrative account. Residents must be onboarded by an admin.

| Role | Login | Notes |
|---|---|---|
| Master Admin | Configured via bootstrap | No default seeded password; created via one-time bootstrap variables above |
| Admin | Created by Master Admin | Master Admin promotes users to Admin role via the dashboard |
| Resident | Onboarded by Admin | Admin creates room assignments; residents complete profile setup on first login |
