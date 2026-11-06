package dev.library.manual;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ManualResponse(String content, @JsonProperty("updated_at") Instant updatedAt, @JsonProperty("updated_by") String updatedBy) {
  static final ManualResponse EMPTY = new ManualResponse("", null, null);

  static ManualResponse from(Manual manual) {
    return new ManualResponse(manual.getContent(), manual.getUpdatedAt(), manual.getUpdatedBy());
  }
}
