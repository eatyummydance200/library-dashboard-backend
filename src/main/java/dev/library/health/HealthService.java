package dev.library.health;

import jakarta.persistence.EntityManager;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthService {
  private final EntityManager entityManager;

  public HealthService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public Object health() {
    entityManager.createNativeQuery("SELECT 1").getSingleResult();
    return Map.of("status", "ok");
  }
}
