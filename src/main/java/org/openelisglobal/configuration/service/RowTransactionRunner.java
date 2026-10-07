package org.openelisglobal.configuration.service;

import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs each catalog CSV row in its own transaction, so a row the database
 * rejects (an over-long column, a duplicate key) is rolled back alone and the
 * rows after it still load. Without a transaction manager, as in plain unit
 * tests, the work runs inline.
 * <p>
 * A dry run executes every row the same way but rolls its transaction back, so
 * the outcome a row would have (created, updated, skipped and why) is computed
 * against the current database without anything being kept. Each row is judged
 * on its own: a row that needs a record another row - or another file of the
 * same upload - would create is reported as it would go without it, and
 * applying the files in load order resolves it.
 */
public final class RowTransactionRunner {

    private final TransactionTemplate template;
    private final boolean dryRun;

    public RowTransactionRunner(PlatformTransactionManager transactionManager) {
        this(transactionManager, false);
    }

    public RowTransactionRunner(PlatformTransactionManager transactionManager, boolean dryRun) {
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
        return template.execute(status -> {
            T result = work.get();
            if (dryRun) {
                status.setRollbackOnly();
            }
            return result;
        });
    }
}
