package dev.library.inquiry;

import dev.library.user.User;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inquiries")
public class Inquiry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "category", length = 16, nullable = false)
  private String category;

  @Column(name = "title", length = 80, nullable = false)
  private String title;

  @Column(name = "body", length = 2000, nullable = false)
  private String body;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Inquiry() {}

  public Long getId() {
    return id;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public User getAuthor() {
    return author;
  }

  public void setAuthor(User author) {
    this.author = author;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
