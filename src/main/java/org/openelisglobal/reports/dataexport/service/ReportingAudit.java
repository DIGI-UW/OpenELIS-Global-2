package org.openelisglobal.reports.dataexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openelisglobal.reports.dataexport.valueholder.ExportJob;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Metadata-only events in the existing OpenELIS application log. */
public final class ReportingAudit {
    private static final Logger LOG = LogManager.getLogger(ReportingAudit.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    public enum Action {
        SUBMITTED, STARTED, READY, FAILED, INTERRUPTED, RETRIED, CANCELLED, EXPIRED, DOWNLOAD_OPENED,
        DEFINITION_CREATED, DEFINITION_UPDATED, DEFINITION_DELETED, ACCESS_DENIED
    }

    private record Entry(Action action, String actor, String ownerId, String targetType, String targetId,
            String parentId, String timestamp) {
    }

    private ReportingAudit() {
    }

    public static void job(Action action, String actor, ExportJob job) {
        committed(action, actor, job.getOwnerId(), "job", job.getId(), job.getParentId());
    }

    public static void definition(Action action, String actor, String id) {
        committed(action, actor, null, "definition", id, null);
    }

    private static void committed(Action action, String actor, String owner, String type, String id, String parent) {
        // Capture identifiers now; a later lifecycle transition must not change this
        // event.
        Runnable write = () -> write(action, actor, owner, type, id, parent);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write.run();
                }
            });
        } else {
            write.run();
        }
    }

    public static void denied(String actor, String targetId) {
        // Denials must survive the rejected request's transaction rollback.
        write(Action.ACCESS_DENIED, actor, null, targetId == null ? "reporting" : "job", targetId, null);
    }

    private static String identifier(String value) {
        if (value == null)
            return null;
        return value.matches("[A-Za-z0-9_.:-]{1,128}") ? value : "invalid-identifier";
    }

    private static void write(Action action, String actor, String owner, String type, String id, String parent) {
        Entry entry = new Entry(action, identifier(actor), identifier(owner), type, identifier(id), identifier(parent),
                Instant.now().toString());
        try {
            LOG.info("REPORTING_AUDIT {}", JSON.writeValueAsString(entry));
        } catch (JsonProcessingException impossibleForScalarMetadata) {
            LOG.error("Unable to serialize reporting audit metadata", impossibleForScalarMetadata);
        }
    }
}
