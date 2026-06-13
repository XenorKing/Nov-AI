# novAI — Android App

AI-powered chat assistant for Android, built with Jetpack Compose.

## Features
- 🔐 Firebase Auth (email + password)
- 💬 AI chat with conversation history saved to Firestore
- 🗂️ View and delete conversations (swipe to delete)
- ⚙️ AI settings: system prompt, temperature, max tokens
- 🌙 Dark theme inspired by Nov-Anime project

## Stack
- Kotlin + Jetpack Compose + Material 3
- Hilt dependency injection
- Firebase Auth + Firestore
- OkHttp for API calls
- Navigation Compose

## Setup
1. Place `google-services.json` in `app/`
2. Build: `./gradlew assembleDebug`

## Architecture
```
com.novaproject.novai/
├── data/model/          Models (Message, Conversation, AISettings)
├── data/repository/     AuthRepository, ChatRepository
├── di/                  Hilt DI module
├── navigation/          AppNavigation with sealed Screen routes
├── ui/screens/          Login, Register, Splash, Conversations, Chat, Settings
├── ui/components/       AuthTextField, NovAILogo, TypingIndicator
├── ui/theme/            Color, Theme, Type
└── viewmodel/           AuthViewModel, ChatViewModel, SettingsViewModel
```

## Firestore Structure
```
users/{uid}/
  conversations/{convId}  → title, preview, createdAt, updatedAt
    messages/{msgId}      → role, content, timestamp
  settings/default        → systemPrompt, temperature, maxTokens
```
