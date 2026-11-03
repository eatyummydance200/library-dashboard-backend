package dev.library.archive;

import dev.library.staff.Staff;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class ArchiveReassignmentRepository {
  private final EntityManager entityManager;

  ArchiveReassignmentRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  void replace(List<ArchiveAssignment> assignments, List<ArchiveTask> tasks, List<Staff> people, List<String> names) {
    // Preserve ids/completion history while avoiding UNIQUE conflicts during participant swaps.
    entityManager.flush();
    Long drawId = assignments.getFirst().getDraw().getId();
    execute("DELETE FROM archive_tasks WHERE assignment_id IN (SELECT id FROM archive_assignments WHERE draw_id=?1)", drawId);
    execute("DELETE FROM archive_assignments WHERE draw_id=?1", drawId);
    for (int i = 0; i < assignments.size(); i++) {
      var assignment = assignments.get(i);
      execute("INSERT INTO archive_assignments(id,draw_id,group_number,staff_id,staff_name) VALUES (?1,?2,?3,?4,?5)",
          assignment.getId(), drawId, assignment.getGroupNumber(), people.get(i).getId(), names.get(i));
    }
    for (var task : tasks)
      execute("INSERT INTO archive_tasks(id,assignment_id,position,newspaper,completed,completed_by,completed_at) VALUES (?1,?2,?3,?4,?5,?6,?7)",
          task.getId(), task.getAssignment().getId(), task.getPosition(), task.getNewspaper(), task.isCompleted(),
          task.getCompletedBy() == null ? null : task.getCompletedBy().getId(), task.getCompletedAt());
    entityManager.clear();
  }

  private void execute(String sql, Object... arguments) {
    var query = entityManager.createNativeQuery(sql);
    for (int i = 0; i < arguments.length; i++) query.setParameter(i + 1, arguments[i]);
    query.executeUpdate();
  }
}
