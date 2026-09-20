# AI Personal Finance Copilot

IIT Patna capstone project for bill-driven expense tracking. The workflow is:

`Login → upload bill → LLM extraction → review/edit → confirm → analytics dashboard`

## Storage design

| Store | Responsibility |
| --- | --- |
| PostgreSQL | users, credentials, authentication and profile data |
| MongoDB + GridFS | bill metadata, line items, categories, AI confidence/extraction metadata and uploaded files |

Only **confirmed** bills are included in the dashboard totals. Uploads start as drafts, so an AI result is never silently recorded as an expense.

## Prerequisites

- Java 25
- Maven 3.9+
- Node.js 22+
- A PostgreSQL database (Supabase is supported)
- MongoDB Atlas or a local MongoDB instance

## Configure

Create `backend/.env` from the example:

```bash
cp backend/.env.example backend/.env
```

Set `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `MONGODB_URI`, and a random `JWT_SECRET` of at least 32 bytes. Keep `AI_PROVIDER=stub` to test the entire upload/review flow without an LLM key.

For live extraction set one of:

```dotenv
AI_PROVIDER=gemini
AI_API_KEY=your_google_ai_key
AI_MODEL=gemini-2.0-flash
```

or:

```dotenv
AI_PROVIDER=claude
AI_API_KEY=your_anthropic_api_key
AI_MODEL=claude-sonnet-4-5
```

## Run locally

In separate terminals:

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The frontend proxies `/api` to the Spring Boot server on port 8080.

## Validate

```bash
cd backend && mvn test
cd frontend && npm run build
```

## API overview

- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `POST /api/bills` uploads and extracts a bill as a draft
- `PATCH /api/bills/{id}` updates a draft
- `POST /api/bills/{id}/confirm` saves it as an expense
- `GET /api/bills`, `GET /api/bills/{id}/file`, `DELETE /api/bills/{id}`
- `GET /api/analytics/summary`, `/monthly`, `/category`
- `GET /api/health`

Every bill endpoint is JWT-protected and resolves the owner exclusively from the token; callers cannot submit another user's id.
