package org.openelisglobal.pharmaceutical.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.dao.EnvironmentalExcursionEventDAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.valueholder.EnvironmentalExcursionEvent;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnvironmentalExcursionServiceImpl implements EnvironmentalExcursionService {

    @Autowired
    private EnvironmentalExcursionEventDAO environmentalExcursionEventDAO;

    @Autowired
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Override
    @Transactional(readOnly = true)
    public EnvironmentalExcursionEvent get(Integer id) {
        return environmentalExcursionEventDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> getAll() {
        return environmentalExcursionEventDAO.getAll();
    }

    @Override
    public EnvironmentalExcursionEvent save(EnvironmentalExcursionEvent event) {
        Integer id = environmentalExcursionEventDAO.insert(event);
        event.setId(id);
        return event;
    }

    @Override
    public EnvironmentalExcursionEvent update(EnvironmentalExcursionEvent event) {
        environmentalExcursionEventDAO.update(event);
        return event;
    }

    @Override
    public void delete(Integer id) {
        EnvironmentalExcursionEvent event = get(id);
        if (event != null) {
            environmentalExcursionEventDAO.delete(event);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> findByDeviceId(Integer deviceId) {
        return environmentalExcursionEventDAO.findByDeviceId(deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> findByStatus(EnvironmentalExcursionEvent.ExcursionStatus status) {
        return environmentalExcursionEventDAO.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> findActiveExcursions() {
        return environmentalExcursionEventDAO.findActiveExcursions();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> findByAlertType(EnvironmentalExcursionEvent.AlertType alertType) {
        return environmentalExcursionEventDAO.findByAlertType(alertType);
    }

    @Override
    public EnvironmentalExcursionEvent recordExcursion(Integer deviceId,
            EnvironmentalExcursionEvent.AlertType alertType,
            Double recordedValue, Double thresholdValue, String deviceLocation, String userId) {

        EnvironmentalExcursionEvent event = new EnvironmentalExcursionEvent();
        event.setDeviceId(deviceId);
        event.setAlertType(alertType);
        event.setRecordedValue(recordedValue);
        event.setThresholdValue(thresholdValue);
        event.setDeviceLocation(deviceLocation);
        event.setStatus(EnvironmentalExcursionEvent.ExcursionStatus.ACTIVE);
        event.setDetectedAt(new Timestamp(System.currentTimeMillis()));
        event.setSysUserId(userId);

        Integer id = environmentalExcursionEventDAO.insert(event);
        event.setId(id);

        return event;
    }

    @Override
    public EnvironmentalExcursionEvent acknowledgeExcursion(Integer excursionId,
            String acknowledgementNotes, String userId) {
        EnvironmentalExcursionEvent event = get(excursionId);
        if (event == null) {
            throw new LIMSRuntimeException("Excursion event not found: " + excursionId);
        }

        if (event.getStatus() != EnvironmentalExcursionEvent.ExcursionStatus.ACTIVE) {
            throw new LIMSRuntimeException("Excursion must be active to acknowledge");
        }

        event.setStatus(EnvironmentalExcursionEvent.ExcursionStatus.ACKNOWLEDGED);
        event.setAcknowledgedAt(new Timestamp(System.currentTimeMillis()));
        event.setAcknowledgedBy(userId);
        event.setNotes(acknowledgementNotes);
        event.setSysUserId(userId);
        environmentalExcursionEventDAO.update(event);

        return event;
    }

    @Override
    public EnvironmentalExcursionEvent resolveExcursion(Integer excursionId,
            String resolutionNotes, String userId) {
        EnvironmentalExcursionEvent event = get(excursionId);
        if (event == null) {
            throw new LIMSRuntimeException("Excursion event not found: " + excursionId);
        }

        if (event.getStatus() != EnvironmentalExcursionEvent.ExcursionStatus.ACKNOWLEDGED
                && event.getStatus() != EnvironmentalExcursionEvent.ExcursionStatus.ACTIVE) {
            throw new LIMSRuntimeException("Excursion must be active or acknowledged to resolve");
        }

        event.setStatus(EnvironmentalExcursionEvent.ExcursionStatus.RESOLVED);
        event.setResolvedAt(new Timestamp(System.currentTimeMillis()));
        event.setResolvedBy(userId);
        String existingNotes = event.getNotes();
        if (existingNotes != null && !existingNotes.isEmpty()) {
            event.setNotes(existingNotes + "\n\nResolution: " + resolutionNotes);
        } else {
            event.setNotes("Resolution: " + resolutionNotes);
        }
        event.setSysUserId(userId);
        environmentalExcursionEventDAO.update(event);

        return event;
    }

    @Override
    public EnvironmentalExcursionEvent escalateExcursion(Integer excursionId,
            String escalationReason, String userId) {
        EnvironmentalExcursionEvent event = get(excursionId);
        if (event == null) {
            throw new LIMSRuntimeException("Excursion event not found: " + excursionId);
        }

        event.setStatus(EnvironmentalExcursionEvent.ExcursionStatus.ESCALATED);
        String existingNotes = event.getNotes();
        if (existingNotes != null && !existingNotes.isEmpty()) {
            event.setNotes(existingNotes + "\n\nEscalation: " + escalationReason);
        } else {
            event.setNotes("Escalation: " + escalationReason);
        }
        event.setSysUserId(userId);
        environmentalExcursionEventDAO.update(event);

        return event;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> getUnacknowledgedExcursions() {
        return environmentalExcursionEventDAO.findByStatus(EnvironmentalExcursionEvent.ExcursionStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveExcursions(Integer deviceId) {
        List<EnvironmentalExcursionEvent> deviceExcursions = environmentalExcursionEventDAO.findByDeviceId(deviceId);
        return deviceExcursions.stream()
                .anyMatch(e -> e.getStatus() == EnvironmentalExcursionEvent.ExcursionStatus.ACTIVE
                        || e.getStatus() == EnvironmentalExcursionEvent.ExcursionStatus.ACKNOWLEDGED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAffectedSamples(Integer excursionId) {
        List<Map<String, Object>> result = new ArrayList<>();

        EnvironmentalExcursionEvent event = get(excursionId);
        if (event == null || event.getAffectedSampleIds() == null || event.getAffectedSampleIds().isEmpty()) {
            return result;
        }

        List<Integer> sampleIds = Arrays.stream(event.getAffectedSampleIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        for (Integer sampleId : sampleIds) {
            try {
                PharmaceuticalSample sample = pharmaceuticalSampleDAO.get(sampleId).orElse(null);
                if (sample != null) {
                    Map<String, Object> sampleInfo = new HashMap<>();
                    sampleInfo.put("sampleId", sample.getId());
                    sampleInfo.put("sampleName", sample.getSampleName());
                    sampleInfo.put("lotNumber", sample.getLotNumber());
                    sampleInfo.put("status", sample.getStatus() != null ? sample.getStatus().name() : null);
                    result.add(sampleInfo);
                }
            } catch (NumberFormatException e) {
                // Skip invalid sample IDs
            }
        }

        return result;
    }
}
