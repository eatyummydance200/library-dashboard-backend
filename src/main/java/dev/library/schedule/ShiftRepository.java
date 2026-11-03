package dev.library.schedule;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
  List<Shift> findByDayGreaterThanEqualAndDayLessThanOrderByDayAscStartAscIdAsc(LocalDate start, LocalDate end);
  boolean existsByDayGreaterThanEqualAndDayLessThan(LocalDate start, LocalDate end);
  @Query("select count(s) > 0 from Shift s where s.day = :day and s.staff.id = :staffId and s.start < :end and s.end > :start and s.id <> :exclude")
  boolean overlaps(LocalDate day, Long staffId, int start, int end, Long exclude);
}
