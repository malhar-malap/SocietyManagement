# Jaswanti Residency - Housing Society Management System

A full-stack housing society management web application for **Jaswanti Residency** (C Wing, 80 rooms).

> **Product scope and development roadmap:** See [PROJECT_GUIDE.md](PROJECT_GUIDE.md) for the agreed MVP, Resident/Admin/Master Admin responsibilities, security and ledger rules, phased roadmap, and instructions for continuing development with Antigravity.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React, TailwindCSS, Lucide React, Axios |
| Backend | Spring Boot 4, Spring Security, JWT |
| Database | MySQL + Flyway migrations |
| Payments | Razorpay |
| Email | Gmail SMTP (JavaMailSender) |

## Project Structure

```
HousingSocietyManagement/
├── backend/JaswantiResidency/   # Spring Boot backend
└── jaswantiresidency/           # React frontend
```

## Getting Started

### Backend
1. Create a `.env` file in `backend/JaswantiResidency/` with:
```env
DB_URL=jdbc:mysql://localhost:3306/jaswanti_residency?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USER=root
DB_PASSWORD=your_db_password
JWT_SECRET=your_jwt_secret
RAZORPAY_API_KEY=your_razorpay_key
RAZORPAY_API_SECRET=your_razorpay_secret
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password
```
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

## Development Credentials

| Role | Login | Password | Notes |
|---|---|---|---|
| Admin | `admin@jaswanti.com` | `admin123` | Pre-seeded routine operational administrator |
| Resident | `resident602@jaswanti.com` (or room `C-602`) | `Temp_C-602` | Seed residents use pattern `Temp_<roomNumber>` and require profile/password setup on first login |
| Master Admin | Configured via bootstrap | Configured via bootstrap | No default seeded password; created via one-time bootstrap variables above |
