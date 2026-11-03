package dev.library.archive;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveTaskRepository extends JpaRepository<ArchiveTask, Long> {
  @EntityGraph(attributePaths = {"assignment", "assignment.staff", "completedBy"})
  List<ArchiveTask> findByAssignmentDrawIdOrderByPositionAsc(Long drawId);
}
