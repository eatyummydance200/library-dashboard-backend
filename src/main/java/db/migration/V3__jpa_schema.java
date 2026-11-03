package db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__jpa_schema extends BaseJavaMigration {
  private static final List<String> TABLES = List.of(
      "staff", "teachers", "users", "shifts", "original_schedule_weeks", "original_shifts", "weekly_hour_overrides", "issues", "monthly_tags", "archive_draws", "archive_assignments", "archive_tasks", "duty_contact");

  private byte[] schema() {
    try (var input = getClass().getResourceAsStream("/db/jpa-schema.sql")) {
      if (input == null) throw new IllegalStateException("Missing JPA migration schema");
      return new String(input.readAllBytes(), StandardCharsets.UTF_8)
          .replace("\r\n", "\n").replace("\r", "\n").getBytes(StandardCharsets.UTF_8);
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Integer getChecksum() {
    var checksum = new CRC32();
    checksum.update(schema());
    return (int) checksum.getValue();
  }

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (Statement statement = connection.createStatement()) {
      for (String part : new String(schema(), StandardCharsets.UTF_8).split(";")) {
        String sql = part.strip();
        if (sql.equals("-- VERIFY_COUNTS")) {
          for (String table : TABLES) {
            if (count(statement, table) != count(statement, table + "_jpa"))
              throw new IllegalStateException("Row count mismatch for " + table);
          }
        } else if (!sql.isEmpty()) {
          statement.execute(sql);
        }
      }
      for (String table : TABLES) {
        if (table.equals("duty_contact")) continue;
        long next;
        try (var result = statement.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM " + table)) {
          result.next();
          next = result.getLong(1);
        }
        statement.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + next);
      }
    }
  }

  private long count(Statement statement, String table) throws Exception {
    try (var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
      result.next();
      return result.getLong(1);
    }
  }
}
