# CampusFind 🎒🔍

> A modern, privacy-first college lost and found platform that matches lost items using location, category, timestamps, and AI photo analysis, enabling secure in-app connections between finder and owner.

---

## 🎯 The Problem

On college campuses, hundreds of items are misplaced every week (watches, ID cards, water bottles, earphones, notebooks). Currently:
- Students post on messy WhatsApp or Telegram groups where sensitive personal contact numbers are exposed to thousands of strangers.
- Posts get buried in minutes.
- Fraudulent claims happen frequently because there is no verification mechanism.
- Items handed to department offices or security lack central visibility.

**CampusFind** solves this with an automated matching engine and privacy-by-design architecture:
1. Students report lost or found items with location tags (campus block + classroom/hall), timestamps, categories, and photos.
2. The matching algorithm computes a compatibility score out of 100.
3. Once matched, the app creates a **private, secure in-app chat** between only the two matched students—no phone numbers or private emails revealed.
4. The finder can pose a secret claim verification question (e.g. *"What is the laptop wallpaper?"*) before handing over the item.

---

## 🌟 Full Feature List (6-Step Roadmap)

- 🔒 **College Email Only Authentication:** Institutional JWT signup/login with `@college.edu` verification and role-based permissions (`ROLE_STUDENT`, `ROLE_ADMIN`).
- 📌 **Detailed Lost/Found Postings:** High-resolution photos, block, room number, timestamp, category, and condition.
- ⚡ **Smart Matching Engine (100 Points):** Evaluates spatial proximity (block + room), category, temporal overlap, and image embedding similarity.
- 💬 **Private Real-time STOMP Chat:** Secure WebSocket messaging isolated strictly to matched student pairs.
- 🔑 **Claim Verification Handoff:** Finder challenge questions to confirm true ownership prior to physical handover.
- 🔄 **Lifecycle State Machine:** `OPEN` ➔ `MATCHED` ➔ `RETURNED` ➔ `CLOSED`.
- 🔔 **Multi-channel Notifications:** In-app real-time alerts and transactional emails via Spring Mail.
- 🤖 **AI Superpowers with Google Gemini:** Automatic tagging and description auto-fill from photos, visual similarity scoring, and sensitive content detection (faces, government ID numbers).
- 🗺️ **Campus Map & Safe Drop Points:** Visual campus pin locator with designated drop points (Library front desk, Campus Security).
- 🏆 **Karma, Badges & Leaderboard:** Incentivizing student honesty with karma points and community recognition.
- 🛡️ **Admin Governance Dashboard:** Audit logs, report resolutions, post moderation, user bans, and analytics.
- 📱 **Modern Mobile-First PWA:** Responsive React interface with dark mode and installable Progressive Web App support.

---

## 🛠️ Tech Stack

### Backend
- **Language / Runtime:** Java 21
- **Framework:** Spring Boot 3.3.4 (Maven)
- **Security:** Spring Security 6 with JWT (HMAC-SHA256)
- **Database:** MySQL 8 with Hibernate / Spring Data JPA
- **Database Migrations:** Flyway (`flyway-mysql`)
- **Real-Time:** Spring WebSocket with STOMP & SockJS
- **Communication:** Spring Mail (SMTP)
- **AI Integration:** Google Gemini API
- **Media Hosting:** Cloudinary

### Frontend
- **Framework:** React 18 + Vite
- **Language:** TypeScript
- **Styling:** Tailwind CSS + PostCSS + Lucide Icons
- **HTTP Client:** Axios

### Infrastructure & DevOps
- **Containerization:** Docker & Docker Compose
- **Configuration:** Multi-profile `application.yml` (`dev`, `prod`) with environment variable isolation

---

## 📁 Repository Structure

```text
campusfind/
├── .env.example              # Template for environment variables and secrets
├── .gitignore                # Git exclusions (build artifacts, secrets, logs)
├── docker-compose.yml        # Local MySQL 8 database service with healthcheck
├── README.md                 # Project documentation and setup instructions
│
├── backend/                  # Spring Boot 3 + Java 21 Backend
│   ├── pom.xml               # Maven configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/campusfind/
│       │   │   ├── CampusFindApplication.java
│       │   │   ├── config/      # CORS and MVC configuration
│       │   │   ├── controller/  # REST API endpoints & Health check
│       │   │   ├── dto/         # Request & Response objects (ApiResponse)
│       │   │   ├── entity/      # JPA entities (User, ItemPost, Match, etc.)
│       │   │   ├── exception/   # GlobalExceptionHandler & custom exceptions
│       │   │   ├── repository/  # Spring Data JPA repositories
│       │   │   ├── security/    # Security filter chain & JWT utilities
│       │   │   ├── service/     # Business logic layer
│       │   │   └── util/        # Helper functions & date formatting
│       │   └── resources/
│       │       ├── application.yml   # Multi-profile Spring configuration
│       │       └── db/migration/     # Flyway SQL migrations
│       └── test/                     # Unit and integration test suites
│
└── frontend/                 # React + Vite + TypeScript Frontend
    ├── package.json          # Frontend dependencies and scripts
    ├── tsconfig.json         # TypeScript compiler configuration
    ├── vite.config.ts        # Vite configuration with /api proxy
    ├── tailwind.config.js    # Tailwind styling configuration
    ├── index.html            # Main HTML document
    └── src/
        ├── App.tsx           # Main application view
        ├── main.tsx          # React DOM entry point
        ├── components/       # Shared UI components
        ├── pages/            # Application views and dashboards
        ├── services/         # Axios API clients
        └── types/            # TypeScript interfaces
```

---

## 🚀 Getting Started & Local Setup

### 1. Prerequisites
- **Java 21 JDK** installed (`java -version`)
- **Maven 3.9+** or Maven wrapper (`mvn -version`)
- **Node.js 18+ & npm** (`node -v`)
- **Docker & Docker Compose** (`docker --version`)

### 2. Environment Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Ensure your configuration values match your local database and credentials.

### 3. Start MySQL 8 via Docker Compose
Start the localized MySQL 8 database container:
```bash
docker compose up -d
```
Verify the container status:
```bash
docker compose ps
```
The MySQL container is configured with UTF-8 (`utf8mb4_unicode_ci`), UTC timezone, a persistent named volume `campusfind_mysql_data`, and automatic health checks.

### 4. Build and Run the Backend
Navigate to `/backend`:
```bash
cd backend
mvn clean spring-boot:run
```
The Spring Boot backend will start on **`http://localhost:8081`**.
Verify system health and database connectivity:
```bash
curl http://localhost:8081/api/v1/health
```

Sample standard response:
```json
{
  "success": true,
  "message": "CampusFind service is operational",
  "data": {
    "status": "UP",
    "service": "campusfind-backend",
    "activeProfile": "[dev]",
    "databaseStatus": "CONNECTED (MySQL 8.0.x)",
    "serverTime": "2026-10-11T19:25:00.000000"
  },
  "timestamp": "2026-10-11T19:25:00.000000"
}
```

### 5. Start the Frontend Application
In a separate terminal, navigate to `/frontend`:
```bash
cd frontend
npm install
npm run dev
```
Open your browser at **`http://localhost:5173`**. The Vite server proxies API calls to `http://localhost:8080`.

---

## 🛡️ Standard API Response Format

All API endpoints follow a unified response structure:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-10-11T19:25:00.000",
  "errors": null
}
```

On validation or business logic exceptions:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "timestamp": "2026-10-11T19:25:00.000",
  "errors": {
    "email": "Must be a valid college domain email",
    "category": "Category is mandatory"
  }
}
```
