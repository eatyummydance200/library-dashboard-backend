package dev.library.schedule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmedShiftRepository extends JpaRepository<ConfirmedShift, Long> {
  List<ConfirmedShift> findAllByOrderByWeekdayAscStartAscIdAsc();
}
