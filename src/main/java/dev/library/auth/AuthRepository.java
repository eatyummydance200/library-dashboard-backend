package dev.library.auth;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
class AuthRepository {
  private final EntityManager entityManager;

  AuthRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  void deleteExpiredAttempts(long cutoff) {
    entityManager.createNativeQuery("DELETE FROM login_attempts WHERE attempted_at < :cutoff")
        .setParameter("cutoff", cutoff).executeUpdate();
  }

  long countAttempts(String username) {
    return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM login_attempts WHERE username = :username")
        .setParameter("username", username).getSingleResult()).longValue();
  }

  void insertAttempt(String username, long now) {
    entityManager.createNativeQuery("INSERT INTO login_attempts(username,attempted_at) VALUES (:username,:now)")
        .setParameter("username", username).setParameter("now", now).executeUpdate();
  }

  void deleteAttempts(String username) {
    entityManager.createNativeQuery("DELETE FROM login_attempts WHERE username = :username")
        .setParameter("username", username).executeUpdate();
  }
}
