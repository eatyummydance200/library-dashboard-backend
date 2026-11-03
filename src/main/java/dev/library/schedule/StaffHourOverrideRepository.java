package dev.library.schedule;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffHourOverrideRepository extends JpaRepository<StaffHourOverride, Long> {
  List<StaffHourOverride> findAllByOrderByStaffIdAsc();
  Optional<StaffHourOverride> findByStaffId(Long staffId);
}
