package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.coldstorage.event.FreezerTemperatureThresholdViolatedEvent;

public interface FreezerAlertService {

    /**
     * Create freezer temperature alert.
     *
     * <p>
     * Wrapper that calls AlertService.createAlert with: -
     * AlertType.FREEZER_TEMPERATURE - alertEntityType="Freezer" - Builds
     * context_data JSON with temperature details
     *
     * @param freezerId      Freezer ID
     * @param temperature    Current temperature reading
     * @param thresholdValue Threshold value that was violated
     * @param thresholdType  Type of threshold (CRITICAL_HIGH, WARNING_HIGH, etc.)
     * @return Created alert
     */
    Alert createFreezerTemperatureAlert(Long freezerId, BigDecimal temperature, BigDecimal thresholdValue,
            String thresholdType);

    /**
     * Event listener for FreezerTemperatureThresholdViolatedEvent.
     *
     * @param event The temperature threshold violation event
     */
    void handleFreezerTemperatureThresholdViolated(FreezerTemperatureThresholdViolatedEvent event);
}
