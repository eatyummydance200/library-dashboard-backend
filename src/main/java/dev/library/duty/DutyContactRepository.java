package dev.library.duty;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DutyContactRepository extends JpaRepository<DutyContact, Long> {
  Optional<DutyContact> findByDay(LocalDate day);
}
