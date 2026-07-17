# SmartChat Android

Android application built with Kotlin and Jetpack Compose.

## Implemented

- Separate login and registration flows backed by the Node API
- JWT session restoration and automatic invalid-session logout
- Chat conversations and AI message exchanges through the backend
- Room persistence for conversations, messages, and attachment metadata
- Synchronized chat history with local and remote deletion
- Read-only backend profile screen
- DataStore theme and notification preferences
- Settings logout and confirmed local-history cleanup
- Network-constrained WorkManager retry for failed messages
- Android system image picker, Coil previews, and multipart image upload

## Run

1. Open this folder in Android Studio.
2. Let Gradle synchronize and install missing SDK components.
3. Start the backend on port `3000`.
4. Run the debug build in an Android emulator.

The emulator reaches the computer backend through `http://10.0.2.2:3000/`.
Cleartext HTTP is enabled only for debug builds; release builds keep it disabled.
