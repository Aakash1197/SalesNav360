# SalesNav360

SalesNav360 is an independently deployable CRM frontend and backend. It owns its authentication, authorization, JWT signing key, users, roles, and PostgreSQL database. It has no runtime dependency on another project's frontend, backend, authentication, or database.

Core Phase 3 workflows are implemented, and Phase 4 API development is underway. Implemented functionality includes a database-backed dashboard, client/investor CRUD and archive, lead pipeline with server-validated stage transitions and duplicate-safe lead conversion, task management and follow-ups, policies and portfolio metrics, decimal-safe quotations with downloadable PDFs, calendar events, email templates and communication history, analytics/report summaries, responsive light/dark frontend views, role-protected user and role administration, paginated audit history, and login/CRM audit events.

## Architecture

- `frontend/`: React 19/Create React App web app.
- `backend/`: Java 17/Spring Boot REST API.
- Backend Java sources follow centralized JIT-style layers under `com.salesnav360`: `controller`, `dto`, `service`, `service.impl`, `entity`, `repository`, `exception`, `config`, `enums`, `pdf`, `security`, and `utilis`.
- Runtime configuration is shared through `application.properties` and separated into `application-localhost.properties`, `application-dev.properties`, and `application-prod.properties`. `localhost` is the default profile; select another profile with `SPRING_PROFILES_ACTIVE`.
- `logback-spring.xml` uses console logging locally and size/time-rotated file logging for development and production. Configure `LOG_PATH` and `LOG_FILE` to choose the log destination and base name.
- PostgreSQL database: `SALESNAV360`, owned by this application.
- Authentication: SalesNav360-local username/password with BCrypt and a SalesNav360-signed JWT in an HttpOnly cookie.
- Authorization: persistent SalesNav360 roles enforced by Spring Security. Bootstrap creates only the initial `ADMIN` role. Administrators may create only the supported non-admin roles through the protected admin API.
- CSRF: state-changing requests require a CSRF token issued by the backend and sent as `X-XSRF-TOKEN`.

The frontend displays the current user's roles but does not enforce security by itself. The backend validates the signed token, reloads the enabled user and current roles from the SalesNav360 database on each request, and enforces permissions server-side.

## Local prerequisites

- Java 17+
- Maven 3.9+
- Node.js 20+ and npm
- A PostgreSQL server where you can create a dedicated `SALESNAV360` database

Create the separate local database using pgAdmin or a PostgreSQL command-line client:

```sql
CREATE DATABASE "SALESNAV360";
```

Do not point SalesNav360 at another application's database. The backend creates/updates only its own `sn_*` tables in `SALESNAV360`, including users, roles, clients, leads, and append-only-by-API audit events.

## Configure and run the backend

In a PowerShell terminal, set a private JWT secret of at least 64 UTF-8 bytes, the database password, and first-admin credentials. Do not commit these values:

```powershell
$env:SALESNAV_JWT_SECRET = Read-Host "Enter a random secret of at least 64 characters"
$env:DB_PASSWORD = Read-Host "Enter the SALESNAV360 PostgreSQL password"
$env:SALESNAV_BOOTSTRAP_ADMIN_USERNAME = Read-Host "Choose the first admin username"
$env:SALESNAV_BOOTSTRAP_ADMIN_EMAIL = Read-Host "Choose the first admin email"
$securePassword = Read-Host "Choose a strong first admin password (12+ characters)" -AsSecureString
$env:SALESNAV_BOOTSTRAP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
```

Then run from `backend`:

```powershell
mvn spring-boot:run
```

The API listens at `http://localhost:8082`. The health endpoint is `http://localhost:8082/actuator/health`. The first admin is inserted only if the SalesNav360 user table is empty. `application.properties` maps the `SALESNAV_BOOTSTRAP_ADMIN_*` environment variables to the bootstrap settings used by the application; provide them on the first start only. If the table is empty and bootstrap variables are missing or invalid, startup fails with an explanatory error. Once an account exists, changing bootstrap variables does not change that account.

The default `localhost` profile uses the local PostgreSQL URL and `DDL_AUTO=update`. Profile configuration stays in `src/main/resources/application.properties` and `application-{profile}.properties`; secrets are supplied through environment-variable placeholders and must not be committed in those files. Select `dev` or `prod` by setting `$env:SPRING_PROFILES_ACTIVE = "dev"` or `"prod"` before launching. Development requires explicit database credentials. Production defaults to `DDL_AUTO=validate` and Secure cookies; provide an exact `SALESNAV_CORS_ALLOWED_ORIGINS` value and provision the schema before startup. A database migration framework is not configured yet, so production deployments must provision and manage schema changes outside this application.

Important backend environment variables:

| Variable | Purpose | Local default |
|---|---|---|
| `DB_URL` | JDBC URL for the independent SalesNav360 database | `jdbc:postgresql://localhost:5432/SALESNAV360` |
| `DB_USER` | PostgreSQL user | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | Required by your local PostgreSQL setup |
| `SALESNAV_JWT_SECRET` | Private JWT signing key, at least 64 UTF-8 bytes | Required |
| `SALESNAV_JWT_COOKIE_NAME` | JWT cookie name | `salesnav360` |
| `SALESNAV_COOKIE_SECURE` | Mark JWT cookie Secure; set true behind HTTPS | `false` |
| `SALESNAV_JWT_EXPIRATION_SECONDS` | JWT lifetime | `3600` |
| `SALESNAV_CORS_ALLOWED_ORIGINS` | Exact comma-separated frontend origins | `http://localhost:3000` |
| `SALESNAV_BOOTSTRAP_ADMIN_USERNAME` | First admin username | Required only while there are no users |
| `SALESNAV_BOOTSTRAP_ADMIN_EMAIL` | First admin email | Required only while there are no users |
| `SALESNAV_BOOTSTRAP_ADMIN_PASSWORD` | First admin password, minimum 12 characters | Required only while there are no users |
| `DDL_AUTO` | Hibernate schema behavior; defaults to `update` locally/dev and `validate` in prod | Profile-dependent |
| `SPRING_PROFILES_ACTIVE` | Select `localhost`, `dev`, or `prod` configuration | `localhost` |
| `LOG_PATH` / `LOG_FILE` | Directory and base file name for development/production logs | `logs` / `salesnav360` |

Use a secret manager or protected deployment environment for production values. Keep `SALESNAV_COOKIE_SECURE=true` in HTTPS deployments. Local and development profiles use `update` by default; production uses `validate`. Plan and apply database migrations before production schema evolution.

## Run the frontend

In another terminal, from `frontend`:

```powershell
npm install
npm run start:local
```

`start:local` uses the local API default at `http://localhost:8082/api/v1/salesnav`; no env file is required. To override it, copy `.env.local.example` to `.env.local` and change `REACT_APP_API_URL`. For dev or production API targets, copy `.env.dev.example` to `.env.dev` or `.env.prod.example` to `.env.prod`, replace `REACT_APP_API_URL` with the corresponding backend URL, then run `npm run start:dev` or `npm run start:prod`. These local environment files are ignored by Git. `REACT_APP_*` values are included in the frontend bundle, so they must not contain secrets. `start:prod` runs the CRA development server against the configured production API; use `npm run build:prod` to create the production bundle.

Open `http://localhost:3000` when using a start command.

Login (the frontend handles CSRF-token retrieval and sends the required header automatically):

```http
GET http://localhost:8082/api/v1/salesnav/auth/csrf
```

Then:

```http
POST http://localhost:8082/api/v1/salesnav/auth/login
Content-Type: application/json
X-XSRF-TOKEN: <token returned by GET /api/v1/salesnav/auth/csrf>

{"username":"<bootstrap-admin>","password":"<admin-password>"}
```

Use browser credentials (`withCredentials`) so the HttpOnly session cookie is sent. The React app obtains and sends the CSRF token automatically.

## Implemented API endpoints

- `GET /actuator/health` — service health.
- `GET /api/v1/salesnav/auth/csrf` — issue a CSRF token for browser requests.
- `POST /api/v1/salesnav/auth/login` — authenticate a SalesNav360 account and set its JWT cookie.
- `POST /api/v1/salesnav/auth/logout` — clear the JWT cookie.
- `GET /api/v1/salesnav/auth/me` — return the current SalesNav360 user's profile; requires authentication.
- `GET /api/v1/salesnav/admin/access` — admin-only access check.
- `GET /api/v1/salesnav/admin/users?query=...&page=0&size=20` — admin-only user search and pagination.
- `POST /api/v1/salesnav/admin/users` — admin-only user creation; password is BCrypt-hashed and roles must already exist.
- `PATCH /api/v1/salesnav/admin/users/{id}` — admin-only activation/deactivation and role assignment; prevents self-access changes and loss of the last enabled administrator.
- `GET /api/v1/salesnav/admin/roles` — list persisted roles (admin only).
- `POST /api/v1/salesnav/admin/roles` — create a supported role from the bounded role catalog (admin only).
- `GET /api/v1/salesnav/admin/audit-events?page=0&size=50` — paginated administrative audit history (admin only).
- `GET /api/v1/salesnav/dashboard` — persisted client, investor, lead, pipeline, follow-up and activity metrics.
- `GET /api/v1/salesnav/clients` — filtered, sorted, paginated client/investor list (`query`, `status`, `assignedTo`, `page`, `size`, `sort`, `direction`). Sort fields: `fullName`, `createdAt`, `updatedAt`, `status`.
- `GET /api/v1/salesnav/clients/{id}` — client detail.
- `GET /api/v1/salesnav/clients/duplicates?email=...&phone=...` — potential email/phone matches.
- `POST /api/v1/salesnav/clients` — create a client or investor.
- `PUT /api/v1/salesnav/clients/{id}` — update a client or investor.
- `DELETE /api/v1/salesnav/clients/{id}` — archive (soft-delete) a record.
- `GET /api/v1/salesnav/leads` — filtered, sorted and paginated leads (`query`, `stage`, `assignedTo`, `page`, `size`, `sort`, `direction`). Sort fields: `fullName`, `createdAt`, `updatedAt`, `expectedValue`, `followUpDate`, `stage`.
- `GET /api/v1/salesnav/leads/{id}` — lead detail.
- `POST /api/v1/salesnav/leads` — create a lead in the `NEW` stage.
- `PUT /api/v1/salesnav/leads/{id}` — update a lead, with the same server-side stage transition checks.
- `PATCH /api/v1/salesnav/leads/{id}/stage` — validate and move a lead through the pipeline.
- `POST /api/v1/salesnav/leads/{id}/convert` — convert a qualified/proposal lead into a client prospect; rejects repeated conversion and active duplicate contact matches; does not mark a sale as won.
- `GET /api/v1/salesnav/tasks?query=...&status=...&assignedTo=...&page=0&size=20&sort=dueDate&direction=ASC` — searchable, filterable, sorted, paginated task list.
- `GET /api/v1/salesnav/tasks/{id}` — task detail.
- `POST /api/v1/salesnav/tasks` — create an open task with optional assignee, due date, and client/lead link.
- `PUT /api/v1/salesnav/tasks/{id}` — update task details without changing its status.
- `PATCH /api/v1/salesnav/tasks/{id}/status` — change task status using backend-validated transitions and completion timestamp.
- `GET /api/v1/salesnav/policies?query=...&status=...&assignedTo=...&page=0&size=20` — policy search, filtering, sorting and pagination; requires an insurance-operations role.
- `GET /api/v1/salesnav/policies/{id}` — policy details with linked client.
- `POST /api/v1/salesnav/policies` — create a policy linked to an active client, with decimal premium/coverage and date validation.
- `PUT /api/v1/salesnav/policies/{id}` — update policy information and status.
- `GET /api/v1/salesnav/policies/portfolio` — portfolio totals, active premium value, and active renewals due within 30 days.
- `GET /api/v1/salesnav/quotations?query=...&status=...&page=0&size=20` — searchable quotation page with status filter.
- `GET /api/v1/salesnav/quotations/{id}` — quotation and line item details.
- `POST /api/v1/salesnav/quotations` — create a server-numbered draft with line items; subtotal, discount, tax and total are calculated with `BigDecimal`.
- `PUT /api/v1/salesnav/quotations/{id}` — update a draft quotation only.
- `PATCH /api/v1/salesnav/quotations/{id}/status` — transition draft/sent/accepted/rejected/expired status; acceptance does not mark a lead as won.
- `GET /api/v1/salesnav/quotations/{id}/pdf` — download an authenticated PDF quotation.
- `GET /api/v1/salesnav/calendar/events?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD&page=0&size=50` — date-filtered, searchable calendar events.
- `POST /api/v1/salesnav/calendar/events` and `PUT /api/v1/salesnav/calendar/events/{id}` — create or update an event, optionally linked to an active client/investor.
- `DELETE /api/v1/salesnav/calendar/events/{id}` — delete a calendar event.
- `GET /api/v1/salesnav/communications/templates?page=0&size=50` — search and page through email templates.
- `POST /api/v1/salesnav/communications/templates` and `PUT /api/v1/salesnav/communications/templates/{id}` — create or update a reusable email template.
- `GET /api/v1/salesnav/communications/history?page=0&size=50` — paginated client/lead communication history.
- `POST /api/v1/salesnav/communications/history` — record an unsent communication draft against a client or lead.
- `GET /api/v1/salesnav/analytics/summary` and `GET /api/v1/salesnav/reports/summary` — aggregate lead, task, policy, and accepted-quotation metrics.

There is no public signup endpoint. User and role changes are enforced by Spring Security and service-level validation; client-side visibility is not the authorization boundary.

### Dashboard metric definitions and current limits

- Active pipeline stages are `NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL`, and `NEGOTIATION`; their `expectedValue` amounts are summed for active pipeline value.
- Closed-won value sums `expectedValue` for leads in `WON`. It is a CRM expected-value measure, not booked accounting revenue.
- Win conversion is `WON / all leads except UNQUALIFIED`, shown as a percentage rounded to one decimal place. `LOST` leads remain in this denominator.
- Average won deal size is closed-won value divided by won-lead count.
- Follow-ups due today count leads with a due date today excluding `WON`, `LOST`, and `UNQUALIFIED`.
- Active tasks count `OPEN` and `IN_PROGRESS`; completed tasks count `COMPLETED`; overdue tasks are active tasks with a due date before today. Upcoming tasks are the next five active tasks due today or later.
- Quota/target and quotation-conversion metrics are omitted until those business definitions are implemented. No sample business records are seeded.
- Client deletion archives records. Client export currently exports the loaded page only; bulk import is not implemented.
- Client/lead assignment accepts only the username of an enabled SalesNav360 account.
- Successful logins and client, lead, user, and role changes record an actor, action, entity type/id, timestamp, and non-sensitive summary in `sn_audit_events`. Audit records have no mutation API; administrators can read them through the paginated admin endpoint.
- Task links currently support active clients and existing leads. Reminder delivery is not implemented.
- Policy CRUD currently covers client/provider/type, premium, coverage, dates, status, and assignee; renewal reminders are displayed as dates only and are not sent as notifications.
- Quotation drafts, line items, decimal-safe totals, status history, and PDF downloads are implemented. Sending a quotation through an external email provider is not implemented.
- Updating a quotation to `SENT` changes its CRM status only; it does not send an email.
- Calendar events support date filtering, event details, and optional active-client association; automatic reminders and recurring events are not implemented.
- Email templates and communication history are persisted, but there is no SMTP or email API integration. Recording a communication does not send it; the UI and API mark records as not sent.
- Analytics and reports currently expose aggregate lifetime metrics and a CSV summary. Date-ranged/monthly/quarterly reports, scheduled reports, and sales-representative breakdowns are not implemented.
- Still not implemented: client interaction history beyond recorded email communications, import and all-record export, custom fields/layouts, and configurable quotas. Do not treat these as available CRM workflows.
- No migration framework or production migration scripts are configured; production schema migrations must be designed, applied, and tested before deployment.

## Docker Compose

Set the required values in the PowerShell session before starting Compose:

```powershell
$env:DB_PASSWORD = Read-Host "Choose a strong database password"
$env:SALESNAV_JWT_SECRET = Read-Host "Enter a random JWT secret of at least 64 characters"
$env:SALESNAV_BOOTSTRAP_ADMIN_USERNAME = Read-Host "Choose the first admin username"
$env:SALESNAV_BOOTSTRAP_ADMIN_EMAIL = Read-Host "Choose the first admin email"
$securePassword = Read-Host "Choose a strong first admin password (12+ characters)" -AsSecureString
$env:SALESNAV_BOOTSTRAP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
docker compose up --build
```

Frontend: `http://localhost:3000`. The frontend container proxies `/api/` to the backend on the internal Compose network. PostgreSQL data is persisted in a named volume. The database service does not expose its port publicly.

Inspect and stop:

```powershell
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
docker compose down
```

`docker compose down` preserves the named database volume. Do not use `docker compose down -v` unless you explicitly intend to erase all SalesNav360 database data.

## Tests

Backend:

```powershell
cd backend
mvn test
```

Frontend:

```powershell
cd frontend
npm test -- --watchAll=false
npm run build
```

The available screenshot assets were not present in the workspace during this review; the current UI follows the supplied description but has not been visually compared against the screenshots.
