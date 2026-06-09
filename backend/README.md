# Gepsi Backend (Flask)

Local-first sync target for the Gepsi Android app.

## Run

```powershell
cd backend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

Server listens on `http://0.0.0.0:5000`. DB at `instance/gepsi.db`, voice uploads under `uploads/<route_id>/`.

## Endpoints

| Method | Path | Purpose |
| ------ | ---- | ------- |
| GET    | `/api/health` | liveness |
| POST   | `/api/routes` | create or update route (idempotent by `client_id`) |
| GET    | `/api/routes` | list routes |
| POST   | `/api/routes/<client_id>/points/batch` | bulk insert track points |
| POST   | `/api/routes/<client_id>/notes` | multipart: `meta` JSON + optional `voice` file |
| GET    | `/api/routes/<client_id>/notes/<note_client_id>/voice` | download voice memo |

All client-supplied IDs are the Android-local Room PKs. Server dedupes on `(route_id, client_id)`.
