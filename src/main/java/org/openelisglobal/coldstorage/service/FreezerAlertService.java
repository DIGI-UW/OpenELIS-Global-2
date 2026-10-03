package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.coldstorage.event.FreezerHumidityThresholdViolatedEvent;
import org.openelisglobal.coldstorage.event.FreezerTemperatureThresholdViolatedEvent;
import org.openelisglobal.coldstorage.event.FreezerTransmissionFailedEvent;
import org.openelisglobal.coldstorage.event.FreezerTransmissionRecoveredEvent;

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
     * Raises a FREEZER_HUMIDITY alert - alertEntityType="Freezer" - for a reading
     * whose relative humidity is outside its profile's band. Severity follows
     * thresholdType, as for temperature.
     */
    Alert createFreezerHumidityAlert(Long freezerId, BigDecimal humidity, BigDecimal thresholdValue,
            String thresholdType);

    /**
     * Create a freezer-offline alert (dead-man's switch) for a device that failed
     * to respond to Modbus polling.
     *
     * @param freezerId    Freezer ID
     * @param errorMessage The poll failure reason
     * @return Created alert
     */
    Alert createFreezerOfflineAlert(Long freezerId, String errorMessage);

    /**
     * Event listener for FreezerTemperatureThresholdViolatedEvent.
     *
     * @param event The temperature threshold violation event
     */
    void handleFreezerTemperatureThresholdViolated(FreezerTemperatureThresholdViolatedEvent event);

    /**
     * Event listener that turns a humidity threshold violation into an alert. Must
     * be declared here, not only on the implementation: the bean is exposed through
     * a JDK interface proxy, and Spring refuses an @EventListener it cannot see on
     * the proxy's interfaces.
     */
    void handleFreezerHumidityThresholdViolated(FreezerHumidityThresholdViolatedEvent event);

    /**
     * Event listener for FreezerTransmissionFailedEvent.
     *
     * @param event The transmission failure event
     */
    void handleFreezerTransmissionFailed(FreezerTransmissionFailedEvent event);

    /**
     * Resolve every open or acknowledged offline alert for a freezer that has
     * resumed responding, so a later outage raises a fresh alert of its own.
     *
     * @param freezerId Freezer ID
     */
    void resolveFreezerOfflineAlerts(Long freezerId);

    /**
     * Event listener for FreezerTransmissionRecoveredEvent.
     *
     * @param event The transmission recovery event
     */
    void handleFreezerTransmissionRecovered(FreezerTransmissionRecoveredEvent event);
}
