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
 */
public final class RowTransactionRunner {

    private final TransactionTemplate template;

    public RowTransactionRunner(PlatformTransactionManager transactionManager) {
        if (transactionManager == null) {
            template = null;
        } else {
            template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
    }

    public <T> T run(Supplier<T> work) {
        if (template == null) {
            return work.get();
        }
        return template.execute(status -> work.get());
    }
}
