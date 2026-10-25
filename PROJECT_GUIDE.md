# Jaswanti Residency Society Management

This file is the durable product brief for this repository. Keep it current when scope or decisions change. It is intended to let a future developer or AI coding assistant continue work after a laptop or context change.

## Product purpose

Digitize the society's **resident and unit registry** and **financial ledger**. The system replaces scattered paper/spreadsheet records for who occupies each room, what the society charged, and what was paid. It is reliable, auditable, and understandable to residents and office staff.

This is not a generic property-management suite. It is tailored to the registered bylaws, bank accounts, and operational needs of Jaswanti Residency (C Wing, 80 rooms).

## Current repository

- Frontend: React 19 with React Router, Axios, TailwindCSS, and Lucide React (`jaswantiresidency/`). Unit tests run via `npm test -- --watchAll=false` (2 suites, 5 tests).
- Backend: Java 17 / 21, Spring Boot 4.1, Spring Security, JWT, JPA, Flyway (`backend/JaswantiResidency/`).
- Database: MySQL with Flyway migration versioning (`V1` through `V18`). Hibernate set to `ddl-auto=validate`.
- Email: SMTP (Gmail App Password) for one-time verification/recovery codes and payment receipt delivery.
- Payments: 100% screenshot-based offline payment submissions with admin approval and allocation to society bank accounts. **Razorpay has been completely removed** from backend and frontend.
- Test Suite: **248 automated backend tests** (`./mvnw test`): 245 run on H2 with 0 failures and 0 errors, and 3 MySQL integration tests are skipped unless `-Dmysql.it.url` points at a disposable MySQL server (all 248 pass when it does). CI (`.github/workflows/ci.yml`) runs both frontend unit tests and backend tests against a MySQL service and fails if any are skipped.

## Product decisions

### Roles and relationships

There are exactly three application roles:

1. **RESIDENT** — a person with a relationship to one or more society units. Resident type is separate data: `OWNER` or `TENANT`.
2. **ADMIN** — office in-charge/staff who perform routine registry, billing, and ledger work.
3. **MASTER_ADMIN** — committee-authorized system authority who manages privileged accounts, bank accounts, share certificates, and exceptional/high-impact actions.

People and units are many-to-many over time: a person can relate to multiple units; a unit can have multiple owners/tenants. Model a dated occupancy/ownership relationship (`RoomResident`: start/end dates, relationship type, primary contact flag, active status) rather than making a room permanently point to one user. Historical residents remain attached to past ledger entries. A resident sees records only for their active authorized unit relationships.

### Permission boundaries

| Capability | Resident | Admin | Master Admin |
|---|---|---|---|
| View personal profile and own linked unit ledger | Yes | Yes | Yes |
| View society-wide registry and ledger | No | Yes | Yes |
| Edit own contact details | Limited; verified fields only | Yes, operationally | Yes |
| Create/update resident, unit, and occupancy records | No; submit correction request | Yes, with audit history | Yes |
| Create bills and record/reverse payments | No | Yes (< 24h for reversals) | Yes (unrestricted reversals) |
| Manage bank accounts & live balances | No | Read-only | Full access |
| Upload/manage official Share Certificates | Own room download only | No | Full access |
| Approve/reject payment submissions | No | Yes | Yes |
| Manage announcements | Read-only | Read-only | Full access |
| Manage complaints, queries, suggestions, permissions | Own submissions only | Triage & respond | Full access |
| Grant, change, suspend, or remove Admin/Master Admin role | No | No | Yes |
| Change system-wide settings or perform exceptional overrides | No | No | Yes |

All authorization is enforced on the backend at the service/API level; hiding UI controls is not security. Resolve access from the authenticated identity and relationship records, never trust a client-supplied resident ID or role.

### Role-administration safeguards

- Bootstrap the first Master Admin through a one-time, documented, out-of-band setup process (`BOOTSTRAP_MASTER_ADMIN_ENABLED=true`); never expose a public endpoint that lets a caller become Master Admin.
- Keep at least two active Master Admin accounts where practical. Demotion, suspension, or deletion of the last active Master Admin is strictly prevented.
- Admins cannot grant themselves or others elevated roles. Only Master Admin can mutate privileged roles or statuses.
- Never physically delete financial entries or audit history. Use reversals/adjustments, and deactivate/archive people and units.
- Record actor, timestamp, target, before/after values, reason, and request metadata for sensitive actions via `ActivityLogService`. Audit records are append-only.

## Security Architecture & Hardening (Completed)

1. **JWT Hardening & Refresh Rotation:**
   - Dedicated token types (`typ` claim: `"access"` vs `"refresh"`). `JwtAuthenticationFilter` rejects refresh tokens on API endpoints; `/api/auth/refresh` rejects access tokens.
   - User account status verification: blocked (`status = BLOCKED`) or inactive accounts are rejected immediately by both the filter and refresh endpoint.
   - Token versioning (`token_version` on `users` table, Flyway `V14`): embedded in all tokens; incremented on password change, password reset, role modification, profile setup, and account deactivation, instantly revoking all outstanding tokens.
   - Refresh token rotation: each call to `/api/auth/refresh` issues both a new access token and a new rotated refresh token.
2. **Download IDOR Protection:**
   - Strict ownership checks on all sensitive file streaming endpoints: payment submission screenshots (`/api/payments/submissions/{id}/screenshot`), complaint attachments (`/api/complaints/{id}/attachment`), share certificates (`/api/rooms/{id}/share-certificate/download`), and bill/transaction PDFs.
   - Non-existent records return 404 Not Found; unauthorized access returns 403 Forbidden. Submissions are accessible only by Admins, Master Admins, the submitter, or active room co-residents.
3. **Magic-Byte File Upload Validation:**
   - Reusable `FileValidationService` inspects true magic byte signatures (JPEG, PNG, WEBP, PDF) rather than trusting client-supplied `Content-Type` headers.
   - Renamed executables, scripts, or HTML payloads with image headers are rejected with 400 Bad Request. Empty files and files exceeding the 5MB upload limit are rejected.
   - Enforced multipart limit in Spring Boot: `spring.servlet.multipart.max-file-size=5MB` and `max-request-size=10MB`.
4. **Brute-Force Login Rate Limiting:**
   - `LoginRateLimiterService` tracks failed authentication attempts per identifier (email or room number) and per client IP address.
   - 5 failures within a 15-minute rolling window triggers an automatic 15-minute lockout returning HTTP 429 Too Many Requests.
   - Generic error responses prevent account enumeration. Lockouts are recorded in `ActivityLogService`. Counter resets immediately upon successful login.
5. **Password Policy Enforcement:**
   - `PasswordValidator` enforces a minimum length of 8 characters and requires at least one letter and at least one digit.
   - Applied consistently across password reset, password change, and initial profile setup.
6. **Seeded Account Retirement & Fresh Schema Compatibility:**
   - Flyway migration `V15__deactivate_seeded_accounts.sql` deactivates the 81 accounts seeded by V2 (`admin@jaswanti.com`, `resident101`–`resident1008@jaswanti.com`) whose email and password hash are still exactly the seeded values, bumping `token_version` without deleting rows or breaking foreign key references.
   - Migration `V1_1__add_force_password_change_column.sql` repairs the V1/V2 schema gap on fresh databases.
   - `spring-boot-starter-flyway` added to pom.xml for Spring Boot 4.1 auto-configuration.
   - Existing databases baseline at V14 (`spring.flyway.baseline-version=14`), so V15 and later run on them too; V15/V16 are written to be safe on a Hibernate-built schema.
   - `V16__align_schema_with_entities.sql` creates the tables and columns that only Hibernate ever created (`activity_logs`, `system_notifications`, three `users` columns), adds `APPROVED`/`REJECTED` to `complaints.status`, and widens upload columns from 255-byte `TINYBLOB` to `LONGBLOB`.
   - `V17__extend_activity_logs.sql`: extends `activity_logs` (nullable `user_id`, actor snapshot, outcome, target, request columns, indexes). Entries are append-only: no API edits or deletes them.
   - `V18__setup_code_attempts.sql`: adds `users.email_verification_attempts` and clears pending setup codes. Email verification codes are 6-digit, BCrypt-hashed, with a 15-minute expiry and 5 maximum attempts before lockout.
   - `MySqlMigrationIntegrationTests` (opt-in via `-Dmysql.it.url`) boots the app against throwaway MySQL databases to prove fresh and existing databases migrate and pass `validate`.
7. **Hibernate Validation & Centralized CORS:**
   - `spring.jpa.hibernate.ddl-auto=validate` replaces `update`. Flyway is the sole schema authority.
   - Removed wildcard `@CrossOrigin(origins = "*")` from all 12 controllers.
   - Centralized CORS via `SecurityConfig` and `app.cors.allowed-origins` property (default `http://localhost:3000,http://localhost:5173`), supporting `PATCH`, `Idempotency-Key`, `Authorization`, and `Content-Type`.
8. **System Logs (Master Admin audit trail):**
   - Every state-changing endpoint carries `@AuditedAction(action, module, summary, target)` or `@AuditExempt(reason)`; `AuditCoverageTests` fails the build if a new POST/PUT/PATCH/DELETE endpoint has neither.
   - `AuditInterceptor` records each audited call after it completes, including FAILED and DENIED attempts: actor (with a name/email/role snapshot), action, module, outcome, target (resolved to a readable label before the action, so deletes keep it), description, method, path and socket IP. Only an allowlist of query parameters is ever logged; passwords, tokens and OTPs never are.
   - Ledger services keep writing their own entries inside the business transaction (audit and money stay atomic); the interceptor skips a request whose entry was committed and writes a FAILED entry when the operation rolled back. Idempotent payment replays are logged as `RECORD_PAYMENT_REPLAY`, never as a second payment.
   - `GET /api/system-logs` (Master Admin only) supports search, module/outcome/role/date filters and server-side paging; `/export` downloads the filtered set as CSV (formula-injection safe, itself audited). The old `/api/activity-logs` endpoint, which returned raw entities to admins, was removed; the admin sidebar's "Logs" page is replaced by **System Logs**, shown to Master Admins only.
9. **User Management & Password Lifecycle:**
   - `POST /api/users`: Admin creates residents; Master Admin may create admins. If password is left blank, a secure one-time temporary password is generated and returned for immediate copy.
   - `POST /api/users/{id}/reset-password`: Admins/Master Admins issue a new temporary password for a user, invalidating prior credentials and tokens.
   - Reactivating a never-onboarded user automatically issues a new temporary password.
   - Tokens accepted strictly in the `Authorization` header (`Bearer <token>`); tokens passed via query parameters or URLs are rejected.
10. **In-App Notification Architecture:**
    - Real-time event notifications wired to `SystemNotificationService` for key lifecycle events:
      - Resident submits payment proof -> all active admins notified.
      - Admin approves payment submission -> resident notified with receipt details.
      - Admin rejects payment submission -> resident notified with rejection reason.
      - Admin responds to or updates helpdesk status -> resident notified.
      - Master Admin posts an announcement -> broadcast notification to all users.
11. **API Cleanups & Removed Endpoints:**
    - Removed `POST /api/transactions` (preventing raw non-atomic ledger writes; all ledger entries require standard payment recording or approval).
    - Removed `GET /api/activity-logs` (replaced by audited `GET /api/system-logs`).
    - Removed dead frontend references to `POST /api/users/register`.


## Roadmap Status

- **Phase 0 (Product & Data Foundation):** COMPLETE.
- **Phase 1 (Identity, Roles, Access Control & Hardening):** COMPLETE.
- **Phase 2 (Registry, Occupancy & Commercial Units):** COMPLETE.
- **Phase 2B (Commercial Units, Dual NOC & Share Certificates):** COMPLETE.
- **Phase 3 (Core Ledger, Decimal Money & Manual Payments):** COMPLETE.
- **Phase 3D (Bank Accounts, Live Balances & 24h Edit Lock):** COMPLETE.
- **Phase 3E (Screenshot Submissions & Multi-Month Advances):** COMPLETE.
- **Phase 3F (5-Year Maintenance Range, PDF/Email Receipts & Excel Operations):** COMPLETE.
- **Phase 4A (Queries, Suggestions & Permissions Module):** COMPLETE.
- **Phase 4B (Bank Reconciliation & System Polish):** COMPLETE.
- **Phase 4C (Security Hardening — Steps 1 through 8):** COMPLETE.
- **Phase 4D (System Logs for Master Admins):** COMPLETE.

## Deferred / Future Scope (Post-MVP)

The following items are deferred for post-launch releases:

1. **Automated Bank Feeds & Account Aggregator:** Direct integration with bank APIs (Open Banking / Account Aggregator) to automatically pull society bank statements. Currently fulfilled via live bank ledger, manual receipting, and 100% discrepancy-free bank reconciliation.
2. **SMS Gateway Integration:** SMS/WhatsApp notifications for overdue maintenance bills. Currently fulfilled via email notifications and in-app system notifications.
3. **Multi-Wing Architecture:** Expansion beyond C Wing (80 units) to support multiple wings, separate building accounts, or multi-society deployments.
4. **Visitor & Gate Management:** Security guard portal, visitor pre-approvals, delivery tracking, and vehicle parking management.
5. **Facility Reservations & Asset Inventory:** Clubhouse/terrace booking system, society asset tracking (generators, pumps, lifts), and vendor AMC management.
6. **Double-Entry Bookkeeping:** Chart of accounts, depreciation schedules, and automated balance sheet / trial balance generation for chartered accountants.
