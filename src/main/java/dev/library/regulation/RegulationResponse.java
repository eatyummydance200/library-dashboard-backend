package dev.library.regulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record RegulationResponse(String content, @JsonProperty("updated_at") Instant updatedAt, @JsonProperty("updated_by") String updatedBy) {
  static final RegulationResponse EMPTY = new RegulationResponse("", null, null);

  static RegulationResponse from(Regulation regulation) {
    return new RegulationResponse(regulation.getContent(), regulation.getUpdatedAt(), regulation.getUpdatedBy());
  }
}
