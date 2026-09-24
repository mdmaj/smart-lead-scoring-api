# Smart Lead Scoring API

A production-oriented lead scoring system built with **Java Spring Boot, Python FastAPI, MySQL, PostgreSQL, and Gemini AI**.

The system accepts lead information, detects duplicates, calculates a configurable hybrid score using **AI + business rules**, stores operational data in MySQL, and maintains an immutable-style scoring audit trail in PostgreSQL JSONB.

---

## Architecture

```text
                    Client
                      |
                      v
          +------------------------+
          |   Spring Boot API      |
          |       Port 5000        |
          +-----------+------------+
                      |
              REST / HTTP
                      |
                      v
          +------------------------+
          |   FastAPI AI Service   |
          |       Port 8000        |
          +-----------+------------+
                      |
                      v
                  Gemini AI


Spring Boot
    |
    +---- MySQL
    |      |
    |      +-- leads
    |      +-- scoring_config
    |      +-- hybrid_config
    |
    +---- PostgreSQL
           |
           +-- score_audit
```

---

## Tech Stack

### Backend

* Java 17
* Spring Boot
* Spring Web
* Spring Data JPA
* Maven
* REST APIs

### AI Service

* Python
* FastAPI
* Google Gemini API
* `google-genai`

### Databases

* MySQL / MariaDB — operational lead data
* PostgreSQL — scoring audit data using JSONB

### Development

* Git / GitHub
* VS Code
* Postman

---

## Features

* Single lead scoring
* AI-based lead analysis
* Configurable rule-based scoring
* Hybrid AI + rule scoring
* Duplicate detection using email or phone
* Batch lead processing with a maximum of 50 leads
* Rescoring of existing leads
* Analytics endpoint
* PostgreSQL audit logging
* Raw AI response storage
* Rule breakdown storage
* Graceful AI-service failure handling
* `PENDING_SCORE` status when AI scoring is unavailable
* Request validation
* Global exception handling
* Environment-based secrets

---

## Project Structure

```text
NectorFoods/
│
├── README.md
├── .gitignore
│
├── lead-scoring-api/
│   ├── pom.xml
│   ├── .env.example
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── lead_scoring_api/
│       │   │       ├── config/
│       │   │       ├── controller/
│       │   │       ├── dto/
│       │   │       ├── entity/
│       │   │       ├── exception/
│       │   │       ├── repository/
│       │   │       └── service/
│       │   └── resources/
│       │       └── application.properties
│       └── test/
│
└── lead-scoring-ai/
    ├── main.py
    ├── gemini_test.py
    ├── requirements.txt
    ├── .env.example
    └── .gitignore
```

---

# Database Setup

## MySQL

Create the operational database:

```sql
CREATE DATABASE lead_scoring_db;
```

The Spring Boot application automatically creates/updates JPA tables using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

### Scoring Configuration

Example scoring factors:

```sql
INSERT INTO scoring_config (factor_name, weight, is_active)
VALUES
('buying_intent', 30, TRUE),
('urgency', 20, TRUE),
('budget', 20, TRUE),
('message_quality', 15, TRUE),
('company_info', 15, TRUE);
```

### Hybrid Configuration

The AI/rule contribution is configurable through the database.

Example:

```sql
CREATE TABLE hybrid_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ai_weight DOUBLE NOT NULL,
    rule_weight DOUBLE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO hybrid_config (ai_weight, rule_weight, is_active)
VALUES (70, 30, TRUE);
```

The current configuration uses:

```text
AI score    = 70%
Rule score  = 30%
```

These values are stored in the database rather than hardcoded in the scoring logic.

---

## PostgreSQL

Create the audit database:

```sql
CREATE DATABASE lead_scoring_audit;
```

Create the audit table:

```sql
CREATE TABLE score_audit (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL,
    model_name VARCHAR(100),
    raw_ai_response JSONB,
    rule_breakdown JSONB,
    final_score DOUBLE PRECISION,
    scored_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

The audit database stores the AI response and rule calculation separately from the operational lead database.

---

# Environment Variables

## Spring Boot

The Java service uses environment variables for database credentials.

Example:

```env
MYSQL_PASSWORD=
POSTGRES_PASSWORD=your_postgres_password
```

These values should be provided through the local environment.

Do not commit actual passwords or credentials to Git.

---

## Python AI Service

Create a local `.env` file inside `lead-scoring-ai`:

```env
GEMINI_API_KEY=your_gemini_api_key
```

The actual `.env` file is ignored by Git.

Only `.env.example` should be committed.

---

# Running the Application

## 1. Start MySQL

Start MySQL/MariaDB using XAMPP or your local MySQL installation.

Make sure:

```text
MySQL/MariaDB → Port 3306
```

is running.

## 2. Start PostgreSQL

Make sure PostgreSQL is running on:

```text
Port 5432
```

and the database `lead_scoring_audit` exists.

---

## 3. Start the Python AI Service

Navigate to the AI service:

```bash
cd lead-scoring-ai
```

Activate the virtual environment.

On Git Bash:

```bash
source venv/Scripts/activate
```

Install dependencies:

```bash
pip install -r requirements.txt
```

Set the Gemini API key in `.env`.

Start FastAPI:

```bash
uvicorn main:app --reload --port 8000
```

The AI service will be available at:

```text
http://localhost:8000
```

Health check:

```http
GET /
```

---

## 4. Start the Spring Boot API

Open another terminal:

```bash
cd lead-scoring-api
```

Set the required environment variables.

For Git Bash:

```bash
export MYSQL_PASSWORD=""
export POSTGRES_PASSWORD="your_postgres_password"
```

Then start Spring Boot:

```bash
./mvnw spring-boot:run
```

The API will run on:

```text
http://localhost:5000
```

---

# API Endpoints

## Create and Score Lead

```http
POST /api/leads
```

Example request:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "message": "We urgently need a software solution for our business.",
  "source": "website",
  "budget": 150000,
  "company": "Example Technologies"
}
```

The Spring Boot service:

1. Validates the request.
2. Checks for duplicate email/phone.
3. Saves the lead in MySQL.
4. Sends the lead to FastAPI.
5. Receives the AI score.
6. Calculates the rule score.
7. Calculates the final hybrid score.
8. Updates the lead.
9. Stores the scoring audit in PostgreSQL.

---

## Rescore Lead

```http
POST /api/leads/{id}/rescore
```

Example:

```http
POST /api/leads/1/rescore
```

This recalculates the score for an existing lead.

---

## Batch Lead Scoring

```http
POST /api/leads/batch
```

Maximum:

```text
50 leads per request
```

Example:

```json
{
  "leads": [
    {
      "name": "John Doe",
      "email": "john@example.com",
      "message": "I need a software solution urgently.",
      "source": "website",
      "budget": 100000,
      "company": "ABC Ltd"
    },
    {
      "name": "Jane Doe",
      "email": "jane@example.com",
      "message": "Please share information about your product.",
      "source": "linkedin",
      "budget": 50000,
      "company": "XYZ Ltd"
    }
  ]
}
```

Requests containing more than 50 leads are rejected by validation.

---

## Analytics

```http
GET /api/analytics
```

The analytics endpoint provides:

* Total leads
* HOT/WARM/COLD breakdown
* Pending leads
* Duplicate leads
* Average score by source
* Top 5 highest-scoring leads

---

# Hybrid Scoring

The final score combines two independent scoring mechanisms.

### AI Score

Gemini analyzes:

* Buying intent
* Urgency
* Budget
* Message quality
* Company information

and returns a score between `0` and `100`.

### Rule Score

Business rules are configurable using the `scoring_config` table.

Example factors:

| Factor              | Example Weight |
| ------------------- | -------------: |
| Buying intent       |             30 |
| Urgency             |             20 |
| Budget              |             20 |
| Message quality     |             15 |
| Company information |             15 |

### Final Score

With the default configuration:

```text
Final Score =
    (AI Score × 70%)
    +
    (Rule Score × 30%)
```

The final score is constrained to the range:

```text
0 - 100
```

---

# Lead Categories

The final score is mapped to:

```text
80 - 100  → HOT
50 - 79   → WARM
0 - 49    → COLD
```

---

# Duplicate Detection

A lead is considered a duplicate when an existing lead has the same:

* Email address, or
* Phone number

Duplicate leads are not inserted as a new record.

Instead, the existing lead is updated with:

```text
status = DUPLICATE
repeatLead = true
```

This prevents duplicate operational records.

---

# Resilience

The system is designed to continue accepting leads even when the AI service is unavailable.

Flow:

```text
Lead Request
     |
     v
Save lead in MySQL
     |
     v
Call FastAPI
     |
     +---- AI available ----> Score + Audit
     |
     +---- AI unavailable --> PENDING_SCORE
```

If the Python service or downstream Gemini API fails, the lead remains stored in MySQL with:

```text
status = PENDING_SCORE
```

This prevents an AI-service failure from causing the lead itself to be lost.

---

# Audit Logging

Every successful scoring operation stores an audit record in PostgreSQL.

The audit includes:

* Lead ID
* AI model name
* Raw AI response
* Rule breakdown
* Final score
* Timestamp

The AI response and rule breakdown are stored using PostgreSQL `JSONB`.

Example conceptual audit:

```json
{
  "ai_score": 85,
  "rule_score": 90,
  "ai_weight": 70,
  "rule_weight": 30
}
```

This makes scoring decisions traceable and easier to debug.

---

# Error Handling

The API includes global exception handling for:

* Request validation errors
* Lead-not-found errors
* Runtime errors
* Unexpected server errors

Example validation response:

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "name": "Name is required",
    "message": "Message is required",
    "source": "Source is required"
  }
}
```

Example missing lead response:

```json
{
  "status": 404,
  "message": "Lead not found"
}
```

---

# Testing Performed

The implementation was manually tested for:

* Single lead creation
* AI scoring flow
* Duplicate email detection
* Duplicate phone detection
* Batch lead creation
* Batch size validation
* Analytics
* Rescoring
* Missing lead handling
* Request validation
* AI service failure / `PENDING_SCORE` behavior
* PostgreSQL connectivity
* MySQL persistence

---

# Security

Secrets are not committed to the repository.

Ignored files include:

```text
.env
venv/
target/
__pycache__/
*.log
```

Database passwords and Gemini API keys are supplied through environment variables.

---

# Future Improvements

Potential production improvements include:

* Docker Compose for all services and databases
* Automated unit and integration tests
* API authentication
* Swagger/OpenAPI documentation
* Asynchronous batch scoring
* Background retry queue for `PENDING_SCORE` leads
* Structured logging
* Rate limiting
* Improved audit transaction handling
* Production database migrations using Flyway/Liquibase

---

## Author

**Md Mazid Hussain**

Backend / Full Stack Developer

GitHub: `https://github.com/mdmaj`

LinkedIn: `https://www.linkedin.com/in/md-mazid-hussain-maj1707/`

Portfolio: `https://www.mdmaj.in/`
