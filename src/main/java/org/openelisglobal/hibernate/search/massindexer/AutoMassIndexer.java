package org.openelisglobal.hibernate.search.massindexer;

import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class AutoMassIndexer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    MassIndexerService massIndexerService;

    private static boolean alreadyIndexed = false;

    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (alreadyIndexed) {
            return;
        }
        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "onApplicationEvent",
                    "Started Rebuilding Lucene Indexes ");
            massIndexerService.reindex();
            alreadyIndexed = true;
            LogEvent.logInfo(this.getClass().getSimpleName(), "onApplicationEvent",
                    "Finished Rebuilding Lucene Indexes ");
        } catch (Exception e) {
            LogEvent.logDebug(e);
        }

    }
}
