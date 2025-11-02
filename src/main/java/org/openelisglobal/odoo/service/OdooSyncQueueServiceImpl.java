package org.openelisglobal.odoo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.odoo.dao.OdooSyncQueueDAO;
import org.openelisglobal.odoo.entity.OdooSyncQueue;
import org.openelisglobal.odoo.entity.OdooSyncQueue.SyncStatus;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing Odoo sync queue operations. Handles
 * queueing, retry logic, and status management for failed Odoo syncs.
 */
@Service
public class OdooSyncQueueServiceImpl extends BaseObjectServiceImpl<OdooSyncQueue, Long>
        implements OdooSyncQueueService {

    private static final Logger log = LogManager.getLogger(OdooSyncQueueServiceImpl.class);

    @Autowired
    private OdooSyncQueueDAO baseObjectDAO;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OdooSyncQueueServiceImpl() {
        super(OdooSyncQueue.class);
    }

    @Override
    protected OdooSyncQueueDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public OdooSyncQueue queueFailedSync(SamplePatientUpdateData updateData, Map<String, Object> invoiceData,
            String errorMessage) {
        try {
            OdooSyncQueue queueEntry = new OdooSyncQueue();
            queueEntry.setAccessionNumber(updateData.getAccessionNumber());

            if (updateData.getSample() != null) {
                queueEntry.setSampleId(updateData.getSample().getId());
            }

            // Serialize invoice data to JSON
            String invoiceDataJson = objectMapper.writeValueAsString(invoiceData);
            queueEntry.setInvoiceData(invoiceDataJson);

            queueEntry.setStatus(SyncStatus.PENDING);
            queueEntry.setErrorMessage(errorMessage);
            queueEntry.setCreatedDate(new Timestamp(System.currentTimeMillis()));

            Long id = insert(queueEntry);
            log.info("Queued failed Odoo sync for accession number: {} with queue ID: {}",
                    updateData.getAccessionNumber(), id);

            return get(id);
        } catch (JsonProcessingException e) {
            log.error("Error serializing invoice data for accession number: {}", updateData.getAccessionNumber(), e);
            throw new RuntimeException("Failed to serialize invoice data", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getPendingSyncRequests() {
        return baseObjectDAO.getPendingSyncRequests();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getByStatus(SyncStatus status) {
        return baseObjectDAO.getByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getByAccessionNumber(String accessionNumber) {
        return baseObjectDAO.getByAccessionNumber(accessionNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getFailedSyncRequests() {
        return baseObjectDAO.getFailedSyncRequests();
    }

    @Override
    @Transactional
    public void markAsProcessing(OdooSyncQueue queueEntry) {
        queueEntry.setStatus(SyncStatus.PROCESSING);
        queueEntry.setLastRetryDate(new Timestamp(System.currentTimeMillis()));
        update(queueEntry);
        log.debug("Marked queue entry {} as processing", queueEntry.getId());
    }

    @Override
    @Transactional
    public void markAsCompleted(OdooSyncQueue queueEntry, Integer invoiceId) {
        queueEntry.markCompleted(invoiceId);
        update(queueEntry);
        log.info("Successfully synced queue entry {} for accession number: {} with Odoo invoice ID: {}",
                queueEntry.getId(), queueEntry.getAccessionNumber(), invoiceId);
    }

    @Override
    @Transactional
    public void markRetryFailed(OdooSyncQueue queueEntry, String errorMessage) {
        queueEntry.incrementRetryCount();
        queueEntry.setErrorMessage(errorMessage);

        if (queueEntry.hasExceededMaxRetries()) {
            queueEntry.markFailed(errorMessage);
            log.error("Queue entry {} for accession number {} has exceeded max retries. Marking as FAILED.",
                    queueEntry.getId(), queueEntry.getAccessionNumber());
        } else {
            queueEntry.setStatus(SyncStatus.PENDING);
            log.warn("Retry attempt {} failed for queue entry {} (accession: {}). Will retry again.",
                    queueEntry.getRetryCount(), queueEntry.getId(), queueEntry.getAccessionNumber());
        }

        update(queueEntry);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getInvoiceDataAsMap(OdooSyncQueue queueEntry) throws JsonProcessingException {
        return objectMapper.readValue(queueEntry.getInvoiceData(), Map.class);
    }

    @Override
    @Transactional
    public void resetToPending(OdooSyncQueue queueEntry) {
        queueEntry.setStatus(SyncStatus.PENDING);
        queueEntry.setRetryCount(0);
        queueEntry.setErrorMessage(null);
        update(queueEntry);
        log.info("Reset queue entry {} to PENDING for manual retry", queueEntry.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public int getPendingCount() {
        return getPendingSyncRequests().size();
    }

    @Override
    @Transactional(readOnly = true)
    public int getFailedCount() {
        return getFailedSyncRequests().size();
    }
}
