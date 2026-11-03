package dev.library.tagging;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyTagRepository extends JpaRepository<MonthlyTag, Long> {
  @EntityGraph(attributePaths = "staff")
  List<MonthlyTag> findByYearOrderByStaffIdAscMonthAsc(int year);
  Optional<MonthlyTag> findByStaffIdAndYearAndMonth(Long staffId, int year, int month);
}
