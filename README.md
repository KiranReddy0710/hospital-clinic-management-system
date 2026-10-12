# MediCare — Hospital & Clinic Management System

A professional starter full-stack project based on the uploaded abstract:
- Frontend: HTML, CSS, Vanilla JavaScript
- Backend: Java 17+, Spring Boot 3.3, Spring Web, Spring Data JPA, Bean Validation
- Database: MySQL
- REST API with patient, doctor, and appointment CRUD
- Gmail/password accounts with admin, doctor, and patient roles

> **Important:** This is a runnable academic starter, not a clinically validated or production-hardened healthcare system. Before real patient data is used, add audited authentication/authorization, HTTPS, secrets management, backups, privacy controls, logging, security review, and jurisdiction-specific compliance. Do not use real patient information in the demo.

## Requirements
- JDK 17 or newer
- Maven 3.9+
- MySQL 8+
- VS Code or IntelliJ IDEA (optional)

## 1. Create the database
Start MySQL, then run:
```bash
mysql -u root -p < database/schema.sql
```
Or open `database/schema.sql` in MySQL Workbench and execute it.

## 2. Configure database credentials
Set environment variables before starting the backend (recommended):
```bash
export DB_URL='jdbc:mysql://localhost:3306/medicare_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME='root'
export DB_PASSWORD='YOUR_MYSQL_PASSWORD'
```
If your MySQL account has no password, set `DB_PASSWORD=''`.

## 3. Start backend
```bash
cd backend
mvn spring-boot:run
```
API base URL: `http://localhost:8080/api`

Health check: `http://localhost:8080/api/health`

## 4. Start frontend
Open `frontend/index.html` in a browser. The frontend calls `http://localhost:8080/api`.
For the smoothest local setup, use VS Code Live Server. If your browser blocks `file://` requests, run:
```bash
cd frontend
python3 -m http.server 5500
```
Then open `http://localhost:5500`.

## Deploy on Railway
This repository includes a Dockerfile and Railway health check configuration. The Spring Boot app serves the frontend and API from one public URL.

1. Push this repository to GitHub, then create a Railway project from that repository.
2. Add a Railway MySQL service to the project.
3. In the application service, add shared variable references for `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, and `MYSQLDATABASE` from the MySQL service. The backend reads these variables to connect to the database.
4. Generate a strong one-time secret (for example, `openssl rand -hex 32`) and set it as `APP_AUTH_ADMIN_BOOTSTRAP_TOKEN` in the app service variables. The session cookie is secure by default on Railway; set `SESSION_COOKIE_SECURE=false` only for local HTTP development.
5. Deploy the app service. Railway uses the included Dockerfile and checks `/api/health`.
6. Open the generated public domain, choose **Admin setup**, and create the first administrator. Remove `APP_AUTH_ADMIN_BOOTSTRAP_TOKEN` from Railway after setup; it stops working once an admin account exists.

The first admin can create one-time, seven-day invite codes for doctors and additional admins. Patients can register themselves. Passwords are hashed, sessions use CSRF protection, and the API enforces roles. Email addresses are not verified; this remains an academic demo and is not clinically validated or compliant for real patient data. Use synthetic data only.

## Features
- Dashboard overview and appointment status counts
- Patient CRUD
- Doctor CRUD
- Appointment create/list/status update/delete
- Search across patient, doctor and appointment tables
- Patient self-registration with Gmail address and password
- Invite-only doctor/admin registration and first-admin bootstrap
- Server-side role checks, password hashing, CSRF protection, and session authentication
- Responsive navigation and UI

## REST endpoints
- `GET /api/health`
- `GET /api/auth/csrf`, `GET /api/auth/me`
- `POST /api/auth/register/patient`, `/api/auth/register/invited`, `/api/auth/register/bootstrap`
- `POST /api/auth/login`, `/api/auth/logout`, `/api/auth/invites` (admin only)
- `GET /api/dashboard`
- `GET|POST /api/patients`
- `GET|PUT|DELETE /api/patients/{id}`
- `GET|POST /api/doctors`
- `GET|PUT|DELETE /api/doctors/{id}`
- `GET|POST /api/appointments`
- `GET|PUT|DELETE /api/appointments/{id}`

## Troubleshooting
- **Access denied:** confirm `DB_USERNAME` / `DB_PASSWORD`.
- **Unknown database:** run `database/schema.sql`.
- **Port 8080 in use:** stop the other process or change `server.port` in `application.properties`.
- **Maven not found:** install Maven or use your IDE's Maven integration.
- **CORS:** backend permits local development origins on ports 5500, 5501, and 3000.

## Production hardening checklist
1. Add email verification and rate limiting before wider public use.
2. Add pagination, DTO mapping, database migrations, monitoring, and backups.
3. Have security and clinical stakeholders review workflows before deployment.
