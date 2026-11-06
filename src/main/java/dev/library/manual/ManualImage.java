package dev.library.manual;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "manual_images")
public class ManualImage {
  @Id
  @Column(name = "name", length = 255)
  private String name;

  @Column(name = "content_type", length = 32, nullable = false)
  private String contentType;

  @Column(name = "data", nullable = false)
  private byte[] data;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ManualImage() {}

  ManualImage(String name, String contentType, byte[] data, Instant createdAt) {
    this.name = name;
    this.contentType = contentType;
    this.data = data;
    this.createdAt = createdAt;
  }

  public String getName() {
    return name;
  }

  public String getContentType() {
    return contentType;
  }

  public byte[] getData() {
    return data;
  }
}
