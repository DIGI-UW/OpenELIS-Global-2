package org.openelisglobal.dataexchange.externalcatalog.event;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Refreshes in-memory caches AFTER the catalog upsert transaction has
 * committed.
 *
 * Using {@code AFTER_COMMIT} guarantees the cache is never updated against a
 * rolled-back DB state — previously refreshCaches() ran inside the transaction
 * which could leave the cache out of sync if the transaction later rolled back.
 */
@Component
public class CatalogCacheRefreshListener {

    @Autowired
    private TestService testService;

    @Autowired
    private DisplayListService displayListService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCatalogUpserted(CatalogUpsertedEvent event) {
        try {
            testService.refreshTestNames();
            displayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
            displayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);
            displayListService.refreshList(DisplayListService.ListType.PANELS_ACTIVE);
            displayListService.refreshList(DisplayListService.ListType.PANELS_INACTIVE);
            displayListService.refreshList(DisplayListService.ListType.PANELS);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_BY_NAME);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
            typeOfSampleService.clearCache();
        } catch (Exception e) {
            // Cache refresh failure must never crash the caller — the DB write already
            // committed. Log it and move on; the cache will self-heal on next access.
            LogEvent.logError(this.getClass().getSimpleName(), "onCatalogUpserted",
                    "Cache refresh failed after catalog upsert: " + e.getMessage());
        }
    }
}
