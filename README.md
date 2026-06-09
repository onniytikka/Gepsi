# Gepsi

GPS walking-trail recorder for Android with location-tagged text + voice notes. Local-first storage in Room, optional sync to a Flask backend.

## Repo layout

- `android/` — Android Studio project (Kotlin + Jetpack Compose, osmdroid maps, foreground tracking service).
- `backend/` — Flask + SQLite sync target (see `backend/README.md`).

## Quick start

### 1. Run the backend

```powershell
cd backend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

### 2. Build the Android app

Open `android/` in Android Studio (Iguana or newer, Gradle 8.5+, JDK 17). Build → install on a **physical device** (emulator GPS is mocked).

The backend base URL is wired into `BuildConfig.BACKEND_URL` via `android/gradle.properties` → `gepsi.backend.url`. Defaults to `http://10.0.2.2:5000/` (Android emulator → host). For a physical device on the same LAN, override:

```
./gradlew assembleDebug -Pgepsi.backend.url=http://192.168.x.x:5000/
```

### 3. Use it

1. Launch the app. Grant location (foreground + background), microphone, and notifications.
2. Tap **Start** — a persistent notification appears, tracking continues with the screen off.
3. Tap the note FAB to drop a location-tagged text + voice memo.
4. Tap **Stop** — the route is finished and queued for sync.
5. Open the route list (top-right icon) → tap a route to see the polyline and tap any note marker to read or play it back.

## CI/CD

GitHub Actions at `.github/workflows/ci.yml` runs secret scan, dependency review, and CodeQL on every push / PR to `main`. See repo's CI section for details.

## Security

See [SECURITY.md](./SECURITY.md).
