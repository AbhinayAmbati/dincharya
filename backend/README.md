# Backend — placeholder (Phase 5)

Dincharya v1 is **fully local**: no backend, no auth, no accounts. The Android
app in [`../frontend`](../frontend) works standalone, and the manifest
deliberately does not declare the `INTERNET` permission — the app physically
cannot send data anywhere.

This folder is reserved for the Express backend that arrives in a later phase.

## Planned scope (when it lands)

The backend stays **thin** — the phone remains the brain. It will only provide:

| Endpoint group | Purpose |
|---|---|
| `POST /auth` | JWT-based login/registration |
| `POST /sync` | Encrypted task/event mirror for cross-device sync |
| `GET /backup` / `POST /backup` | Client-side encrypted backup and restore |
| `POST /push` | Push relay for reminder delivery reliability |

**Planned stack:** Node.js + Express in **TypeScript**, Zod for request
validation, PostgreSQL (SQLite locally for dev). Planned layout:

```
backend/
├── src/
│   ├── routes/        # route definitions only
│   ├── controllers/  # request/response handling
│   ├── services/      # business logic
│   ├── middleware/    # auth guard, rate limiter, error handler
│   ├── models/        # schemas + validation
│   └── config/        # typed env loading
└── tests/
```

## Privacy rule (non-negotiable)

Task content is encrypted on the client before it ever reaches this server.
The backend stores opaque blobs — it never sees what a user's tasks say.
