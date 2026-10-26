"""Check the local profile against a temporary file DB; run after Gradle build."""
from concurrent.futures import ThreadPoolExecutor
from contextlib import contextmanager
from http.cookiejar import CookieJar
import json
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.request import build_opener, HTTPCookieProcessor, Request


ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/libs/app.jar"


def main():
    if not JAR.is_file():
        raise SystemExit("Build build/libs/app.jar first.")
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        port = listener.getsockname()[1]
    base = f"http://127.0.0.1:{port}/library-dashboard"
    opener = build_opener(HTTPCookieProcessor(CookieJar()))
    env = {key: value for key, value in os.environ.items()
           if not key.startswith(("SPRING_", "SERVER_", "DB_", "ADMIN_", "APP_ADMIN_"))
           and key != "APP_BASE_PATH"}
    password = "local-admin-password"
    csrf = None

    def request(method, path, body=None, status=200):
        headers = {"Content-Type": "application/json"}
        if csrf:
            headers["X-CSRF-Token"] = csrf
        data = None if body is None else json.dumps(body).encode()
        req = Request(base + path, data=data, headers=headers, method=method)
        try:
            response = opener.open(req, timeout=5)
        except HTTPError as error:
            response = error
        with response:
            text = response.read().decode()
            assert response.status == status, (method, path, response.status, text)
            return json.loads(text), response.headers

    with tempfile.TemporaryDirectory(prefix="library-local-") as directory:
        folder = Path(directory)
        command = ["java", "-jar", str(JAR),
                   "--server.address=127.0.0.1", f"--server.port={port}",
                   "--management.endpoints.web.exposure.include=health,shutdown",
                   "--management.endpoint.shutdown.access=unrestricted"]

        @contextmanager
        def running():
            with (folder / "app.log").open("w+", encoding="utf-8") as log:
                process = subprocess.Popen(command, cwd=folder, env=env,
                                           stdout=log, stderr=subprocess.STDOUT)
                try:
                    deadline = time.monotonic() + 60
                    while time.monotonic() < deadline:
                        if process.poll() is not None:
                            raise RuntimeError("Application exited during startup")
                        try:
                            request("GET", "/healthz")
                            break
                        except (URLError, TimeoutError):
                            time.sleep(0.2)
                    else:
                        raise TimeoutError("Application did not become ready")
                    yield
                except BaseException:
                    log.seek(0)
                    print(log.read())
                    raise
                finally:
                    try:
                        if process.poll() is None:
                            if csrf:
                                request("POST", "/actuator/shutdown")
                            else:
                                process.terminate()
                    finally:
                        try:
                            process.wait(timeout=30)
                        except subprocess.TimeoutExpired:
                            process.kill()
                            process.wait(timeout=10)

        with running():
            request("GET", "/api/me", status=401)
            token, headers = request("GET", "/api/auth/csrf")
            cookie = headers.get("Set-Cookie", "").lower()
            assert "httponly" in cookie and "samesite=lax" in cookie
            assert "; secure" not in cookie
            csrf = token["csrf"]
            result, _ = request("POST", "/api/auth/login",
                                {"username": "admin", "password": password})
            csrf = result["csrf"]
            for index in range(7):
                request("POST", "/api/staff", {"name": f"check-{index}"}, status=201)
            staff, _ = request("GET", "/api/staff")
            assert len(staff) == 7 and len({row["id"] for row in staff}) == 7
            request("POST", "/api/staff", {"name": "check-0"}, status=409)
            request("POST", "/api/teachers", {"name": "check-teacher"}, status=201)
            teachers, _ = request("GET", "/api/teachers")
            teacher = teachers[0]["id"]
            for selected in (teacher, None, teacher):
                request("PUT", "/api/duty", {"teacher_id": selected})
            tag = f"/api/tagging/{staff[0]['id']}/2026/10"
            with ThreadPoolExecutor(max_workers=4) as pool:
                list(pool.map(lambda _: request("PUT", tag, {"completed": True}), range(8)))
            for done in (False, True):
                request("PUT", tag, {"completed": done})
            request("POST", "/api/archive/draw", {"month": "2026-10"}, status=201)
            archive, _ = request("GET", "/api/archive?month=2026-10")
            assert len(archive["assignments"]) == 7
            assert all(row["tasks"] for row in archive["assignments"])
            request("POST", "/api/archive/draw", {"month": "2026-10"}, status=409)
            backup, _ = request("GET", "/api/backup")
            assert len(backup["tables"]["monthly_tags"]) == 1
            assert backup["tables"]["monthly_tags"][0]["completed"] == 1
            assert len(backup["tables"]["duty_contact"]) == 1
        assert (folder / "data/library-dashboard.mv.db").is_file()
        env.update(ADMIN_USER="replacement-admin", ADMIN_PASSWORD="replacement-password")
        command.append("--spring.profiles.active=local")
        with running():
            me, _ = request("GET", "/api/me")
            assert me["user"]["username"] == "admin"
            persisted, _ = request("GET", "/api/backup")
            for table, before in backup["tables"].items():
                after = persisted["tables"][table]
                assert sorted(map(lambda row: json.dumps(row, sort_keys=True), before)) == sorted(
                    map(lambda row: json.dumps(row, sort_keys=True), after)), table
        env.pop("ADMIN_USER")
        env.pop("ADMIN_PASSWORD")
        result = subprocess.run(
            ["java", "-jar", str(JAR), "--spring.main.web-application-type=none",
             "--spring.profiles.active=prod"],
            cwd=folder, env=env, capture_output=True, text=True, encoding="utf-8", timeout=45)
        assert result.returncode != 0, "Unexpected production startup without DB settings"
        assert "jdbc:h2:" not in result.stdout, "Unexpected embedded DB fallback"
        result = subprocess.run(
            ["java", "-jar", str(JAR), "--spring.main.web-application-type=none",
             "--spring.profiles.active=prod",
             "--spring.datasource.url=jdbc:h2:mem:prod-check;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR,MONTH,DAY",
             "--spring.datasource.username=sa", "--spring.datasource.password=",
             "--spring.datasource.hikari.connection-init-sql="],
            cwd=folder, env=env, capture_output=True, text=True, encoding="utf-8", timeout=45)
        assert result.returncode != 0, "Unexpected production startup without initial admin"
        assert "Initial admin credentials required" in result.stdout, result.stdout
    print("PASS: local file H2, login/cookies, concurrent saves, generated IDs, rollback, "
          "data/session persistence, no-env local startup, unchanged existing admin, "
          "and required production DB/admin settings")


if __name__ == "__main__":
    main()
