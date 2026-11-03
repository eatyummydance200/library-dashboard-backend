package dev.library.schedule;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyHourOverrideRepository extends JpaRepository<WeeklyHourOverride, Long> {
  List<WeeklyHourOverride> findByScheduleWeekIdOrderByStaffIdAsc(Long weekId);
  Optional<WeeklyHourOverride> findByScheduleWeekIdAndStaffId(Long weekId, Long staffId);
}
