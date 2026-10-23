# Jaswanti Residency Society Management

This file is the durable product brief for this repository. Keep it current when scope or decisions change. It is intended to let a future developer or AI coding assistant continue work after a laptop or context change.

## Product purpose

Digitize the society's **resident and unit registry** and **financial ledger**. The first usable release should replace scattered paper/spreadsheet records for who occupies each room, what the society charged, and what was paid. It should be reliable, auditable, and understandable to residents and office staff.

This is not initially a full property-management suite. Build the trustworthy registry and ledger first; defer convenience and automation until those records are correct.

## Current repository

- Frontend: React 19 with React Router, Axios, and Create React App (`jaswantiresidency/`).
- Backend: Java 17, Spring Boot, Spring Security, JWT, JPA, Flyway (`backend/JaswantiResidency/`).
- Database: MySQL.
- Email: SMTP (currently Gmail SMTP configuration) for one-time verification/recovery codes and essential account messages.
- Existing areas include authentication, resident/admin pages, rooms, maintenance bills, Razorpay payment code, complaints, announcements, transactions, notifications, and activity logs.
- Current role enum only has `ADMIN` and `RESIDENT`; the UI and many endpoint checks assume those two values.
- Existing implementation has known gaps: password reset is not actually implemented safely; residents can reach an unfiltered all-bills endpoint; payment verification needs ownership/duplicate/idempotency checks; money is represented as floating point; schema is managed by both Flyway and Hibernate update. Treat these as verified priorities to investigate/fix before production.
- The repository may have pre-existing working-tree changes. Inspect `git status` before edits and preserve changes that are not yours.

## Product decisions

### Roles and relationships

There are exactly three application roles:

1. **RESIDENT** — a person with a relationship to one or more society units. Resident type is separate data: `OWNER` or `TENANT`. Do not create separate Owner and Tenant login roles.
2. **ADMIN** — office in-charge/staff who perform routine registry, billing, and ledger work.
3. **MASTER_ADMIN** — committee-authorized system authority who manages privileged accounts and exceptional/high-impact actions.

Use `MASTER_ADMIN` as the technical role name. In the UI, display “Master Admin” and optionally store a separate committee title/name. Do not call this role `SUPER_ADMIN` in one layer and `MASTER_ADMIN` in another.

People and units are many-to-many over time: a person can relate to multiple units; a unit can have multiple owners/tenants. Model a dated occupancy/ownership relationship (start/end dates, relationship type, primary contact flag, status) rather than making a room permanently point to one user. Historical residents must remain attached to past ledger entries. A resident sees records only for their active authorized unit relationships.

### Permission boundaries

| Capability | Resident | Admin | Master Admin |
|---|---|---|---|
| View personal profile and own linked unit ledger | Yes | Yes | Yes |
| View society-wide registry and ledger | No | Yes | Yes |
| Edit own contact details | Limited; verified fields only | Yes, operationally | Yes |
| Create/update resident, unit, and occupancy records | No; submit correction request | Yes, with audit history | Yes |
| Create bills and record/reverse payments | No | Yes, with audit trail and reason for corrections | Yes |
| Manage announcements/complaints | Own submissions/status only as applicable | Routine operations | Full access |
| Grant, change, suspend, or remove Admin/Master Admin role | No | No | Yes |
| Change system-wide settings or perform exceptional overrides | No | No | Yes |

All authorization is enforced on the backend, at service/API level; hiding UI controls is not security. Resolve access from the authenticated identity and relationship records, never trust a client-supplied resident ID or role.

### Role-administration safeguards

- Bootstrap the first Master Admin through a one-time, documented, out-of-band setup process; never expose a public endpoint that lets a caller become Master Admin.
- Keep at least two active Master Admin accounts where practical. Prevent demotion, suspension, or deletion of the last active Master Admin.
- Admins cannot grant themselves or others elevated roles. Only Master Admin can change privileged roles.
- Require re-authentication and a reason for privileged role changes and exceptional financial corrections.
- Never physically delete financial entries or audit history. Use reversals/adjustments, and deactivate/archive people and units.
- Record actor, timestamp, target, before/after values, reason, and request metadata for sensitive actions. Audit records are append-only to normal app users.

### Master Admin bootstrap (implemented foundation)

The first Master Admin is created only by an authorized deployment operator, not through an HTTP endpoint. For a one-time bootstrap, set these environment variables in the backend process environment:

```text
BOOTSTRAP_MASTER_ADMIN_ENABLED=true
BOOTSTRAP_MASTER_ADMIN_EMAIL=<operator-controlled email>
BOOTSTRAP_MASTER_ADMIN_NAME=<operator-controlled name>
BOOTSTRAP_MASTER_ADMIN_PASSWORD=<temporary strong password>
```

Start the backend once with those values present, confirm the account was created, and then remove the values or set `BOOTSTRAP_MASTER_ADMIN_ENABLED=false` before normal operation. Bootstrap fails closed when any value is missing, when the email is already used, or when an active Master Admin already exists. The password is stored hashed and the account is marked to require a password change. No default email/password is created and no credentials are printed by the application.

### First release scope (MVP)

Must have:

- Secure login and account invitation/activation; SMTP-delivered, expiring, single-use OTP for verification and password recovery, with rate limits and attempt limits.
- Registry for units and people, Owner/Tenant relationships, contact details, active/inactive state, and move-in/move-out history.
- Admin-maintained monthly/periodic charges associated with a unit and period; unique bill per unit/period/type.
- Manual payment entry for cash/bank/other modes, receipt/reference details, and printable/downloadable receipts.
- Ledger views: unit statement and society-wide totals for charges, collections, and outstanding balance, with date/period filters and CSV export.
- Resident read-only view of their own unit ledger and registry details.
- Role-based backend authorization, server-side validation, append-only audit history, and useful error responses.
- Database migrations, environment-based configuration, backup/restore instructions, and basic automated tests for permission and ledger rules.

Explicitly defer from the first release unless the society confirms a need:

- Razorpay/online payments and automatic reconciliation.
- Automated late-fee policies, interest, refunds, split/partial payment rules (manual adjustments may be recorded, but policy automation waits).
- Complex accounting, chart of accounts, vendor/payables, budgets, tax filing, and double-entry bookkeeping.
- Visitor/gate management, facility reservations, asset/maintenance inventory, polls, chat, mobile apps, and extensive notification automation.
- Complaint system expansion beyond a simple intake/status flow if that is not required for the ledger launch.

## Roadmap

### Phase 0 — Product and data foundation

- Confirm society rules for unit identifiers, billing cycle, charge types, payment modes, receipt numbering, and who can approve corrections.
- Map existing schema and data; decide migration/backfill approach before changing relationships or role enum.
- Write acceptance criteria and permission matrix for each endpoint/page.
- Establish safe dev/staging configuration and ensure no production secrets or real resident data are committed.

**Exit:** agreed MVP rules and migration plan; current code and data risks documented.

### Phase 1 — Identity, roles, and access control

- Add `MASTER_ADMIN` role and safe initial bootstrap. Migrate existing admin account deliberately.
- Implement invitation/activation and SMTP OTP flows: random secure codes, hashed storage, expiration, single use, resend cooldown, attempt/abuse limits, generic responses that avoid account enumeration, and no OTP in logs.
- Implement actual password reset and password change; require password change for temporary credentials.
- Review every controller and service for role and record ownership. Residents must only read their own linked records.
- Avoid browser-persisted long-lived bearer tokens where feasible; define refresh-token rotation/revocation and logout behavior.

**Exit:** role and ownership tests pass; no public self-promotion; OTP and recovery are functional and bounded.

### Phase 2 — Registry and occupancy history

- Model units, people, and dated Owner/Tenant relationships with multiple people per unit and multiple units per person.
- Build admin workflows for create/edit/search/archive and move-in/move-out/transfer.
- Preserve historical links and prevent overlapping/invalid active occupancy according to agreed rules.
- Add resident profile and correction-request workflow; sensitive registry changes are attributable.

**Exit:** staff can maintain the real registry without deleting history; residents see only authorized unit data.

### Phase 3 — Core ledger

- Use decimal money types (`BigDecimal` and fixed-precision database columns), never `float`/`double` for money.
- Define charge and bill model with unique unit + period + charge type; make batch generation repeat-safe.
- Record manual payment entries with mode, date, reference/receipt, actor, and allocation to bills.
- Correct errors through reversal/adjustment entries with reasons; do not silently overwrite or delete posted ledger activity.
- Provide unit statements, outstanding balances, collection summaries, filters, and CSV/PDF receipts/reports.
- Make ledger updates transactional and prevent duplicate or inconsistent posting.

**Exit:** a sample billing cycle reconciles from bills to payments to balances, with audit history and exports.

### Phase 4 — Operational polish and release readiness

- Finish basic complaint/announcement workflows only to the level needed for initial operations.
- Remove or explicitly defer incomplete Razorpay functionality; never expose a half-secured payment flow.
- Set Flyway as the production schema authority (`ddl-auto=validate` or equivalent); add indexes/constraints and migration checks.
- Add structured logs, health checks, secure CORS/HTTPS deployment settings, backups and tested restore procedure.
- Replace placeholder tests with backend integration and frontend flow tests for login, roles, registry, billing, payment entry, and reporting.
- Document setup, SMTP configuration, Master Admin bootstrap, migration, deployment, backup/restore, and support procedures.

**Exit:** staging walkthrough by committee and office staff; backup restore and release checklist completed.

## Working with Antigravity

Use `PROJECT_GUIDE.md` as the source of truth. For each coding task, give Antigravity one bounded phase/task at a time. Require it to:

1. Read this guide and inspect relevant code before editing.
2. State a short implementation plan and list files/schema/API changes.
3. Preserve unrelated worktree changes and avoid broad rewrites.
4. Enforce authorization on the backend, validate input, and add/update targeted tests for the behavior changed.
5. Add Flyway migrations for schema changes; do not edit already-applied migrations to change production history.
6. Run the smallest relevant checks, report exact commands/results, and identify unfinished work.
7. Update this guide when a product decision, scope, or phase status changes.

Do not ask an AI coding tool to implement the entire roadmap in one prompt. Start with Phase 0 and Phase 1 security/access-control fixes, then implement the registry, then the ledger.

## Open policy questions (default decisions)

These are defaults to use until the society decides otherwise:

- One login per person; a person may be linked to multiple units; a unit may have multiple owners/tenants.
- Each unit has one primary ledger account; payments and charges belong to the unit, not an individual occupant.
- Residents can view statements but cannot post or modify financial entries.
- Admin may post ordinary charges and payments; reversals/adjustments require a reason and are fully audited. Master Admin reviews exceptional actions.
- Use manual payment recording for MVP; online payment is deferred.
- Archive residents/units rather than delete them. Preserve all ledger and audit history.
- Society-specific fee, due-date, late-fee, partial-payment, refund, and approval policies must be confirmed before automating those rules.

## Current roadmap status

- Phase 0: initial product decisions captured here; detailed data/backfill assessment still required.
- Phase 1: partial. `MASTER_ADMIN` role persistence, controlled out-of-band bootstrap, routine Admin-workflow access, privileged role/status endpoint restrictions, Master Admin user-management UI (role promotion/demotion, account blocking/reactivation, account deactivation), clear safeguard/conflict error presentation, and last-active-Master-Admin protection are implemented. Secure SMTP OTP password recovery is implemented. Re-authentication/reason requirements, privileged-action audit history, refresh-token rotation/revocation, and full multi-person occupancy coverage remain outstanding.
- Phase 2: partial. Dated unit-person relationship model (`RoomResident`) implemented with `OWNER`/`TENANT` relationship types, start/end dates, primary-contact flags, active state, Flyway migration backfill (`V5`), and documentation migration (`V6` documenting the limitation of legacy backfill placeholder start dates, intentionally avoiding ambiguous automated bulk updates). Note on migration history: local development MySQL does not have `flyway_schema_history` initialized; for any external/shared/production database, migration application status remains **unresolved** until directly verified via `SELECT version, checksum, installed_on FROM flyway_schema_history;`. Occupancy lifecycle validations (active RESIDENT assignment only, duplicate prevention, date ordering/null constraints, future start date rejection on both assignment and date correction, clear client errors on invalid/inactive transitions), privacy boundaries for `GET /api/rooms/{id}/occupants` (residents see active occupants only with no former occupant data; admins see full history), and safe legacy fallback protections against stale `rooms.resident` data are fully implemented and verified with integration tests. Admin/Master Admin occupancy registry UI implemented on the Room Directory screen to inspect active/historical occupants, assign eligible RESIDENT-only occupants with relationship type, local start date (avoiding UTC date shifts), and primary contact status, and end active occupancies with end dates. For historical records, an inline date-correction form is available exclusively to Admin and Master Admin to update start and end dates with a mandatory reason, invoking the audited backend endpoint `PUT /api/rooms/occupants/{id}/correct-dates`. Date correction fails closed: a resolvable authenticated Admin/Master Admin actor is required, and both the date update and append-only `ActivityLog` entry (recording actor, before/after dates, and reason) execute in the same transaction; if actor resolution or audit persistence fails, the correction rolls back. For direct database maintenance, a reviewed manual database correction procedure is documented for operators. Formal move-in/move-out/transfer wizard workflows and resident correction-request submission flow remain to be completed.
- Phase 3: in progress (Phase 3A, 3B, 3C, and 3D complete). Core ledger foundation, uniqueness, authorization, manual payments, live bank accounts, and reversals:
  1. **BigDecimal Migration (Implemented Phase 3A):** Converted `amount` and `late_fee` in `MaintenanceBill`, `MaintenanceBillDto`, `Transaction`, `TransactionDto`, `PaymentAttempt`, `PaymentRequestDto`, and `OrderResponseDto` to `BigDecimal` with JPA `@Column(precision = 12, scale = 2)`. Inputs and calculation boundaries normalize to scale 2 with `RoundingMode.HALF_UP`. Floating-point arithmetic removed from `MaintenanceBillService`, `PaymentService`, `AdminDashboardController`, and `PdfInvoiceGenerator`. Overloaded `Double` constructors and setters preserved for backwards compatibility with `@JsonSetter`/`@JsonIgnore` to guarantee unambiguous Jackson deserialization.
  2. **Flyway Migration `V7` (Implemented Phase 3A):** Added `V7__ledger_money_decimal.sql` converting `maintenance_bills.amount`, `maintenance_bills.late_fee`, `transactions.amount`, and `payment_attempts.amount` to `DECIMAL(12, 2)`, preserving existing values safely, with documented pre-check queries for values outside the `DECIMAL(12, 2)` range.
  3. **Unit-Period-Charge Uniqueness & Idempotent Generation (Implemented Phase 3B):** Added `ChargeType` enum (`MAINTENANCE`, `SINKING_FUND`, `SPECIAL_ASSESSMENT`, `PENALTY`), `charge_type` column on `MaintenanceBill` (default `MAINTENANCE`), `uk_room_month_charge` compound database constraint on `(room_id, billing_month, charge_type)`, Flyway migration `V8__bill_charge_type_uniqueness.sql`, pre-check query confirming 0 pre-existing duplicate bills, `DuplicateBillException` handling with HTTP 409 Conflict, and idempotent monthly bill generation.
  4. **Canonical Billing Period & Legacy Compatibility (Implemented Phase 3B Cleanup):** Enforced canonical `YYYY-MM` format (`^\d{4}-(0[1-9]|1[0-2])$`, e.g. `2026-10`) across all bill creation and generation paths (`POST /api/bills`, `POST /api/bills/generate-monthly`, `PUT /api/bills/{id}`). Malformed or non-canonical strings (including text representations like `"October 2026"`, single-digit months `"2026-1"`, or month numbers outside 01-12) are strictly rejected for new records with HTTP 400 Bad Request. Legacy compatibility is fully preserved: existing historical bills with textual month strings (such as `"October 2026"`) remain unmutated in the database, can be read via all bill retrieval endpoints (`GET /api/bills/{id}`, `GET /api/bills/room/{roomId}`, `GET /api/bills/my-bills`), and can be marked paid. Duplicate detection checks logical equivalence across canonical and legacy representations (`existsByRoomIdAndBillingMonthInAndChargeType`), preventing duplicate bills from being created for periods already represented by historical bills without requiring risky database rewrites or schema migrations.
  5. **Relationship-Based Ledger Authorization (Implemented Phase 3B):** Replaced legacy `rooms.resident_id` authorization in `MaintenanceBillController`, `TransactionController`, `PaymentController`, and `DashboardController` with active `RoomResident` relationship checks (`existsByRoomIdAndResidentIdAndIsActiveTrue` and `findByResidentIdAndIsActiveTrue`). Multiple active occupants (Owner, Tenant) can access the same unit ledger; former residents and non-occupants are denied (HTTP 403); Admin and Master Admin retain society-wide access; safe fallback applies only when a unit has no relationship records at all.
  6. **Audited Manual Payment Recording & Compatibility Wrapper (Implemented Phase 3C):** Added `POST /api/bills/{billId}/record-payment` restricted strictly to Admin and Master Admin (residents receive HTTP 403). Accepts `RecordPaymentRequest` with positive `BigDecimal` amount (scale <= 2), `PaymentMode` (`CASH`, `BANK_TRANSFER`, `CHEQUE`, `UPI_MANUAL`), mandatory `referenceNumber` for non-cash modes, non-future `paymentDate`, and optional notes/reason. Bill and authenticated actor are resolved strictly from path and security principal. Allows partial payments (transitions bill to `PARTIALLY_PAID`) and full payments (transitions to `PAID` or `PAID_CASH`); rejects zero/negative amounts, overpayments exceeding outstanding balance, and payments on reversed bills. Implemented monotonic, gap-free receipt number generation (`REC-YYYYMM-00001`) backed by transactional pessimistic locking on `receipt_sequences` table. Retries are idempotent via `Idempotency-Key` header/body or non-cash reference reuse check, returning existing payment details and stable receipt numbers without duplicate transaction posting. Created Flyway migration `V9__manual_payment_recording.sql` modifying `maintenance_bills.status` to `VARCHAR(32)`, adding `paid_amount` column with backfill, adding metadata columns to `transactions` (`recorded_by_id`, `reference_number`, `payment_date`, `notes`, `receipt_number`, `idempotency_key`), unique constraints on receipt number and idempotency key, foreign key to `users`, and `receipt_sequences` table. Every payment writes an append-only `ActivityLog` in the same transaction, failing closed and rolling back all state if payment or audit persistence fails.
     - **Legacy Endpoint Compatibility Wrapper (`PUT /api/bills/{id}/mark-paid-cash`):** Converted from direct entity mutation into a safe delegating wrapper around `PaymentRecordingService.recordPayment(...)`. It records an official cash payment for the remaining balance with deterministic idempotency key `"LEGACY-CASH-" + id`, generates an official receipt, writes a transaction, updates `paid_amount` and status, attributes the authenticated admin actor, and commits an `ActivityLog` entry. It emits `X-Deprecated: true` and `Warning: 299 - "PUT /api/bills/{id}/mark-paid-cash is deprecated; please use POST /api/bills/{id}/record-payment instead"` response headers while returning the expected `MaintenanceBillDto` so existing clients continue functioning without unaudited ledger bypasses.
  7. **Society Bank Accounts, Live Balances, Income/Expense Ledger & 24-Hour Edit Lock (Implemented Phase 3D):**
     - **Bank Accounts & Running Balance:** Added `SocietyBankAccount` entity and Flyway migration `V10__bank_accounts_and_ledger_lock.sql` creating `society_bank_accounts` (`account_name`, `bank_name`, `account_number`, `ifsc_code`, `account_type` [`BANK`, `CASH_LEDGER`], `opening_balance`, `current_balance`, `is_active`). Endpoints `GET /api/bank-accounts` (accessible to Admin & Master Admin) and `POST`/`PUT /api/bank-accounts` (restricted strictly to Master Admin with audit logging).
     - **Live Balance Automatic Updates:** Added `bank_account_id` (FK), `entry_direction` (`CREDIT`, `DEBIT`), `category` (`MAINTENANCE`, `STAFF_SALARY`, `DAILY_WAGES`, `UTILITY_EXPENSE`, `VENDOR_PAYMENT`, `OTHER_INCOME`), and `approved_at` to `transactions`. Transaction updates use pessimistic write locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on `SocietyBankAccount`: `CREDIT` payments automatically add to `current_balance`, and `DEBIT` expenses automatically subtract.
     - **Expense Recording Endpoint:** Added `POST /api/transactions/expense` for recording staff salaries, daily wages, and utility expenses against a chosen Bank or Cash account.
     - **Audited Reversals (`POST /api/transactions/{id}/reverse`):** Immutability preserved by marking the original transaction `REVERSED` and creating a compensating `REVERSAL` transaction; updates live bank account balance and restores unit bill balance and status in the same transaction with `ActivityLog`.
     - **24-Hour Admin Edit Lock Safeguard & Master Admin Queue:** `ADMIN` cannot directly reverse or edit transactions once 24 hours have elapsed since `approved_at`. Within 24h, `ADMIN` can reverse immediately (HTTP 200); after 24h, the request is placed into `transaction_amendment_requests` returning HTTP 202 Accepted. `MASTER_ADMIN` can reverse directly at any time or review and approve/reject queued requests via `GET /api/transactions/amendment-requests` and `POST /api/transactions/amendment-requests/{id}/resolve`.
     - **Frontend Integration:** Updated `AdminDashboard.jsx` and `AdminTransactions.jsx` with live Bank/Cash balance cards, Expense Recording modal, 24-hour edit lock badges (`Editable (<24h)` vs `Locked (>24h)`), and Master Admin Approvals Queue dialog.
   8. **100% Screenshot-Based Resident Payment Submissions & Multi-Month Advance (Implemented Phase 3E):**
      - **Razorpay Removal:** Completely removed/disabled online Razorpay checkout and order verification endpoints and UI triggers. All resident payments flow through verified offline screenshot submissions.
      - **Database Migration `V11`:** Created `V11__resident_payment_submissions.sql` establishing `resident_payment_submissions` (`room_id`, `submitted_by_user_id`, `amount`, `payment_mode`, `reference_number`, `screenshot_data`, `screenshot_filename`, `screenshot_content_type`, `resident_description`, `selected_bill_ids_json`, `advance_months_count`, `status`, `reviewed_by_id`, `review_notes`, `society_bank_account_id`).
      - **Resident Submission API:** Added `POST /api/payments/submissions` (multipart/form-data) allowing residents to submit payment proofs for outstanding bills and multi-month advances, with authenticated attachment viewing via `GET /api/payments/submissions/{id}/screenshot`.
      - **Admin Review & Multi-Month Allocation:** Added `POST /api/payments/submissions/{id}/approve` and `POST /api/payments/submissions/{id}/reject`. Upon approval, the operator allocates funds to a specific `SocietyBankAccount` with automatic balance update. The system transactionally settles selected unpaid bills, creates advance future bills as needed (`ADVANCE_MAINTENANCE`), generates individual receipted transactions with unique reference suffixes (`-B{id}`, `-ADV-{month}`) to avoid idempotency collisions, and writes audit logs.
      - **Frontend Components:** Created `PendingPaymentApprovalsModal.jsx` with full screenshot image preview and society bank selector, updated `PaymentModal.jsx` and `ResidentInvoices.jsx` for resident submission, and added pending approval badges on `AdminBilling.jsx`.

### Phase 2B — Commercial Units, Dual Calculation, and Share Certificates (Implemented)
- **Database Migration `V12`:** Created `V12__commercial_units_noc_share_cert.sql` adding `unit_category` (`RESIDENTIAL`, `COMMERCIAL`), `noc_applicable`, `noc_amount`, `share_certificate_number`, `share_certificate_issued_date`, `share_certificate_data`, `share_certificate_filename`, and `share_certificate_content_type` to `rooms`. Added `NOC` to `ChargeType` enum.
- **Dual Calculation:** Updated `MaintenanceBillService` monthly bill generation to detect commercial units with `noc_applicable = true` and automatically generate a companion `NOC` charge bill in addition to the standard `MAINTENANCE` bill.
- **Share Certificate Management:** Added endpoints `POST /api/rooms/{id}/share-certificate` (restricted strictly to Master Admin), `GET /api/rooms/{id}/share-certificate`, and `GET /api/rooms/{id}/share-certificate/download`.
- **Frontend Integration:** Added `RoomEditModal.jsx` with unit category and NOC configuration, `ShareCertificateModal.jsx` for Master Admin upload and preview, and updated `AdminRooms.jsx` and `ResidentProfile.jsx` to download official share certificates.

### Phase 3F — 5-Year Maintenance Range, PDF/Email Receipts, and Excel Operations (Implemented)
- **5-Year Maintenance Range:** Extended `MaintenanceBillController` and `TransactionController` with financial year filtering (`financialYear` e.g. `2026-2027` covering April through March) and arbitrary range queries (`fromMonth` and `toMonth`).
- **PDF Generation & Email Receipts:** Added PDF payment receipt generator in `PdfInvoiceGenerator` and email dispatch in `EmailService` with PDF attachment (`GET /api/bills/{id}/invoice-pdf`, `GET /api/transactions/{id}/receipt-pdf`, `POST /api/transactions/{id}/email-receipt`).
- **Excel Bulk Operations:** Added Apache POI (`poi-ooxml 5.2.5`) integration via `ExcelBillingService`. Exposes `GET /api/bills/export-excel` (exporting multi-year bills with formatting), `GET /api/bills/import-template` (providing standard spreadsheet upload template), and `POST /api/bills/import-excel` (bulk importing bills with room number resolution, format validation, and duplicate conflict reporting).
- **Frontend Integration:** Added Financial Year dropdown filter and Excel export/template/import buttons in `AdminBilling.jsx`, and receipt download and email buttons in `AdminTransactions.jsx`.

### Phase 4A — Queries, Suggestions & Permissions Module and Role Boundaries (Implemented)
- **Database Migration `V13`:** Created `V13__queries_suggestions_permissions.sql` adding `submission_type` (`COMPLAINT`, `QUERY`, `SUGGESTION`, `PERMISSION`), `permission_category` (`RENOVATION`, `INTERIOR_WORK`, `MOVE_IN_OUT`, `ADDRESS_PROOF`, `OTHER`), `start_date`, `end_date`, `admin_response`, `image_data`, `image_filename`, and `image_content_type` to `complaints`. Added `APPROVED` and `REJECTED` to `ComplaintStatus`.
- **3-Way Submissions & Permissions Workflow:** Updated `ComplaintController` and `ComplaintService` to support file attachments, date windows, official admin responses (`POST /api/complaints/{id}/respond`), and secure attachment streaming (`GET /api/complaints/{id}/attachment`) with room/resident ownership checks.
- **Strict Role Partitioning:** Restricted `AnnouncementController` creation, updating, and deletion strictly to `MASTER_ADMIN` (`hasRole('MASTER_ADMIN')`). Restricted `UserController` deletion, status updates, and role mutations strictly to `MASTER_ADMIN` with last-active Master Admin protection.
- **Frontend Integration:** Updated `AdminComplaints.jsx` with tabbed filtering across Complaints, Queries, Suggestions, and Permission Requests; updated `UpdateComplaintModal.jsx` with official response input and Approval/Rejection buttons; updated `ResidentComplaints.jsx` with 3-way submission selector and date range inputs.

### Phase 4B — Bank Reconciliation & System Polish (Implemented)
- **Bank Reconciliation Endpoints:** Implemented `GET /api/bank-accounts/reconcile` (society-wide reconciliation across all bank accounts and cash ledgers) and `GET /api/bank-accounts/{id}/reconcile` (specific account reconciliation).
- **Mathematical Integrity Validation:** Evaluates `openingBalance + SUM(credits) - SUM(debits)` from transactions with status in `['SUCCESS', 'REVERSED', 'REVERSAL']` against `currentBalance`, verifying exact `0.00` discrepancy.
- **Frontend Reconciliation Dashboard:** Integrated "Reconcile Accounts" action and modal in `AdminTransactions.jsx` displaying live reconciliation breakdown (opening balance, total credits, total debits, calculated balance, live balance, discrepancy, and 100% Reconciled badge).
- **Test Suite & Verification:** 152/152 backend integration tests passing (`BUILD SUCCESS`), frontend React production build passing (`npm run build`), and clean `git diff --check`.

