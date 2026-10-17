# Jaswanti Residency - Housing Society Management System

A full-stack housing society management web application for **Jaswanti Residency** (C Wing, 80 rooms).

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

## Default Credentials (Development)

| Role | Login | Password |
|---|---|---|
| Admin | admin@jaswanti.com | admin123 |
| Resident | resident602@jaswanti.com (or room number `C-602`) | resident123 |
