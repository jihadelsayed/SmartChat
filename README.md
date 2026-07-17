# SmartChat

SmartChat is an Android AI-assistant application that allows users to create conversations, send messages, upload images, manage chat history, and customize application settings.

The project is being developed as a university final project using a separate Android application and Node.js backend.

## Group Members

- Jihad ElSayed
- Julio Gutierrez
- Sahar Alqaderi
- Cameron
- Narem

---

## Project Status

The following backend foundation is working:

- Node.js and TypeScript backend
- Express REST API
- PostgreSQL 17 running in Docker
- Prisma ORM
- Initial Prisma migration
- User registration
- JWT access-token generation
- Health endpoint
- Database tables for users, conversations, messages, and attachments

The initial registration endpoint has been tested successfully against the Docker PostgreSQL database.

---

## Project Goals

SmartChat provides a simple interface where users can:

- Register and log in
- Create and manage conversations
- Send messages to an AI assistant
- Upload images
- View previous conversations
- Edit profile information
- Change application settings
- Store conversations locally using Room
- Store synchronized application data in PostgreSQL
- Perform background database and synchronization work

---

## Course Requirements

SmartChat satisfies the final-project requirements through:

- More than three screens
- Room local database
- PostgreSQL server database
- In-app networking
- WorkManager background work
- Descriptive Kotlin and TypeScript naming
- Comments where logic is not self-explanatory
- A readable package-by-feature structure

---

## Minimum Viable Product

The MVP includes:

1. Registration and login
2. AI chat screen
3. Chat History screen
4. Profile screen
5. Settings screen
6. Room local database
7. PostgreSQL backend database
8. WorkManager background task
9. Mock AI responses
10. Image selection from the device
11. REST API communication between Android and Node.js
12. JWT authentication

The following features are outside the MVP:

- Voice chat
- AI image generation
- Multiple AI providers
- Calendar integration
- Cloud file storage
- Advanced offline synchronization

---

## Technology Stack

### Android Application

- Kotlin
- Jetpack Compose
- Material Design 3
- Navigation Compose
- ViewModel
- Kotlin Coroutines
- StateFlow
- Room
- WorkManager
- DataStore Preferences
- Retrofit
- Coil
- Hilt, optional

### Backend

- Node.js
- TypeScript
- Express
- Prisma ORM
- PostgreSQL 17
- JWT authentication
- bcrypt password hashing
- Zod validation
- Docker Compose for PostgreSQL

---

## Project Structure

```text
SmartChat/
├── README.md
├── docs/
│   ├── design-document.md
│   ├── architecture.md
│   ├── database-diagram.md
│   ├── api-documentation.md
│   ├── team-responsibilities.md
│   └── screen-sketches/
│       ├── login-screen.png
│       ├── chat-screen.png
│       ├── history-screen.png
│       ├── profile-screen.png
│       └── settings-screen.png
│
├── android/
│   ├── README.md
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── gradle/
│   └── app/
│       ├── build.gradle.kts
│       └── src/
│           ├── main/
│           │   ├── AndroidManifest.xml
│           │   ├── java/com/smartchat/
│           │   │   ├── SmartChatApplication.kt
│           │   │   ├── MainActivity.kt
│           │   │   ├── navigation/
│           │   │   ├── core/
│           │   │   │   ├── database/
│           │   │   │   ├── datastore/
│           │   │   │   ├── model/
│           │   │   │   ├── network/
│           │   │   │   ├── ui/
│           │   │   │   └── util/
│           │   │   ├── feature/
│           │   │   │   ├── auth/
│           │   │   │   ├── chat/
│           │   │   │   ├── history/
│           │   │   │   ├── profile/
│           │   │   │   └── settings/
│           │   │   ├── repository/
│           │   │   ├── mapper/
│           │   │   ├── worker/
│           │   │   └── di/
│           │   └── res/
│           ├── test/
│           └── androidTest/
│
└── backend/
    ├── README.md
    ├── package.json
    ├── package-lock.json
    ├── tsconfig.json
    ├── prisma.config.ts
    ├── docker-compose.yml
    ├── .env.example
    ├── prisma/
    │   ├── schema.prisma
    │   ├── seed.ts
    │   └── migrations/
    ├── src/
    │   ├── server.ts
    │   ├── app.ts
    │   ├── config/
    │   ├── database/
    │   ├── middleware/
    │   ├── routes/
    │   ├── modules/
    │   │   ├── auth/
    │   │   ├── users/
    │   │   ├── conversations/
    │   │   ├── messages/
    │   │   ├── attachments/
    │   │   └── ai/
    │   ├── shared/
    │   └── generated/
    │       └── prisma/
    └── tests/
```

---

## Architecture

```text
Android Compose Screen
          |
          v
      ViewModel
          |
          v
      Repository
       /      \
      v        v
Room Database  Node REST API
                    |
                    v
                 Prisma
                    |
                    v
             PostgreSQL Database
```

### Android Responsibilities

#### Compose Screens

- Display interface state
- Collect state from ViewModels
- Send user actions to ViewModels
- Avoid direct database and network access

#### ViewModels

- Hold screen state
- Validate user input
- Call repositories
- Expose StateFlow values

#### Repositories

- Coordinate Room and backend access
- Hide storage implementation details
- Convert network and database models

#### Room DAOs

- Store local conversations
- Store local messages
- Store attachment metadata
- Return Flow for live updates

#### Workers

- Clean temporary files
- Perform deferred synchronization
- Retry failed synchronization tasks

### Backend Responsibilities

#### Controllers

- Read HTTP requests
- Call services
- Return API responses

#### Services

- Implement business logic
- Validate permissions
- Coordinate repositories and providers

#### Repositories

- Read and write database records through Prisma

#### Middleware

- Authenticate JWT tokens
- Validate request bodies
- Handle errors
- Log requests

---

## Database Design

### Android Local Database

Room stores:

- Conversations
- Messages
- Attachment metadata
- Offline synchronization state

DataStore stores:

- Access token
- Current user ID
- Theme
- Notification preference
- Language
- First-launch state

### Backend Database

PostgreSQL stores:

- Users
- Conversations
- Messages
- Attachments
- Prisma migration history

### Relationships

- One user can have many conversations.
- One conversation belongs to one user.
- One conversation can have many messages.
- One message can have many attachments.

---

## PostgreSQL Docker Setup

The Docker PostgreSQL database uses:

```text
Host: 127.0.0.1
Host port: 5433
Container port: 5432
Database: smartchat
User: smartchat
```

The local PostgreSQL installation may continue using port `5432`.

### Start PostgreSQL

```powershell
cd backend
docker compose up -d postgres
docker compose ps
```

Expected port mapping:

```text
0.0.0.0:5433->5432/tcp
```

### Enter PostgreSQL

```powershell
docker compose exec postgres psql -U smartchat -d smartchat
```

Useful PostgreSQL commands:

```sql
\l
\dt
\q
```

---

## Environment Configuration

Create `backend/.env` from `.env.example`.

```env
NODE_ENV=development
PORT=3000
DATABASE_URL=postgresql://smartchat:smartchat@127.0.0.1:5433/smartchat?schema=public
JWT_SECRET=replace-with-a-long-development-secret
JWT_EXPIRES_IN=7d
CORS_ORIGIN=*
```

Do not commit `.env`.

---

## Backend Installation

```powershell
cd backend

npm config set registry "https://registry.npmjs.org/"
npm install
```

Generate Prisma Client:

```powershell
npm run prisma:generate
```

Create the initial migration:

```powershell
npm run db:migrate -- --name init
```

Check migration status:

```powershell
npx prisma migrate status
```

Expected tables:

```text
User
Conversation
Message
Attachment
_prisma_migrations
```

---

## Running the Backend

```powershell
cd backend

$env:DATABASE_URL = "postgresql://smartchat:smartchat@127.0.0.1:5433/smartchat?schema=public"

npm run dev
```

The backend runs at:

```text
http://localhost:3000
```

---

## API Endpoints

### Health

```http
GET /api/health
```

Example response:

```json
{
  "success": true,
  "data": {
    "status": "ok",
    "service": "smartchat-backend"
  }
}
```

### Register

```http
POST /api/v1/auth/register
```

Example request:

```json
{
  "email": "student@smartchat.local",
  "password": "Password123",
  "displayName": "Student User"
}
```

### Login

```http
POST /api/v1/auth/login
```

Example request:

```json
{
  "email": "student@smartchat.local",
  "password": "Password123"
}
```

### Conversations

```http
GET    /api/v1/conversations
POST   /api/v1/conversations
GET    /api/v1/conversations/{conversationId}
DELETE /api/v1/conversations/{conversationId}
```

### Messages

```http
GET  /api/v1/conversations/{conversationId}/messages
POST /api/v1/conversations/{conversationId}/messages
```

### AI

```http
POST /api/v1/ai/chat
```

The MVP uses a mock AI provider.

---

## PowerShell API Testing

### Health

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:3000/api/health" `
  -Method GET
```

### Registration

```powershell
$registerBody = @{
    email       = "student@smartchat.local"
    password    = "Password123"
    displayName = "Student User"
} | ConvertTo-Json

$registerResponse = Invoke-RestMethod `
    -Uri "http://localhost:3000/api/v1/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body $registerBody

$token = $registerResponse.data.accessToken
```

Do not place real access tokens in documentation or commit them to Git.

---

## Application Screens

### Login and Registration

- Register a new account
- Log in with an existing account
- Store the access token securely
- Navigate to Chat after authentication

### Chat

- Display conversation messages
- Send user messages
- Receive mock AI responses
- Select an image
- Save messages locally

### Chat History

- Display stored conversations
- Open a conversation
- Delete a conversation
- Search conversations if time allows

### Profile

- Display current user information
- Edit display name
- Change profile image

### Settings

- Toggle dark mode
- Enable or disable notifications
- Select language
- Clear local history
- Log out

---

## Background Work

The project uses WorkManager.

### DatabaseCleanupWorker

The worker may:

- Delete empty draft conversations
- Delete broken attachment records
- Remove temporary files
- Retry deferred synchronization

Normal message saving should use Room and coroutines directly. WorkManager is for reliable deferred work.

---

## Team Work Division

### Cameron: Navigation and Authentication

- Application navigation
- Login screen
- Registration screen
- Authentication validation

### Julio: Chat Feature

- Chat interface
- Message bubbles
- Message input
- Image selection
- Mock AI responses

### Jihad and Sahar: Data and Background Work

- Room entities
- DAOs
- Database class
- Repositories
- PostgreSQL and Prisma
- WorkManager workers

### Narem: History, Profile, Settings, and Documentation

- Chat History screen
- Profile screen
- Settings screen
- DataStore preferences
- Testing
- Documentation

All members should review integration changes and avoid editing the same file simultaneously.

---

## Development Progress

### Completed

- Project folder structure
- Docker PostgreSQL setup
- PostgreSQL port separation
- Prisma schema
- Initial migration
- Prisma Client generation
- Health endpoint
- User registration
- Password hashing
- JWT token generation

### Next Tasks

1. Test login.
2. Test authenticated profile access.
3. Test conversation creation and listing.
4. Test message creation.
5. Test the mock AI endpoint.
6. Complete Android Gradle sync.
7. Connect Android registration and login.
8. Add Android and backend tests.
9. Persist selected image metadata in Room.
10. Implement multipart attachment upload.
11. Synchronize Room with backend conversations.
12. Add token-expiration and logout handling.
13. Improve loading, validation, and error UI.
14. Add required screen sketches.

---

## Code Quality Rules

- Use descriptive names.
- Keep Compose functions small.
- Do not access DAOs directly from screens.
- Do not access Prisma directly from controllers.
- Do not perform blocking work on the main Android thread.
- Keep navigation logic out of repositories.
- Avoid giant ViewModels and services.
- Add comments only when the reason is not obvious.
- Remove dead code before submission.
- Keep secrets and tokens out of Git.

Weak naming:

```kotlin
val x = dao.getAll()
```

Better naming:

```kotlin
val conversations = conversationDao.observeAllConversations()
```

---

## Testing

Minimum tests should include:

### Android

- DAO insert and query test
- Conversation deletion cascade test
- Login ViewModel test
- Chat ViewModel test
- Settings repository test
- Navigation smoke test

### Backend

- Registration service test
- Login service test
- JWT middleware test
- Conversation endpoint test
- Message endpoint test
- AI endpoint test

### Manual Checks

- Registration creates a PostgreSQL user.
- Login returns a valid token.
- Protected endpoints reject missing tokens.
- Conversations persist in PostgreSQL.
- Room data survives Android restart.
- Settings persist through DataStore.
- WorkManager executes successfully.
- Image selection does not crash.

---

## Security

- Passwords are hashed with bcrypt.
- Authentication uses JWT access tokens.
- Database credentials remain in `.env`.
- AI provider keys must only exist in the backend.
- Real tokens must never be committed or placed in documentation.
- Production deployments must use HTTPS and a strong JWT secret.