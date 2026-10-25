What this repo does: This API manages a library team’s shifts, contacts, issues, tagging, and newspaper archive work. Admins manage people and assignments. Workers use cookie sessions to update their work.

Assumed load: one small library team, with two or more API Pods behind a proxy and one shared PostgreSQL database. This is an audit of commit c8ad548 before the requested changes. Each fix below is included in this branch.

## Must fix

1. **Each Pod would have its own database** (original `src/main/java/dev/library/Db.java:19-94`, `Dockerfile:12-16`)
   - **What this is:** The app stores all business data in one SQLite file on a local container volume.
   - **Problem:** Two Pods with separate disks show different schedules. Sharing that file between nodes also keeps file locking in the app’s data path.
   - **Fix:** Done. Use one PostgreSQL database, a pool of five connections per Pod, and Flyway for schema changes. Flyway tracks each applied SQL version and locks schema changes during startup.
   - **If we skip it:** Users can read or change a different copy of the data after each request.

2. **Login state disappears between Pods** (original `src/main/java/dev/library/Auth.java:61-65`, `src/main/resources/application.properties:3-10`)
   - **What this is:** Login puts the user ID, account version, and CSRF token in the servlet session. A CSRF token proves that a write came from the client that started the session.
   - **Problem:** Sessions live in one JVM’s memory. A login on Pod A is unknown to Pod B; restarting A logs its users out.
   - **Fix:** Done. Store sessions in PostgreSQL with Spring Session JDBC. Keep the cookie name, account-version checks, and CSRF flow.
   - **If we skip it:** Users get random 401 or 403 replies during load balancing and rollouts.

3. **Parallel logins can bypass the attempt limit** (original `src/main/java/dev/library/Auth.java:29-59`)
   - **What this is:** Five failed logins should block that username for fifteen minutes.
   - **Problem:** Counting attempts and recording the failed login happen in separate transactions. Parallel requests can all read fewer than five attempts before any writes finish.
   - **Fix:** Done. Count, verify, and record within one database transaction protected by a shared lock. Return the 401 after committing the failure count.
   - **If we skip it:** The limit does not hold when requests arrive together, including across Pods.

4. **The old single-writer rule would disappear after the DB switch** (original `src/main/java/dev/library/Db.java:104-117`, `src/main/java/dev/library/Api.java:587-701`)
   - **What this is:** SQLite’s immediate write transaction holds one write lock while the app checks a schedule or changes an archive assignment.
   - **Problem:** Replacing only the connection URL with PostgreSQL removes that lock. Two overlapping shifts can both pass the check. A worker’s completion and an admin’s assignment rewrite can also overwrite each other.
   - **Fix:** Done. Use a PostgreSQL advisory transaction lock for business writes across all Pods. This is a lock with a fixed name stored in the shared database; it releases when the transaction ends. Keep one lock for this small team.
   - **If we skip it:** The same person can get overlapping shifts, or completed work can be lost during reassignment.

5. **A database outage could restart every API Pod** (original `src/main/java/dev/library/Api.java:104-113`)
   - **What this is:** The only health endpoint checks a database connection.
   - **Problem:** Using it as Kubernetes liveness makes a database outage look like a dead app. Kubernetes then restarts healthy JVMs while the database is still down.
   - **Fix:** Done. Separate liveness from readiness. Liveness checks the app; readiness also checks PostgreSQL and stops new traffic when it is unavailable.
   - **If we skip it:** A database outage creates a restart loop and slows recovery.

## Should fix

6. **Backup and deploy code depend on retired infrastructure** (original `scripts/deploy.py:1-93`, `src/main/java/dev/library/Db.java:176-221`, `ops/`)
   - **What this is:** A Python script calls the former deployment manager. A system timer runs SQLite file backups inside a named container.
   - **Problem:** These commands require the old control service, fixed container names, fixed volumes, and a local SQLite file. None fits the requested Kubernetes/PostgreSQL setup.
   - **Fix:** Done. Delete those scripts and timers. Add a Deployment and Service, use PostgreSQL backup tools outside API Pods, and keep the admin download endpoint as a consistent JSON export. Add a guarded importer for SQLite v7 and that JSON format.
   - **If we skip it:** Deployment and recovery instructions invoke services and files that no longer exist.

7. **Build and runtime instructions would disagree** (original `pom.xml:1-16`, `Dockerfile:1-17`, `.github/workflows/image.yml:16-28`)
   - **What this is:** Maven, CI, the image, and the README each describe the old application.
   - **Problem:** Changing only the build file leaves CI running Maven and images exposing port 8000. The old Tomcat customizer and Jackson imports also use the older Spring Boot layout.
   - **Fix:** Done. Switch the build, CI, Dockerfile, JSON mapper import, and docs together. Pin Spring Boot 4.1.2 and the Gradle 9.7.1 Wrapper; use port 8080. Delete the old Tomcat customizer because this API does not serve redirecting pages.
   - **If we skip it:** The source may compile locally while CI or the deployment still uses the old build and port.

## Verification performed

- Gradle Wrapper build and executable JAR: passed on Java 21. Gradle reports no test source.
- Docker image build from the new Dockerfile: passed.
- PostgreSQL 18.3: schema creation and subsequent Flyway validation passed.
- Two API instances: shared session, restart survival, account revocation, CSRF, admin/worker checks, login throttling, both probes, and the core data APIs passed.
- Simultaneous overlapping shift writes across instances: one succeeded and one returned 409.
- JSON restore and SQLite v7 import: every business table matched. IDs, password hashes, completion state, and source file bytes were preserved. Generated IDs advanced correctly.
- Import safety: a populated target was rejected; a foreign-key failure rolled back all inserted rows and kept the bootstrap admin.
- Compose configuration: passed. Kubernetes client validation could not reach the configured cluster, so no cluster-level validation is claimed.

Verdict: The requested platform changes and the concrete multi-Pod blockers above are fixed. Review the migration and proxy settings before production rollout.

Test source and dependencies were removed at the owner’s request. The change keeps temporary verification out of the tracked source tree. No automated regression suite remains in CI.

Lean: -127 lines, -0 dependencies possible. The retired deployment scripts and timers are removed in this branch. This count excludes tests, database code, and generated Wrapper files.

Not checked: Real production data, the user’s Kubernetes cluster, Ingress and certificates, and a production restore. The frontend is a separate repository and its proxy still needs port 8080 and the real Ingress address. Large-load behavior was not measured; the shared write lock is an explicit small-team limit.
