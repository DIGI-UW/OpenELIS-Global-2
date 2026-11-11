package org.openelisglobal.containertype.service;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.ConfigurationLoadedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Imports the environmental container dictionary after each configuration load
 * (OGC-1611 FR-G4). Liquibase runs before the configuration loaders, so on a
 * fresh install the dictionary does not exist yet when the migration runs.
 */
@Component
public class ContainerTypeConfigurationListener {

    @Autowired
    private ContainerTypeService containerTypeService;

    @EventListener
    public void onConfigurationLoaded(ConfigurationLoadedEvent event) {
        try {
            int imported = containerTypeService.importEnvironmentalContainers();
            if (imported > 0) {
                LogEvent.logInfo(getClass().getSimpleName(), "onConfigurationLoaded",
                        imported + " environmental container types imported from the container dictionary");
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }
}
