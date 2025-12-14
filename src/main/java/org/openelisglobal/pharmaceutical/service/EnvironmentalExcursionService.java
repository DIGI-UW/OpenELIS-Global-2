package org.openelisglobal.pharmaceutical.service;

import java.util.List;
import org.openelisglobal.pharmaceutical.valueholder.EnvironmentalExcursionEvent;

public interface EnvironmentalExcursionService {

    EnvironmentalExcursionEvent get(Integer id);

    List<EnvironmentalExcursionEvent> getAll();

    EnvironmentalExcursionEvent save(EnvironmentalExcursionEvent event);

    EnvironmentalExcursionEvent update(EnvironmentalExcursionEvent event);

    void delete(Integer id);

    List<EnvironmentalExcursionEvent> findByDeviceId(Integer deviceId);

    List<EnvironmentalExcursionEvent> findByStatus(EnvironmentalExcursionEvent.ExcursionStatus status);

    List<EnvironmentalExcursionEvent> findActiveExcursions();

    List<EnvironmentalExcursionEvent> findByAlertType(EnvironmentalExcursionEvent.AlertType alertType);

    EnvironmentalExcursionEvent recordExcursion(Integer deviceId, EnvironmentalExcursionEvent.AlertType alertType,
            Double recordedValue, Double thresholdValue, String deviceLocation, String userId);

    EnvironmentalExcursionEvent acknowledgeExcursion(Integer excursionId, String acknowledgementNotes, String userId);

    EnvironmentalExcursionEvent resolveExcursion(Integer excursionId, String resolutionNotes, String userId);

    EnvironmentalExcursionEvent escalateExcursion(Integer excursionId, String escalationReason, String userId);

    List<EnvironmentalExcursionEvent> getUnacknowledgedExcursions();

    boolean hasActiveExcursions(Integer deviceId);

    java.util.List<java.util.Map<String, Object>> getAffectedSamples(Integer excursionId);
}
