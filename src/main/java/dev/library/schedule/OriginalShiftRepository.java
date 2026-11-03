package dev.library.schedule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OriginalShiftRepository extends JpaRepository<OriginalShift, Long> {
  List<OriginalShift> findByScheduleWeekIdOrderByDayAscStartAscIdAsc(Long weekId);
  void deleteByScheduleWeekId(Long weekId);
}
