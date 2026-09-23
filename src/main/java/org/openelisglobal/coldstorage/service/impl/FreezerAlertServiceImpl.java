package org.openelisglobal.coldstorage.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.coldstorage.event.FreezerHumidityThresholdViolatedEvent;
import org.openelisglobal.coldstorage.event.FreezerTemperatureThresholdViolatedEvent;
import org.openelisglobal.coldstorage.event.FreezerTransmissionFailedEvent;
import org.openelisglobal.coldstorage.event.FreezerTransmissionRecoveredEvent;
import org.openelisglobal.coldstorage.service.FreezerAlertService;
import org.openelisglobal.common.util.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class FreezerAlertServiceImpl implements FreezerAlertService {

    private static final Logger logger = LoggerFactory.getLogger(FreezerAlertServiceImpl.class);

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserContextHolder userContextHolder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Alert createFreezerTemperatureAlert(Long freezerId, BigDecimal temperature, BigDecimal thresholdValue,
            String thresholdType) {

        AlertSeverity severity = thresholdType.startsWith("CRITICAL") ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        String message = buildAlertMessage(temperature, thresholdValue, thresholdType);
        String contextDataJson = buildContextDataJson(temperature, thresholdValue, thresholdType);

        return alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", freezerId, severity, message,
                contextDataJson);
    }

    @Override
    @Transactional
    public Alert createFreezerHumidityAlert(Long freezerId, BigDecimal humidity, BigDecimal thresholdValue,
            String thresholdType) {

        AlertSeverity severity = thresholdType.startsWith("CRITICAL") ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        String message = buildHumidityAlertMessage(humidity, thresholdValue, thresholdType);
        String contextDataJson = buildHumidityContextDataJson(humidity, thresholdValue, thresholdType);

        return alertService.createAlert(AlertType.FREEZER_HUMIDITY, "Freezer", freezerId, severity, message,
                contextDataJson);
    }

    @Override
    @Transactional
    public Alert createFreezerOfflineAlert(Long freezerId, String errorMessage) {
        String message = buildOfflineAlertMessage(errorMessage);
        String contextDataJson = buildOfflineContextDataJson(errorMessage);

        return alertService.createAlert(AlertType.FREEZER_OFFLINE, "Freezer", freezerId, AlertSeverity.CRITICAL,
                message, contextDataJson);
    }

    // AFTER_COMMIT so a rolled-back ingest() never leaves behind a false alert.
    // REQUIRES_NEW because the committed transaction is still bound in that phase.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void handleFreezerTemperatureThresholdViolated(FreezerTemperatureThresholdViolatedEvent event) {
        try {
            createFreezerTemperatureAlert(event.getFreezerId(), event.getTemperature(), event.getThresholdValue(),
                    event.getThresholdType());
        } catch (Exception e) {
            logger.error("Error creating freezer temperature alert for freezer ID: {}", event.getFreezerId(), e);
        }
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void handleFreezerHumidityThresholdViolated(FreezerHumidityThresholdViolatedEvent event) {
        try {
            createFreezerHumidityAlert(event.getFreezerId(), event.getHumidity(), event.getThresholdValue(),
                    event.getThresholdType());
        } catch (Exception e) {
            logger.error("Error creating freezer humidity alert for freezer ID: {}", event.getFreezerId(), e);
        }
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void handleFreezerTransmissionFailed(FreezerTransmissionFailedEvent event) {
        try {
            createFreezerOfflineAlert(event.getFreezerId(), event.getErrorMessage());
        } catch (Exception e) {
            logger.error("Error creating freezer offline alert for freezer ID: {}", event.getFreezerId(), e);
        }
    }

    @Override
    @Transactional
    public void resolveFreezerOfflineAlerts(Long freezerId) {
        for (Alert alert : alertService.getAlertsByEntity("Freezer", freezerId)) {
            if (alert.getAlertType() != AlertType.FREEZER_OFFLINE) {
                continue;
            }
            if (alert.getStatus() != AlertStatus.OPEN && alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
                continue;
            }
            alertService.resolveAlert(alert.getId(), Integer.valueOf(userContextHolder.requireSysUserId()),
                    "Freezer resumed responding to monitoring polls");
        }
    }

    // AFTER_COMMIT for the same reason alert creation is: a rolled-back ingest()
    // must not resolve an alert whose recovery reading was never stored.
    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void handleFreezerTransmissionRecovered(FreezerTransmissionRecoveredEvent event) {
        try {
            resolveFreezerOfflineAlerts(event.getFreezerId());
        } catch (Exception e) {
            logger.error("Error resolving freezer offline alerts for freezer ID: {}", event.getFreezerId(), e);
        }
    }

    private String buildAlertMessage(BigDecimal temperature, BigDecimal thresholdValue, String thresholdType) {
        return String.format("Temperature threshold violated: Current %.1f°C, Threshold %.1f°C (%s)",
                temperature.doubleValue(), thresholdValue.doubleValue(), thresholdType);
    }

    private String buildOfflineAlertMessage(String errorMessage) {
        return "Freezer is not responding to monitoring polls"
                + (errorMessage != null && !errorMessage.isBlank() ? ": " + errorMessage : "");
    }

    private String buildOfflineContextDataJson(String errorMessage) {
        try {
            Map<String, Object> contextData = new HashMap<>();
            contextData.put("errorMessage", errorMessage);
            return objectMapper.writeValueAsString(contextData);
        } catch (Exception e) {
            logger.error("Error building offline-alert context data JSON", e);
            return "{}";
        }
    }

    private String buildContextDataJson(BigDecimal temperature, BigDecimal thresholdValue, String thresholdType) {
        try {
            Map<String, Object> contextData = new HashMap<>();
            contextData.put("temperature", temperature);
            contextData.put("thresholdValue", thresholdValue);
            contextData.put("thresholdType", thresholdType);
            return objectMapper.writeValueAsString(contextData);
        } catch (Exception e) {
            logger.error("Error building context data JSON", e);
            return "{}";
        }
    }

    private String buildHumidityAlertMessage(BigDecimal humidity, BigDecimal thresholdValue, String thresholdType) {
        return String.format("Humidity threshold violated: Current %.1f%%, Threshold %.1f%% (%s)",
                humidity.doubleValue(), thresholdValue.doubleValue(), thresholdType);
    }

    private String buildHumidityContextDataJson(BigDecimal humidity, BigDecimal thresholdValue, String thresholdType) {
        try {
            Map<String, Object> contextData = new HashMap<>();
            contextData.put("humidity", humidity);
            contextData.put("thresholdValue", thresholdValue);
            contextData.put("thresholdType", thresholdType);
            return objectMapper.writeValueAsString(contextData);
        } catch (Exception e) {
            logger.error("Error building humidity context data JSON", e);
            return "{}";
        }
    }
}
