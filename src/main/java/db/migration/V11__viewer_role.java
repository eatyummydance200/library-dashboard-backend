package db.migration;

import java.sql.Statement;
import java.util.ArrayList;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

// users.role CHECK에 VIEWER 추가. V3가 이름 없이 만든 제약이라 H2·PostgreSQL에서 이름이 달라 조회해서 지운다.
public class V11__viewer_role extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    try (Statement statement = context.getConnection().createStatement()) {
      var names = new ArrayList<String>();
      try (var rows = statement.executeQuery(
          "SELECT tc.constraint_name FROM information_schema.table_constraints tc"
              + " JOIN information_schema.check_constraints cc"
              + " ON cc.constraint_schema = tc.constraint_schema AND cc.constraint_name = tc.constraint_name"
              + " WHERE LOWER(tc.table_name) = 'users' AND tc.constraint_type = 'CHECK'"
              + " AND cc.check_clause LIKE '%WORKER%'")) {
        while (rows.next()) names.add(rows.getString(1));
      }
      if (names.isEmpty()) throw new IllegalStateException("users.role CHECK constraint not found");
      for (String name : names)
        statement.execute("ALTER TABLE users DROP CONSTRAINT \"" + name.replace("\"", "\"\"") + "\"");
      statement.execute(
          "ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN','WORKER','VIEWER'))");
    }
  }
}
