package dev.library;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;

@Component
@DependsOnDatabaseInitialization
class Db {
  private static final Logger LOG = LoggerFactory.getLogger(Db.class);
  private final DataSource source;
  static final List<String> TABLES = List.of(
      "staff", "users", "teachers", "shifts", "issues", "monthly_tags",
      "archive_draws", "archive_assignments", "archive_tasks", "duty_contact");

  interface Work<T> {
    T run(Connection c) throws Exception;
  }

  Db(DataSource source) {
    this.source = source;
    write(c -> {
      if (one(c, "SELECT id FROM users LIMIT 1") == null) {
        String name = System.getenv().getOrDefault("ADMIN_USER", ""),
            password = System.getenv().getOrDefault("ADMIN_PASSWORD", "");
        if (!name.matches("[A-Za-z0-9_.-]{2,32}")
            || password.length() < 10 || password.length() > 128)
          throw new IllegalArgumentException("Initial admin credentials required");
        update(c, "INSERT INTO users(username,password_hash,role) VALUES (?,?,'admin')",
            name, Passwords.encode(password));
      }
      return null;
    });
  }

  <T> T read(Work<T> work) {
    return transaction(work, true);
  }

  <T> T write(Work<T> work) {
    return transaction(work, false);
  }

  private <T> T transaction(Work<T> work, boolean readOnly) {
    try (Connection c = source.getConnection()) {
      c.setReadOnly(readOnly);
      c.setTransactionIsolation(readOnly
          ? Connection.TRANSACTION_REPEATABLE_READ : Connection.TRANSACTION_READ_COMMITTED);
      c.setAutoCommit(false);
      try {
        // shortcut: serialize this small team's writes across Pods; split locks if write waits grow.
        if (!readOnly) one(c, "SELECT pg_advisory_xact_lock(714219801)");
        T value = work.run(c);
        c.commit();
        return value;
      } catch (Exception e) {
        try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
        throw e;
      }
    } catch (Exception e) {
      throw failure(e);
    }
  }

  static RuntimeException failure(Exception e) {
    if (e instanceof SQLException sql) {
      String state = sql.getSQLState() == null ? "" : sql.getSQLState();
      if (state.startsWith("23"))
        return new Api.Failure(409, "같은 이름이 있거나 입력값이 올바르지 않습니다.");
      if (state.startsWith("40") || state.equals("55P03"))
        return new Api.Failure(409, "다른 요청과 충돌했습니다. 새로고침 후 다시 시도해주세요.");
      LOG.error("Database operation failed (SQLSTATE {})", state, sql);
      return new Api.Failure(503, "저장소에 접근하지 못했습니다. 잠시 후 다시 시도해주세요.");
    }
    if (e instanceof RuntimeException r) return r;
    return new IllegalStateException(e);
  }

  private static void bind(PreparedStatement p, Object[] args) throws SQLException {
    for (int i = 0; i < args.length; i++)
      p.setObject(i + 1, args[i] instanceof Boolean b ? (b ? 1 : 0) : args[i]);
  }

  static List<Map<String, Object>> rows(Connection c, String sql, Object... args)
      throws SQLException {
    try (PreparedStatement p = c.prepareStatement(sql)) {
      bind(p, args);
      try (ResultSet r = p.executeQuery()) {
        List<Map<String, Object>> out = new ArrayList<>();
        int n = r.getMetaData().getColumnCount();
        while (r.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= n; i++) row.put(r.getMetaData().getColumnLabel(i), r.getObject(i));
          out.add(row);
        }
        return out;
      }
    }
  }

  static Map<String, Object> one(Connection c, String sql, Object... args) throws SQLException {
    var rows = rows(c, sql, args);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  static int update(Connection c, String sql, Object... args) throws SQLException {
    try (PreparedStatement p = c.prepareStatement(sql)) {
      bind(p, args);
      return p.executeUpdate();
    }
  }

  static long insert(Connection c, String sql, Object... args) throws SQLException {
    return ((Number) one(c, sql + " RETURNING id", args).get("id")).longValue();
  }

  Map<String, Object> backup() {
    return read(c -> {
      var tables = new LinkedHashMap<String, Object>();
      for (String table : TABLES) tables.put(table, rows(c, "SELECT * FROM " + table));
      return Map.of("format", "library-dashboard-v1", "created_at", Api.stamp(), "tables", tables);
    });
  }
}
