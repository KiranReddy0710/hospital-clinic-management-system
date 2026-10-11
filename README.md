# MediCare — Hospital & Clinic Management System

A professional starter full-stack project based on the uploaded abstract:
- Frontend: HTML, CSS, Vanilla JavaScript
- Backend: Java 17+, Spring Boot 3.3, Spring Web, Spring Data JPA, Bean Validation
- Database: MySQL
- REST API with patient, doctor, and appointment CRUD
- Demo role-based UI (Admin / Doctor / Patient) and dashboard metrics

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
4. Deploy the app service. Railway uses the included Dockerfile and checks `/api/health`.
5. Open the generated public domain. The frontend uses the same domain for API requests.

Do not expose real patient information through this demo. It has no authentication or authorization, and its CRUD API is publicly accessible after deployment. Use synthetic data only until proper access controls and privacy safeguards are implemented.

## Features
- Dashboard overview and appointment status counts
- Patient CRUD
- Doctor CRUD
- Appointment create/list/status update/delete
- Search across patient, doctor and appointment tables
- Demo role selector for UI preview (not security; server-side authentication is not included in this starter)
- Responsive navigation and UI

## REST endpoints
- `GET /api/health`
- `GET /api/dashboard`
- `GET|POST /api/patients`
- `GET|PUT|DELETE /api/patients/{id}`
- `GET|POST /api/doctors`
- `GET|PUT|DELETE /api/doctors/{id}`
- `GET|POST /api/appointments`
- `GET|PUT|DELETE /api/appointments/{id}`

## Sample request
```bash
curl -X POST http://localhost:8080/api/patients \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Aarav Sharma","age":28,"gender":"Male","phone":"9876543210","email":"aarav@example.com","bloodGroup":"O+","address":"Hyderabad"}'
```

## Troubleshooting
- **Access denied:** confirm `DB_USERNAME` / `DB_PASSWORD`.
- **Unknown database:** run `database/schema.sql`.
- **Port 8080 in use:** stop the other process or change `server.port` in `application.properties`.
- **Maven not found:** install Maven or use your IDE's Maven integration.
- **CORS:** backend permits local development origins on ports 5500, 5501, and 3000.

## Production hardening checklist
1. Replace demo role selector with Spring Security authentication and server-side role checks.
2. Add password hashing, session/token security, CSRF strategy, rate limiting, and audit trails.
3. Add pagination, DTO mapping, database migrations, automated tests, monitoring, and backups.
4. Add real SMTP integration only after securely configuring provider credentials.
5. Have security and clinical stakeholders review workflows before deployment.
