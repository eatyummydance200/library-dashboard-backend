package dev.library.regulation;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RegulationQuestionRepository extends JpaRepository<RegulationQuestion, Long> {
  long countByUserIdAndCreatedAtGreaterThanEqual(Long userId, Instant from);

  long countByCreatedAtGreaterThanEqual(Instant from);

  /** One row: count, input, output, cache read, cache write. */
  @Query("SELECT COUNT(q), COALESCE(SUM(q.inputTokens), 0), COALESCE(SUM(q.outputTokens), 0),"
      + " COALESCE(SUM(q.cacheReadTokens), 0), COALESCE(SUM(q.cacheWriteTokens), 0)"
      + " FROM RegulationQuestion q WHERE q.createdAt >= :from")
  List<Object[]> usageSince(Instant from);

  /** filter: flagged | not_found (not_found or refused) | all. Unresolved first, then newest. */
  @Query("SELECT q FROM RegulationQuestion q JOIN FETCH q.user WHERE :filter = 'all'"
      + " OR (:filter = 'flagged' AND q.flaggedAt IS NOT NULL)"
      + " OR (:filter = 'not_found' AND q.outcome IN ('not_found', 'refused'))"
      + " ORDER BY CASE WHEN q.resolvedAt IS NULL THEN 0 ELSE 1 END, q.createdAt DESC, q.id DESC")
  List<RegulationQuestion> search(String filter, Limit limit);
}
