package org.openelisglobal.dataexchange.externalcatalog.event;

import org.springframework.context.ApplicationEvent;

/**
 * Published after a catalog item (test or panel) has been successfully
 * upserted. Consumed by {@link CatalogCacheRefreshListener} which runs AFTER
 * the transaction commits — ensuring the in-memory cache is never refreshed
 * against a rolled-back state.
 */
public class CatalogUpsertedEvent extends ApplicationEvent {

    public CatalogUpsertedEvent(Object source) {
        super(source);
    }
}
