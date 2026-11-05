package dev.library.cleaning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleaningDrawRepository extends JpaRepository<CleaningDraw, Long> {
  @EntityGraph(attributePaths = "createdBy")
  Optional<CleaningDraw> findByMonth(LocalDate month);

  @EntityGraph(attributePaths = "createdBy")
  List<CleaningDraw> findAllByOrderByMonthDesc();
}
