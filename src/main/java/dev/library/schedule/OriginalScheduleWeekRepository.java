package dev.library.schedule;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OriginalScheduleWeekRepository extends JpaRepository<OriginalScheduleWeek, Long> {
  Optional<OriginalScheduleWeek> findByWeek(LocalDate week);
}
