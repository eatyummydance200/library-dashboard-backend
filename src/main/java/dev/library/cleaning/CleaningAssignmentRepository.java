package dev.library.cleaning;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CleaningAssignmentRepository extends JpaRepository<CleaningAssignment, Long> {
  @EntityGraph(attributePaths = "staff")
  List<CleaningAssignment> findAllByOrderByStaffIdAsc();

  @EntityGraph(attributePaths = "staff")
  List<CleaningAssignment> findByDrawIdOrderByStaffIdAsc(Long drawId);

  // Bulk delete runs immediately, so new rows with the same (draw_id, staff_id) can be inserted afterwards.
  @Modifying
  @Query("delete from CleaningAssignment a where a.draw.id = :drawId")
  void deleteByDrawId(Long drawId);
}
