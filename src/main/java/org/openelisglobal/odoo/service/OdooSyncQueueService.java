package org.openelisglobal.odoo.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openelisglobal.odoo.dao.OdooSyncQueueDAO;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue.SyncStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OdooSyncQueueService {

    private static final Logger log = LogManager.getLogger(OdooSyncQueueService.class);

    private static final int[] RETRY_DELAYS_MINUTES = {1, 5, 15};

    @Autowired
    private OdooSyncQueueDAO odooSyncQueueDAO;

    public OdooSyncQueue enqueue(String accessionNumber) {
        OdooSyncQueue existing = odooSyncQueueDAO.getActiveItemByAccessionNumber(accessionNumber);
        if (existing != null) {
            log.info("Odoo sync for accession {} is already queued (id={}, status={}). Skipping duplicate enqueue.",
                    accessionNumber, existing.getId(), existing.getStatus());
            return existing;
        }

        OdooSyncQueue item = new OdooSyncQueue();
        item.setAccessionNumber(accessionNumber);
        item.setStatus(SyncStatus.PENDING);
        item.setRetryCount(0);
        item.setMaxRetries(3);
        item.setCreatedAt(Timestamp.from(Instant.now()));
        item.setNextRetryTime(Timestamp.from(Instant.now()));
        odooSyncQueueDAO.insert(item);
        log.info("Enqueued Odoo sync for accession: {}", accessionNumber);
        return item;
    }

    public void markInProgress(OdooSyncQueue item) {
        item.setStatus(SyncStatus.IN_PROGRESS);
        odooSyncQueueDAO.update(item);
    }

    public void markCompleted(OdooSyncQueue item) {
        item.setStatus(SyncStatus.COMPLETED);
        item.setCompletedAt(Timestamp.from(Instant.now()));
        item.setLastError(null);
        odooSyncQueueDAO.update(item);
        log.info("Odoo sync completed for accession: {}", item.getAccessionNumber());
    }

    public void markFailed(OdooSyncQueue item, String errorMessage) {
        int newRetryCount = item.getRetryCount() + 1;
        item.setRetryCount(newRetryCount);
        item.setLastError(errorMessage);

        if (item.hasExceededMaxRetries()) {
            item.setStatus(SyncStatus.FAILED);
            log.warn("Odoo sync permanently FAILED for accession: {} after {} retries. Manual review required.",
                    item.getAccessionNumber(), newRetryCount);
        } else {
            item.setStatus(SyncStatus.PENDING);
            int delayMinutes = RETRY_DELAYS_MINUTES[Math.min(newRetryCount - 1, RETRY_DELAYS_MINUTES.length - 1)];
            Timestamp nextRetry = Timestamp.from(Instant.now().plusSeconds(delayMinutes * 60L));
            item.setNextRetryTime(nextRetry);
            log.warn("Odoo sync failed for accession: {}. Retry {} of {} scheduled in {} minutes.",
                    item.getAccessionNumber(), newRetryCount, item.getMaxRetries(), delayMinutes);
        }
        odooSyncQueueDAO.update(item);
    }

    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getItemsReadyForRetry() {
        return odooSyncQueueDAO.getItemsReadyForRetry();
    }

    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getFailedItems() {
        return odooSyncQueueDAO.getByStatus(SyncStatus.FAILED);
    }
}
