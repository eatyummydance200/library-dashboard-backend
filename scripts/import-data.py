"""Import a v7 SQLite snapshot or API JSON export into a new PostgreSQL database."""
import argparse
import json
from pathlib import Path
import sqlite3
import psycopg
from psycopg import sql

TABLES = (
    "staff", "users", "teachers", "shifts", "issues", "monthly_tags",
    "archive_draws", "archive_assignments", "archive_tasks", "duty_contact",
)
IDENTITIES = (
    "staff", "users", "teachers", "shifts", "issues",
    "archive_draws", "archive_assignments", "archive_tasks",
)


def load(path):
    if path.suffix == ".json":
        data = json.loads(path.read_text(encoding="utf-8"))
        if data.get("format") != "library-dashboard-v1":
            raise ValueError("Unsupported JSON export format")
        tables = data["tables"]
    else:
        with sqlite3.connect(path.resolve().as_uri() + "?mode=ro", uri=True) as db:
            db.row_factory = sqlite3.Row
            if db.execute("PRAGMA user_version").fetchone()[0] != 7:
                raise ValueError("Upgrade the SQLite database to v7 with the old app first")
            if db.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
                raise ValueError("SQLite integrity check failed")
            if db.execute("PRAGMA foreign_key_check").fetchall():
                raise ValueError("SQLite foreign key check failed")
            db.execute("BEGIN")
            tables = {table: [dict(r) for r in db.execute('SELECT * FROM "' + table + '"')]
                      for table in TABLES}
    if set(tables) != set(TABLES) or not tables["users"]:
        raise ValueError("All business tables and an existing user are required")
    if not any(u["role"] == "admin" and u["active"] == 1 for u in tables["users"]):
        raise ValueError("The source must contain an active admin")
    return tables


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("--replace-bootstrap-admin", action="store_true",
                        help="Replace the sole admin in an otherwise empty target")
    args = parser.parse_args()
    tables = load(args.source)
    # PGHOST, PGPORT, PGDATABASE, PGUSER and PGPASSWORD come from libpq's environment.
    with psycopg.connect() as target:
        target.execute("SELECT pg_advisory_xact_lock(714219801)")
        for table in TABLES:
            count = target.execute(sql.SQL("SELECT count(*) FROM {}").format(
                sql.Identifier(table))).fetchone()[0]
            if count and table != "users":
                raise ValueError("Target must be empty: " + table)
        users = target.execute("SELECT role FROM users").fetchall()
        if users and (not args.replace_bootstrap_admin or users != [("admin",)]):
            raise ValueError("Target has users; only a sole bootstrap admin may be replaced")
        target.execute("DELETE FROM spring_session")
        target.execute("DELETE FROM login_attempts")
        target.execute("DELETE FROM users")
        for table in TABLES:
            for row in tables[table]:
                columns = list(row)
                target.execute(sql.SQL("INSERT INTO {} ({}) VALUES ({})").format(
                    sql.Identifier(table),
                    sql.SQL(",").join(map(sql.Identifier, columns)),
                    sql.SQL(",").join(sql.Placeholder() for _ in columns)),
                    [row[c] for c in columns])
        for table in IDENTITIES:
            target.execute(sql.SQL(
                "SELECT setval(pg_get_serial_sequence(%s,'id'), "
                "COALESCE((SELECT max(id) FROM {}),0)+1, false)"
            ).format(sql.Identifier(table)), (table,))
    print("Import committed. Old sessions were not imported. Source was not modified.")


if __name__ == "__main__":
    main()
