package dev.library.archive;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveDrawRepository extends JpaRepository<ArchiveDraw, Long> {
  @EntityGraph(attributePaths = "createdBy")
  Optional<ArchiveDraw> findByMonth(LocalDate month);
}
