package org.openelisglobal.odoo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OdooSyncQueueServiceImpl extends BaseObjectServiceImpl<OdooSyncQueue, Long>
        implements OdooSyncQueueService {

    private static final Logger log = LogManager.getLogger(OdooSyncQueueServiceImpl.class);

    @Autowired
    private OdooSyncQueueDAO baseObjectDAO;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${org.openelisglobal.odoo.retry.processingTimeoutMinutes:1}")
    private long processingTimeoutMinutes;

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
    @Transactional
    public List<OdooSyncQueue> getPendingSyncRequests() {
        long timeoutMinutes = Math.max(processingTimeoutMinutes, 0);
        Instant cutoffInstant = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);
        Timestamp cutoffTimestamp = Timestamp.from(cutoffInstant);
        return baseObjectDAO.getPendingSyncRequests(cutoffTimestamp);
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
        OdooSyncQueue managedEntry = get(queueEntry.getId());
        managedEntry.setStatus(SyncStatus.PROCESSING);
        managedEntry.setLastRetryDate(new Timestamp(System.currentTimeMillis()));
        log.debug("Marked queue entry {} as processing", managedEntry.getId());
    }

    @Override
    @Transactional
    public void markAsCompleted(OdooSyncQueue queueEntry, Integer invoiceId) {
        OdooSyncQueue managedEntry = get(queueEntry.getId());
        managedEntry.markCompleted(invoiceId);
        log.info("Successfully synced queue entry {} for accession number: {} with Odoo invoice ID: {}",
                managedEntry.getId(), managedEntry.getAccessionNumber(), invoiceId);
    }

    @Override
    @Transactional
    public void markRetryFailed(OdooSyncQueue queueEntry, String errorMessage) {
        OdooSyncQueue managedEntry = get(queueEntry.getId());
        managedEntry.incrementRetryCount();
        managedEntry.setErrorMessage(errorMessage);

        if (managedEntry.hasExceededMaxRetries()) {
            managedEntry.markFailed(errorMessage);
            log.error("Queue entry {} for accession number {} has exceeded max retries. Marking as FAILED.",
                    managedEntry.getId(), managedEntry.getAccessionNumber());
        } else {
            managedEntry.setStatus(SyncStatus.PENDING);
            log.warn("Retry attempt {} failed for queue entry {} (accession: {}). Will retry again.",
                    managedEntry.getRetryCount(), managedEntry.getId(), managedEntry.getAccessionNumber());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getInvoiceDataAsMap(OdooSyncQueue queueEntry) throws JsonProcessingException {
        return objectMapper.readValue(queueEntry.getInvoiceData(), Map.class);
    }

    @Override
    @Transactional
    public void resetToPending(OdooSyncQueue queueEntry) {
        OdooSyncQueue managedEntry = get(queueEntry.getId());
        managedEntry.setStatus(SyncStatus.PENDING);
        managedEntry.setRetryCount(0);
        managedEntry.setErrorMessage(null);
    }

    @Override
    @Transactional
    public void updateInvoiceData(OdooSyncQueue queueEntry, Map<String, Object> invoiceData) {
        OdooSyncQueue managedEntry = get(queueEntry.getId());
        try {
            String invoiceDataJson = objectMapper.writeValueAsString(invoiceData);
            managedEntry.setInvoiceData(invoiceDataJson);
            log.debug("Updated invoice data for queue entry {}", managedEntry.getId());
        } catch (JsonProcessingException e) {
            String message = String.format("Failed to serialize invoice data for queue entry %d", queueEntry.getId());
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
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
