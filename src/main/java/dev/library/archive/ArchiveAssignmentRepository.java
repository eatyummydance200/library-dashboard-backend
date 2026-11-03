package dev.library.archive;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveAssignmentRepository extends JpaRepository<ArchiveAssignment, Long> {
  @EntityGraph(attributePaths = "staff")
  List<ArchiveAssignment> findByDrawIdOrderByGroupNumberAsc(Long drawId);
}
