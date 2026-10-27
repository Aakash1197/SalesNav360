# SalesNav360 Phase 1: Existing JIT Architecture Review

**Review scope:** This is the original Phase 1 source-inspection report for the JIT frontend and backend. SalesNav360 implementation subsequently proceeded in the separate `D:\SalesNav360` workspace; JIT remains the architecture reference and was not changed by the SalesNav360 implementation.

**Follow-up architecture decision:** The project owner selected an independent SalesNav360 frontend/backend with its own users, roles, JWT signing key, authentication, and PostgreSQL database. This supersedes the earlier Phase 2 shared-JIT-auth options. Bootstrap creates only the initial SalesNav360 `ADMIN`; credentials are provided only through environment variables. SalesNav360 follows JIT's layered controller/service/repository/entity/DTO responsibilities inside feature modules, with independent security and persistence.

## Frontend architecture

- **Framework and tooling:** React 19 with Create React App (`react-scripts` 5), JavaScript, and npm.
- **UI and styling:** Tailwind CSS, Radix UI components, Lucide icons, Axios, and `framer-motion`. Shared UI components are under `Frontend/src/components/ui`.
- **Structure and entry point:** `Frontend/src/index.js` mounts the app. Feature components and pages are in `src/components`; API functions are in `src/services`; shared contexts, hooks, themes, utilities, and assets have their own directories.
- **Routing:** `BrowserRouter` is mounted, but `App.js` selects the current page using `window.location.pathname`, `pushState`, and local React state. It is not built around route-level protected components.
- **Existing app pages:** The primary authenticated shell links to dashboard, bills, quotations, and customers. The broader component tree also includes product, analytics, feedback, employee/role management, and bill/quotation workflows. The source has 68 component files plus 22 shared UI component files.
- **Authentication and session:** The login page calls the shared Axios API client, stores the returned user profile in `localStorage`, and updates app state. Startup restores that profile from local storage. Logout calls the backend and then removes the saved profile. A `/me` API exists, but the current startup flow does not use it to validate the saved session.
- **Authorization:** Some UI visibility is role-aware, but server-side Spring Security is the actual authorization boundary. Existing frontend role checks must not be treated as sufficient access control.
- **API configuration and errors:** `src/services/api.js` reads `REACT_APP_API_URL`, uses `withCredentials`, and centralizes error notifications and unauthorized handling. `.env`, `.env.development`, and `.env.production` each define that variable. The local `.env` currently points to the localhost backend. CRA's `setupProxy.js` separately proxies Swagger, OpenAPI, and actuator requests; the Axios client uses its configured base URL for application API calls.
- **State and validation:** State is primarily component state, with a notification context and reusable persisted-form hook. Form validation is handled in components and by backend validation responses.
- **Reusable components:** Existing shell/navigation, dashboard patterns, login page, theme provider, notification system, modal/dialog components, form controls, and API client conventions can inform a separate CRM frontend. JIT-specific branding and features should not be copied as business functionality.
- **Frontend test coverage:** `App.test.js` is still the Create React App starter test expecting a "learn react" link. It does not represent meaningful application or CRM integration coverage. The rest of the reviewed frontend test inventory is small.

## Backend architecture

- **Framework and build:** Java 17, Spring Boot 4.0.0, Maven.
- **Package layers:** `controller`, `service`, `repository`, `entity`, `dto`, `security`, `exception`, and supporting packages. The application main class is `com.jit.JitApplication`.
- **Persistence:** PostgreSQL with Spring Data JPA and Hibernate. Entities include employees and roles, customers, products, quotations and quotation items, sales orders and items, payments, feedback, and other JIT domain data.
- **API conventions:** REST endpoints follow `/api/v1/jit/...`; controllers call service and repository layers, and DTOs are used for API inputs/outputs. OpenAPI/Springdoc is configured.
- **Existing API groups:** Auth; admin/public feedback; categories; customers; employees and roles; products and product bills; quotations; payments; sales orders and sales-order items; and YouTube videos. Customer list and employee/role operations use paginated response DTOs. Sales-order and product APIs include report, upload/download, and document workflows.
- **Validation and errors:** Jakarta Bean Validation is used. `MyGlobalExeceptionHandler` centralizes validation, application, persistence-integrity, authorization, and other exceptions.
- **Logging:** SLF4J/Logback conventions are present, with profile-dependent logging configuration.
- **Tests:** The backend test tree contains the Spring Boot context test and DTO tests for categories/employees. This is not broad coverage of authorization, persistence workflows, or CRM requirements.

## Authentication and authorization flow

1. The frontend sends credentials to `POST /api/v1/jit/auth/login`.
2. Spring Security authenticates the username/password through an `AuthenticationManager`, employee-backed `UserDetailsService`, and BCrypt password encoder.
3. On success, the backend creates a JWT in an HttpOnly cookie and returns the user's ID, username, email, and authorities.
4. The frontend stores that profile in local storage. Axios includes credentials on API calls.
5. `AuthTokenFilter` reads the JWT cookie (or an Authorization bearer header), validates it, loads the active employee, and populates the Spring Security context.
6. The frontend calls logout; the backend clears the JWT cookie, and the frontend clears local storage.

There are no refresh-token endpoints in the inspected flow. JWT expiry is configured through application properties.

## Existing users, roles, and permissions

- `Employee` maps to `employees`, including username, password hash, email, active flag, and audit fields.
- `Role` maps to `roles`; employees and roles are linked through `employee_roles`.
- Existing role names are `ADMIN`, `IT`, `AMC_USER`, and `GUEST_USER`.
- Role records also carry read/write/delete/insert/export flags. `UserDetailsImpl` maps role names to `ROLE_*` authorities and adds corresponding granular permission authorities.
- Spring Security applies role and HTTP-method rules at the backend. CRM authorization should reuse or compatibly extend this system; it must not grant roles based only on frontend values.
- The JWT cookie builder currently sets `secure(false)` in code, while its domain is profile-configurable. Review cookie security and same-site behavior before any production hostname or deployment changes.

## Database and schema evolution

- PostgreSQL is the database used in local and Docker configurations.
- Existing schema is represented by JPA entities and relationships.
- The inspected Maven configuration did not show Flyway or Liquibase. Profile files set Hibernate `ddl-auto=update`, including the production profile.
- A live database was not inspected. The schema findings above are based on source code, not a database dump.
- Before adding CRM tables, choose and test a safe schema-evolution strategy. Do not drop, recreate, or overwrite existing JIT tables or records.

## Profiles and environment configuration

The resource directory contains `application.properties` plus `application-localhost.properties`, `application-dev.properties`, and `application-prod.properties`. Local configuration targets a host PostgreSQL instance; Docker configuration uses environment variables and the Compose database service hostname. The base properties currently select the `localhost` profile.

The profile files currently include JWT signing-secret values in source-controlled configuration. Database credentials are represented through environment-variable placeholders in profile configuration, and Compose supplies its own values at runtime. Production and local values must remain properly separated. CRM deployment should move sensitive values to external environment/secrets configuration and verify cookie, CORS, and frontend API settings for each environment before deployment.

## Docker and deployment

- The repository-root Compose file defines PostgreSQL, backend, and frontend services on a shared network.
- PostgreSQL data is stored in a named volume; backend startup depends on database health.
- The backend Dockerfile is multi-stage and packages built frontend assets into the Spring Boot application.
- The frontend has a separate Nginx image and proxy configuration for API and supporting routes.
- Compose injects the Docker database connection settings and activates the production profile for the backend.
- The frontend image uses an Nginx reverse proxy to the backend service for `/api/`, actuator, and OpenAPI/Swagger paths. The backend Dockerfile also builds frontend assets into the backend artifact, so there are two frontend packaging paths; decide which one is authoritative for a separate CRM deployment to avoid duplicated build/deploy responsibilities.

Preserve existing service boundaries and persistent-volume behavior when adding a separate CRM deployment. Do not remove or recreate the existing database volume as a routine build or deployment step.

## Reusable patterns and required additions

**Reuse or adapt:** JIT JWT authentication, employee and role model, Spring Security configuration, API client conventions, DTO validation, exception handling, logging, PostgreSQL connectivity, profile conventions, Docker networking, and selected generic UI components.

**Add for the CRM:** CRM-specific domain entities/DTOs/repositories/services/controllers and tests; CRM user journeys and navigation; data-backed dashboards and reports; and a safe database migration/evolution plan. Keep CRM features modular and avoid changing existing JIT endpoints or behavior unnecessarily.

The specification references UI screenshots, but no SalesNav360 screenshots were available in the reviewed workspace context. The design cannot be assessed against those references until they are provided.

## Compatibility and design risks

1. **Springdoc compatibility:** The project combines Spring Boot 4.0.0 with Springdoc 2.7.0. Validate dependency and runtime compatibility before relying on or upgrading this combination.
2. **Frontend toolchain:** Validate React 19 with Create React App 5 and the current test/build tooling before making broad UI changes.
3. **Schema updates:** Hibernate `ddl-auto=update` is not a reviewed migration/rollback process. CRM schema changes need a safe, repeatable strategy that preserves existing records.
4. **Session restoration:** Frontend local storage is used to restore the profile without first verifying the JWT via `/me`; CRM pages must not infer authorization from this client-side state.
5. **Cookie/CORS configuration:** Verify local and deployed origins, cookie domain/security/SameSite settings, and credentialed CORS behavior for the chosen CRM hostnames.
6. **Secrets in profile files:** JWT signing secrets are configured as source literals. Do not copy these values into the new project; plan external secret configuration and rotation as a separate deployment concern.
7. **Duplicate frontend packaging:** The Compose frontend container and the backend's multi-stage image both build/package the frontend. Choose a clear serving model for the CRM.
8. **Separate app versus module:** The request calls SalesNav360 a new project while also requiring JIT compatibility. Confirm whether it should be independently deployable or a CRM module in the existing monorepo before selecting database and API boundaries.
9. **Screenshot gap:** Provide the referenced UI screenshots before finalizing detailed layout and visual requirements.
10. **Test baseline:** Existing test files provide limited baseline coverage; establish focused frontend/backend tests before relying on changes to shared authentication, roles, or schema.

## Proposed implementation sequence

1. Confirm whether SalesNav360 is a separate deployable application or a CRM module, and provide the UI references.
2. Define data ownership, API boundaries, roles, and CRM-specific authorization while preserving existing JIT users and roles.
3. Design CRM schema changes and a safe migration/rollback approach.
4. Implement CRM backend modules and focused authorization, validation, and persistence tests.
5. Implement frontend pages and navigation using compatible shared UI patterns and the existing API/auth conventions.
6. Integrate local and Docker profiles without altering JIT behavior or removing persistent data.
7. Run targeted builds/tests, then validate startup, database persistence, authentication, and access control in the intended environments.

## Review boundaries

This report documents source-level inspection and repository inventory only. No live production environment, running PostgreSQL schema, deployment server, or SalesNav360 UI screenshots were available for verification. Tests and builds were not run because Phase 1 is an inspection/report deliverable. No application files were changed as part of Phase 1.
