package org.openelisglobal.configuration.service;

import jakarta.persistence.EntityManager;
import java.sql.Savepoint;
import java.util.function.Supplier;
import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs each catalog CSV row in its own transaction, so a row the database
 * rejects (an over-long column, a duplicate key) is rolled back alone and the
 * rows after it still load. Without a transaction manager, as in plain unit
 * tests, the work runs inline.
 * <p>
 * A dry run executes the whole file in one transaction that is rolled back when
 * the runner is closed, with each row behind its own savepoint. A later row
 * therefore sees what the earlier rows of the file would create or change, as
 * the apply does, and a row the database rejects is rolled back to its
 * savepoint alone (OGC-1466). Rows of another file in the same upload are not
 * seen; applying the files in load order resolves them.
 */
public final class RowTransactionRunner implements AutoCloseable {

    private final PlatformTransactionManager transactionManager;
    private final TransactionTemplate template;
    private final boolean dryRun;
    private TransactionStatus dryRunTransaction;

    public RowTransactionRunner(PlatformTransactionManager transactionManager) {
        this(transactionManager, false);
    }

    public RowTransactionRunner(PlatformTransactionManager transactionManager, boolean dryRun) {
        this.transactionManager = transactionManager;
        this.dryRun = dryRun;
        if (transactionManager == null) {
            template = null;
        } else {
            template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public <T> T run(Supplier<T> work) {
        if (template == null) {
            return work.get();
        }
        if (!dryRun) {
            return template.execute(status -> work.get());
        }
        if (dryRunTransaction == null) {
            DefaultTransactionDefinition definition = new DefaultTransactionDefinition(
                    TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            dryRunTransaction = transactionManager.getTransaction(definition);
        }
        EntityManager entityManager = entityManager();
        if (entityManager == null) {
            return template.execute(status -> {
                status.setRollbackOnly();
                return work.get();
            });
        }
        Session session = entityManager.unwrap(Session.class);
        Savepoint savepoint = session.doReturningWork(connection -> connection.setSavepoint());
        try {
            T result = work.get();
            session.flush();
            session.doWork(connection -> connection.releaseSavepoint(savepoint));
            return result;
        } catch (RuntimeException e) {
            session.doWork(connection -> connection.rollback(savepoint));
            session.clear();
            throw e;
        }
    }

    /** Rolls a dry run's transaction back; an apply has nothing left open. */
    @Override
    public void close() {
        if (dryRunTransaction != null && !dryRunTransaction.isCompleted()) {
            transactionManager.rollback(dryRunTransaction);
        }
        dryRunTransaction = null;
    }

    private EntityManager entityManager() {
        if (!(transactionManager instanceof JpaTransactionManager jpa) || jpa.getEntityManagerFactory() == null) {
            return null;
        }
        return EntityManagerFactoryUtils.getTransactionalEntityManager(jpa.getEntityManagerFactory());
    }
}
