# MindVault (Android)

Offline-first journaling app with on-device AI and secure sync.

- 🎙️ **Speech-to-Text** — Whisper.cpp JNI  
- 🧠 **Summarization + Mood Tagging** — Qwen3/Gemma 2–3B via llama.cpp JNI  
- 🔒 **Encryption** — AES-GCM with Android Keystore for data key  
- 🗄️ **Storage** — Room (SQLite) local DB, encrypted sync blobs to server  
- 🔄 **Sync** — Encrypted entries pushed/pulled via FastAPI backend with JWT auth  
- 🖥️ **Frontend** — Material 3 UI, dark/light themes, toolbar + menu (Entries, Settings)

---

## Quickstart

### Prereqs
- Android Studio Hedgehog+  
- NDK + CMake installed (for JNI builds)  
- Gradle sync (see `app/build.gradle.kts`)  
- Clone/Download llama.cpp + whisper.cpp into `jni/`  

### First Build
```bash
./gradlew assembleDebug

