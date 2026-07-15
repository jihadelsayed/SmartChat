# SmartChat

SmartChat is an Android AI-assistant application that allows users to create conversations, send messages, upload images, manage chat history, and customize application settings.

The project is designed as a university final project using Kotlin, Jetpack Compose, Room, WorkManager, and MVVM architecture.

## Group Members

-  Jihad ElSayed
- Julio Gutierrez
- Sahar Alqaderi
- Cameron
- Narem

Replace the placeholder names before submission.

---

## Project Goals

The application provides a simple interface where users can:

- Log in locally
- Create and manage conversations
- Send messages to an AI assistant
- Upload images
- View previous conversations
- Edit profile information
- Change application settings
- Store conversations locally
- Perform background database maintenance

---

## Course Requirements

SmartChat satisfies the final-project requirements through:

- More than three screens
- A Room database
- WorkManager background work
- Descriptive Kotlin naming
- Comments where logic is not self-explanatory
- A readable package-by-feature structure

---

## Recommended MVP

The first version should include:

1. Login screen
2. Chat screen
3. Chat History screen
4. Profile screen
5. Settings screen
6. Room database
7. WorkManager background task
8. Simulated AI responses or a small predefined response service
9. Image selection from the device

Do not begin with voice chat, image generation, multiple AI providers, calendar integration, or cloud synchronization. Those features are outside the MVP and will waste development time before the required features work.

---

## Recommended Technology Stack

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
- Coil for image display
- Hilt for dependency injection, optional

### Optional Backend

A backend is not required for the MVP because the Room database already satisfies the course database requirement.

A backend should only be added when the application needs:

- Real AI responses
- Cloud synchronization
- Multi-device accounts
- Remote authentication
- Shared file storage
- Push notifications

For a future production version, use:

- Django REST Framework
- PostgreSQL
- Token-based authentication
- Object storage for attachments
- An AI-provider gateway

Never place a paid AI provider API key directly inside the Android application. Mobile applications can be inspected, and embedded keys can be stolen. Real AI requests should go through a backend.

---

## Database Decision

### MVP Database: Room

Room should be the primary database for the university project.

Reasons:

- It works locally without a server.
- It satisfies the database requirement.
- It is officially designed for structured Android data.
- It works well with Kotlin coroutines and Flow.
- It is easier to test and demonstrate.
- It keeps the MVP small.

### Settings Storage: DataStore

Use DataStore for small user preferences such as:

- Dark mode
- Notification preference
- Selected language
- Selected AI model name
- First-launch status

Do not store conversations in DataStore. DataStore is for preferences, not relational application data.

### Future Server Database: PostgreSQL

If a backend is added later, PostgreSQL should store server-side users, conversations, messages, attachments, and synchronization metadata.

---

## Data Model

### UserEntity

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val emailAddress: String,
    val profileImageUri: String?
)
```

### ConversationEntity

```kotlin
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val userId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

### MessageEntity

```kotlin
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["conversationId"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderType: String,
    val messageText: String,
    val createdAt: Long
)
```

### AttachmentEntity

```kotlin
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["messageId"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class AttachmentEntity(
    @PrimaryKey val attachmentId: String,
    val messageId: String,
    val fileName: String,
    val mimeType: String,
    val localUri: String,
    val createdAt: Long
)
```

### Relationships

- One user can have many conversations.
- One conversation can have many messages.
- One message can have multiple attachments.

---

## Application Architecture

Use MVVM with a repository layer.

```text
Compose Screen
      |
      v
ViewModel
      |
      v
Repository
      |
      +------------------+
      |                  |
      v                  v
Room Database      Optional API Service
```

### Responsibilities

#### Compose Screens

- Display UI
- Collect ViewModel state
- Send user actions to the ViewModel
- Avoid database and networking logic

#### ViewModels

- Hold screen state
- Validate user actions
- Call repositories
- Expose StateFlow to Compose

#### Repositories

- Decide where data comes from
- Read and write Room data
- Later communicate with the backend
- Keep ViewModels independent from database implementation

#### Room DAOs

- Contain database queries
- Return Flow where live updates are useful
- Avoid business logic

#### Workers

- Run maintenance tasks
- Remove expired temporary files
- Retry unsynchronized records in a future cloud version

---

## Recommended Project Structure

Use one Android application module and organize code by feature.

```text
SmartChat/
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/smartchat/
│       │   │   ├── SmartChatApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   │
│       │   │   ├── navigation/
│       │   │   │   ├── AppDestination.kt
│       │   │   │   └── SmartChatNavGraph.kt
│       │   │   │
│       │   │   ├── core/
│       │   │   │   ├── database/
│       │   │   │   │   ├── SmartChatDatabase.kt
│       │   │   │   │   ├── DatabaseMigrations.kt
│       │   │   │   │   ├── dao/
│       │   │   │   │   │   ├── UserDao.kt
│       │   │   │   │   │   ├── ConversationDao.kt
│       │   │   │   │   │   ├── MessageDao.kt
│       │   │   │   │   │   └── AttachmentDao.kt
│       │   │   │   │   └── entity/
│       │   │   │   │       ├── UserEntity.kt
│       │   │   │   │       ├── ConversationEntity.kt
│       │   │   │   │       ├── MessageEntity.kt
│       │   │   │   │       └── AttachmentEntity.kt
│       │   │   │   │
│       │   │   │   ├── datastore/
│       │   │   │   │   └── SettingsDataStore.kt
│       │   │   │   ├── network/
│       │   │   │   │   ├── SmartChatApi.kt
│       │   │   │   │   └── NetworkModels.kt
│       │   │   │   ├── ui/
│       │   │   │   │   ├── components/
│       │   │   │   │   └── theme/
│       │   │   │   └── util/
│       │   │   │
│       │   │   ├── feature/
│       │   │   │   ├── login/
│       │   │   │   │   ├── LoginScreen.kt
│       │   │   │   │   ├── LoginViewModel.kt
│       │   │   │   │   └── LoginUiState.kt
│       │   │   │   ├── chat/
│       │   │   │   │   ├── ChatScreen.kt
│       │   │   │   │   ├── ChatViewModel.kt
│       │   │   │   │   ├── ChatUiState.kt
│       │   │   │   │   ├── ChatRepository.kt
│       │   │   │   │   └── components/
│       │   │   │   ├── history/
│       │   │   │   │   ├── ChatHistoryScreen.kt
│       │   │   │   │   ├── ChatHistoryViewModel.kt
│       │   │   │   │   └── ChatHistoryUiState.kt
│       │   │   │   ├── profile/
│       │   │   │   │   ├── ProfileScreen.kt
│       │   │   │   │   ├── ProfileViewModel.kt
│       │   │   │   │   └── ProfileUiState.kt
│       │   │   │   └── settings/
│       │   │   │       ├── SettingsScreen.kt
│       │   │   │       ├── SettingsViewModel.kt
│       │   │   │       └── SettingsUiState.kt
│       │   │   │
│       │   │   ├── repository/
│       │   │   │   ├── UserRepository.kt
│       │   │   │   ├── ConversationRepository.kt
│       │   │   │   └── SettingsRepository.kt
│       │   │   │
│       │   │   ├── worker/
│       │   │   │   ├── DatabaseCleanupWorker.kt
│       │   │   │   └── WorkScheduler.kt
│       │   │   │
│       │   │   └── di/
│       │   │       ├── DatabaseModule.kt
│       │   │       └── RepositoryModule.kt
│       │   │
│       │   └── res/
│       │       ├── drawable/
│       │       ├── mipmap/
│       │       ├── values/
│       │       └── xml/
│       │
│       ├── test/
│       └── androidTest/
└── docs/
    ├── design-document.md
    ├── screen-sketches/
    └── database-diagram.md
```

---

## Why Package by Feature

A package-by-feature structure keeps related files together.

For example, everything required by the chat feature is inside:

```text
feature/chat/
```

This is cleaner than placing every screen in one folder, every ViewModel in another folder, and every state class somewhere else. That layer-only structure becomes annoying as the project grows.

Shared infrastructure such as Room, networking, theme, and reusable components belongs in `core`.

---

## Screen Responsibilities

### Login Screen

- Accept a name and email
- Create or load a local user
- Navigate to the chat screen

For the MVP, this can be local login. Real authentication is not required unless the group adds a backend.

### Chat Screen

- Display conversation messages
- Send a user message
- Create a simulated AI response
- Select an image
- Save messages to Room

### Chat History Screen

- Display stored conversations
- Open a selected conversation
- Delete a conversation
- Search by title if time allows

### Profile Screen

- Display the current user
- Update display name
- Change profile image

### Settings Screen

- Toggle dark mode
- Enable or disable notifications
- Select a language
- Clear local history

---

## Background Work Requirement

Use WorkManager for a real but simple task.

### Recommended Worker

`DatabaseCleanupWorker`

The worker can:

- Delete temporary attachment records whose files no longer exist
- Remove draft conversations with no messages after a defined period
- Clear old temporary files

Schedule it as periodic work.

Do not pretend that saving every message requires WorkManager. Normal database writes should happen immediately through Room and coroutines. WorkManager is for deferrable work that must still execute reliably.

---

## Optional Networking

Networking is optional because Room already satisfies the project requirement.

If networking is added, keep it small:

- Send a prompt to a controlled backend endpoint
- Receive a text response
- Store the response in Room

Recommended request flow:

```text
Android App
    |
    | HTTPS
    v
Django REST Backend
    |
    v
AI Provider
```

The Android app should never communicate with a paid AI provider using a secret key embedded in the app.

---

## Optional Backend Structure

Keep the backend in a separate repository or sibling directory.

```text
smartchat-backend/
├── README.md
├── manage.py
├── requirements.txt
├── config/
│   ├── settings.py
│   ├── urls.py
│   └── wsgi.py
├── accounts/
├── conversations/
│   ├── models.py
│   ├── serializers.py
│   ├── services.py
│   ├── views.py
│   └── urls.py
├── ai_gateway/
│   ├── providers/
│   ├── services.py
│   ├── views.py
│   └── urls.py
└── tests/
```

Suggested backend endpoints:

```text
POST /api/v1/auth/login/
GET  /api/v1/conversations/
POST /api/v1/conversations/
GET  /api/v1/conversations/{id}/messages/
POST /api/v1/conversations/{id}/messages/
POST /api/v1/ai/chat/
```

Do not build this backend until the local Android MVP is complete.

---

## Team Work Division

### Cameron: Navigation and Login

- Application navigation
- Login screen
- Local user creation
- Login validation

### Julio: Chat Feature

- Chat UI
- Message bubbles
- Message input
- Image selection
- Simulated AI responses

### Jihad And Sahar: Data and Background Work

- Room entities
- DAOs
- Database class
- Repositories
- WorkManager worker

### Narem: History, Profile, and Settings

- Chat History screen
- Profile screen
- Settings screen
- DataStore preferences
- Testing and documentation

All members should review integration changes and avoid editing the same file at the same time.

---

## Development Order

### Phase 1: Project Foundation

- Create the Compose project
- Add navigation
- Add theme
- Create empty screens
- Verify navigation between all screens

### Phase 2: Room Database

- Create entities
- Create DAOs
- Create the database
- Create repositories
- Test inserts, reads, updates, and deletes

### Phase 3: Chat Feature

- Create conversations
- Send messages
- Store messages
- Display saved messages
- Generate mock AI responses

### Phase 4: Other Screens

- Build history
- Build profile
- Build settings
- Add image selection

### Phase 5: Background Work

- Create the cleanup worker
- Schedule periodic work
- Add a visible test option for demonstration

### Phase 6: Testing and Presentation

- Test navigation
- Test empty states
- Test database persistence
- Test deleting conversations
- Test screen rotation
- Prepare screenshots and demo data

### Phase 7: Optional Backend

Only begin this phase after the full MVP works locally.

---

## Git Workflow

Recommended branch names:

```text
main
develop
feature/login
feature/chat
feature/database
feature/history
feature/profile-settings
```

Recommended workflow:

1. Create a feature branch from `develop`.
2. Make one focused change.
3. Open a pull request.
4. Review the change.
5. Merge into `develop`.
6. Merge stable releases into `main`.

Do not let every team member push unfinished work directly into `main`.

---

## Code Quality Rules

- Use descriptive names.
- Keep composables small.
- Do not access DAOs directly from screens.
- Do not perform database work on the main thread.
- Do not place navigation logic inside repositories.
- Avoid giant ViewModels.
- Add comments only where the reason is not obvious.
- Remove dead code before submission.
- Use consistent formatting.
- Keep secrets out of the repository.

Example of weak naming:

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

- DAO insert and query test
- Conversation deletion cascade test
- ViewModel message-send test
- Settings repository test
- Navigation smoke test

Manual testing should verify:

- Login data survives application restart
- Conversations appear in history
- Messages remain after leaving the screen
- Deleted conversations disappear
- Theme setting persists
- Image selection does not crash
- WorkManager task can execute

---

## Final Recommendation

Build the MVP without a backend.

Use:

- Room for users, conversations, messages, and attachments
- DataStore for settings
- WorkManager for cleanup work
- Mock AI responses for the first working version

Add a Django REST backend only after every course requirement works. A half-built backend plus a broken Android app is worse than a complete local app with clean architecture.
