# SmartChat Backend

Node.js, Express, TypeScript, PostgreSQL, and Prisma backend for the SmartChat Android application.

## Setup

```bash
cp .env.example .env
docker compose up -d
npm install
npm run prisma:generate
npm run db:migrate -- --name init
npm run dev
```

## Main endpoints

- `GET /api/health`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me`
- `GET /api/v1/conversations`
- `POST /api/v1/conversations`
- `GET /api/v1/conversations/:conversationId`
- `PATCH /api/v1/conversations/:conversationId`
- `DELETE /api/v1/conversations/:conversationId`
- `POST /api/v1/conversations/:conversationId/messages`
- `POST /api/v1/ai/chat`

The AI provider currently returns a mock response. Replace `MockAiProvider` later without changing the controllers or routes.
