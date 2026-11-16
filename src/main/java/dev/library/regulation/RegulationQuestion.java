package dev.library.regulation;

import dev.library.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "regulation_questions")
public class RegulationQuestion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "question", length = 500, nullable = false)
  private String question;

  @Column(name = "outcome", length = 16, nullable = false)
  private String outcome;

  @Column(name = "answer", length = 2000, nullable = false)
  private String answer;

  /** JSON {"sections":[...],"extensions":[{"number","place"}]}. */
  @Column(name = "sources", length = 4000, nullable = false)
  private String sources;

  @Column(name = "model", length = 64, nullable = false)
  private String model;

  @Column(name = "input_tokens", nullable = false)
  private int inputTokens;

  @Column(name = "output_tokens", nullable = false)
  private int outputTokens;

  @Column(name = "cache_read_tokens", nullable = false)
  private int cacheReadTokens;

  @Column(name = "cache_write_tokens", nullable = false)
  private int cacheWriteTokens;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "flagged_at")
  private Instant flaggedAt;

  @Column(name = "flag_note", length = 500)
  private String flagNote;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  protected RegulationQuestion() {}

  RegulationQuestion(User user, String question, ChatbotClient.Reply reply, String sources, Instant createdAt) {
    this.user = user;
    this.question = question;
    this.outcome = reply.outcome();
    this.answer = reply.answer();
    this.sources = sources;
    this.model = reply.model();
    this.inputTokens = reply.inputTokens();
    this.outputTokens = reply.outputTokens();
    this.cacheReadTokens = reply.cacheReadTokens();
    this.cacheWriteTokens = reply.cacheWriteTokens();
    this.createdAt = createdAt;
  }

  void flag(Instant at, String note) {
    flaggedAt = at;
    flagNote = note;
  }

  void unflag() {
    flaggedAt = null;
    flagNote = null;
    resolvedAt = null;
  }

  void resolve(Instant at) {
    resolvedAt = at;
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getQuestion() {
    return question;
  }

  public String getOutcome() {
    return outcome;
  }

  public String getAnswer() {
    return answer;
  }

  public String getSources() {
    return sources;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getFlaggedAt() {
    return flaggedAt;
  }

  public String getFlagNote() {
    return flagNote;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }
}
