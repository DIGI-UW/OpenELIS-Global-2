package org.openelisglobal.odoo.service;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OdooRetryJob {

    private static final Logger log = LogManager.getLogger(OdooRetryJob.class);

    @Value("${org.openelisglobal.odoo.enabled:false}")
    private boolean odooEnabled;

    @Value("${org.openelisglobal.odoo.retry.enabled:true}")
    private boolean retryEnabled;

    @Autowired
    private OdooSyncQueueService odooSyncQueueService;

    @Autowired
    private OdooIntegrationService odooIntegrationService;

    @Autowired
    private SampleService sampleService;

    @Scheduled(fixedDelayString = "${org.openelisglobal.odoo.retry.interval.ms:300000}")
    public void processRetryQueue() {
        if (!odooEnabled) {
            return;
        }
        if (!retryEnabled) {
            return;
        }

        List<OdooSyncQueue> items = odooSyncQueueService.getItemsReadyForRetry();
        if (items.isEmpty()) {
            return;
        }

        log.info("Odoo retry job: processing {} item(s)", items.size());

        for (OdooSyncQueue item : items) {
            try {
                processItem(item);
            } catch (Exception e) {
                log.error("Unexpected error processing Odoo queue item for accession {}: {}", item.getAccessionNumber(),
                        e.getMessage(), e);
            }
        }
    }

    private void processItem(OdooSyncQueue item) {
        item = odooSyncQueueService.markInProgress(item);
        try {
            Sample sample = sampleService.getSampleByAccessionNumber(item.getAccessionNumber());
            if (sample == null) {
                odooSyncQueueService.markFailed(item, "Sample not found for accession: " + item.getAccessionNumber());
                return;
            }

            odooIntegrationService.createInvoiceForSample(sample);
            odooSyncQueueService.markCompleted(item);

        } catch (Exception e) {
            log.error("Odoo retry failed for accession {}: {}", item.getAccessionNumber(), e.getMessage(), e);
            odooSyncQueueService.markFailed(item, e.getMessage());
        }
    }
}
