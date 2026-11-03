package dev.library.duty;
import com.fasterxml.jackson.annotation.JsonProperty;
public record DutyResponse(@JsonProperty("teacher_id") Long teacherId, String teacher, String extension, String phone) {}
