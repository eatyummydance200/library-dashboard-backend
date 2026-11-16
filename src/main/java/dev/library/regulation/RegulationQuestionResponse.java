package dev.library.regulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record RegulationQuestionResponse(
    Long id,
    String username,
    String question,
    String outcome,
    String answer,
    List<String> sections,
    List<Extension> extensions,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("flagged_at") Instant flaggedAt,
    @JsonProperty("flag_note") String flagNote,
    @JsonProperty("resolved_at") Instant resolvedAt) {

  public record Extension(String number, String place) {}

  /** Shape of the sources column. */
  public record Sources(List<String> sections, List<Extension> extensions) {}

  static RegulationQuestionResponse from(RegulationQuestion q, Sources sources) {
    return new RegulationQuestionResponse(q.getId(), q.getUser().getUsername(), q.getQuestion(), q.getOutcome(),
        q.getAnswer(), sources.sections(), sources.extensions(), q.getCreatedAt(), q.getFlaggedAt(),
        q.getFlagNote(), q.getResolvedAt());
  }
}
