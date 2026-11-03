package dev.library.common.db;

import jakarta.persistence.EntityManager;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class WriteLock {
  private final EntityManager entityManager;
  private final boolean postgres;
  private final ReentrantLock local = new ReentrantLock();

  public WriteLock(EntityManager entityManager, @Value("${spring.datasource.url}") String url) {
    this.entityManager = entityManager;
    this.postgres = url.startsWith("jdbc:postgresql:");
  }

  public void acquire() {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("A write transaction is required");
    if (postgres) {
      entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(714219801)").getSingleResult();
    } else {
      // Keep the H2 lock until commit/rollback, after the transactional method returns.
      local.lock();
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCompletion(int status) {
          local.unlock();
        }
      });
    }
  }
}
