# Staff reservation detail UI verification

Local fixtures follow `StaffReservationController`, `StaffReservationDetailResponse`,
`ReservationLogEventType`, and the creation/reschedule snapshots in the sibling
backend. They are synthetic data, not evidence of live backend behavior.
The mock was built from this repository's Java contract; no remote mock is used.

```powershell
postman simulation run postman/simulations/staff-reservation-detail.sim.yaml --port 4900 --no-history
```

In another terminal (do not persist this URL in `.env`):

```powershell
$env:VITE_API_BASE_URL = 'http://127.0.0.1:4900/sage'
npm run dev -- --port 8001
```

Open `http://localhost:8001/login`. The local mock accepts any syntactically valid
email and any non-empty password; use synthetic values such as `staff@example.test`.
It does not validate authentication. Its CORS origin is limited to this local UI.

Visit `/staff/reservations` and use the eye button, or open a detail URL:

| ID | Case |
| --- | --- |
| 1–7 | PENDING, CONFIRMED, ARRIVED, COMPLETED, CANCELLED, REJECTED, NO_SHOW |
| 8 | Staff guest, nullable fields, empty history |
| 9 | Long note, unsorted reschedule history, UTC timestamps crossing dates |
| 10 | Delayed response for loading/cancellation checks |
| 403 | Forbidden |
| 404 | Missing reservation |
| 500 | Server error once, success after retry (restart mock to reset) |
| 0 / abc | Invalid route ID; frontend should not request detail |

Verify desktop and mobile widths, newest-first logs, Vietnamese time formatting,
back navigation, loading/error/retry states, and disabled status actions. This task
connects only GET detail; mutation controls remain disabled until their APIs exist.

Stop both servers after testing. Mutation endpoints outside this check return 501.

With a freshly started simulation, run the HTTP and server-rendered markup checks:

```powershell
node scripts/check-staff-reservation-detail.mjs
```

This checks the actual frontend API client and React view against the fixtures;
it does not execute browser effects, validate responsive geometry, or call the live backend.
