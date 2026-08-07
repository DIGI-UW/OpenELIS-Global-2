package org.openelisglobal.coldstorage.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.dao.FreezerReadingDAO;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.dto.FreezerExcursionData;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FreezerReadingServiceImpl implements FreezerReadingService {

    private final FreezerReadingDAO freezerReadingDAO;

    @PersistenceContext
    private EntityManager entityManager;

    public FreezerReadingServiceImpl(FreezerReadingDAO freezerReadingDAO) {
        this.freezerReadingDAO = freezerReadingDAO;
    }

    @Override
    @Transactional
    public FreezerReading saveReading(Freezer freezer, OffsetDateTime recordedAt, BigDecimal temperature,
            BigDecimal humidity, FreezerReading.Status status, boolean transmissionOk, String errorMessage) {
        // Get a managed reference to the freezer entity
        Freezer managedFreezer = entityManager.getReference(Freezer.class, freezer.getId());

        FreezerReading reading = new FreezerReading();
        reading.setFreezer(managedFreezer);
        reading.setRecordedAt(recordedAt);
        reading.setTemperatureCelsius(temperature);
        reading.setHumidityPercentage(humidity);
        reading.setStatus(status == null ? FreezerReading.Status.NORMAL : status);
        reading.setTransmissionOk(transmissionOk);
        reading.setErrorMessage(errorMessage);
        Long id = freezerReadingDAO.insert(reading);
        reading.setId(id);
        return reading;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FreezerReading> getLatestReading(Long freezerId) {
        return freezerReadingDAO.findLatestByFreezer(freezerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FreezerReading> getRecentReadings(Long freezerId, int limit) {
        return freezerReadingDAO.findRecentByFreezer(freezerId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FreezerReading> getReadingsBetween(Long freezerId, OffsetDateTime start, OffsetDateTime end) {
        return freezerReadingDAO.findByFreezerWithin(freezerId, start, end);
    }

    @Override
    @Transactional
    public int deleteReadingsOlderThan(OffsetDateTime cutoff) {
        return freezerReadingDAO.deleteOlderThan(cutoff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FreezerExcursionData> findExcursions(Freezer freezer, OffsetDateTime start, OffsetDateTime end) {
        List<FreezerReading> readings = freezerReadingDAO.findByFreezerWithin(freezer.getId(), start, end);
        List<FreezerExcursionData> excursions = new ArrayList<>();
        if (readings.isEmpty()) {
            return excursions;
        }

        List<FreezerReading> currentExcursion = new ArrayList<>();
        FreezerReading.Status currentStatus = null;

        for (FreezerReading reading : readings) {
            if (reading.getStatus() == FreezerReading.Status.WARNING
                    || reading.getStatus() == FreezerReading.Status.CRITICAL) {
                if (currentExcursion.isEmpty() || reading.getStatus() == currentStatus) {
                    currentExcursion.add(reading);
                    currentStatus = reading.getStatus();
                } else {
                    excursions.add(summarizeExcursion(currentExcursion, freezer));
                    currentExcursion = new ArrayList<>();
                    currentExcursion.add(reading);
                    currentStatus = reading.getStatus();
                }
            } else if (!currentExcursion.isEmpty()) {
                excursions.add(summarizeExcursion(currentExcursion, freezer));
                currentExcursion = new ArrayList<>();
                currentStatus = null;
            }
        }

        if (!currentExcursion.isEmpty()) {
            excursions.add(summarizeExcursion(currentExcursion, freezer));
        }

        return excursions;
    }

    private FreezerExcursionData summarizeExcursion(List<FreezerReading> excursionReadings, Freezer freezer) {
        FreezerExcursionData excursion = new FreezerExcursionData();
        if (excursionReadings.isEmpty()) {
            return excursion;
        }

        FreezerReading firstReading = excursionReadings.get(0);
        FreezerReading lastReading = excursionReadings.get(excursionReadings.size() - 1);

        excursion.setAlertId(firstReading.getId());
        excursion.setFreezerId(freezer.getId());
        excursion.setFreezerName(freezer.getName());
        excursion.setLocationName(freezer.getRoom());
        excursion.setStartTime(firstReading.getRecordedAt() != null ? firstReading.getRecordedAt().toString() : "");
        excursion.setEndTime(lastReading.getRecordedAt() != null ? lastReading.getRecordedAt().toString() : "");

        if (firstReading.getRecordedAt() != null && lastReading.getRecordedAt() != null) {
            excursion.setDurationSeconds(
                    Duration.between(firstReading.getRecordedAt(), lastReading.getRecordedAt()).getSeconds());
        }

        excursionReadings.stream().filter(r -> r.getTemperatureCelsius() != null)
                .min((r1, r2) -> r1.getTemperatureCelsius().compareTo(r2.getTemperatureCelsius()))
                .ifPresent(r -> excursion.setMinTemperature(r.getTemperatureCelsius()));

        excursionReadings.stream().filter(r -> r.getTemperatureCelsius() != null)
                .max((r1, r2) -> r1.getTemperatureCelsius().compareTo(r2.getTemperatureCelsius()))
                .ifPresent(r -> excursion.setMaxTemperature(r.getTemperatureCelsius()));

        excursion.setSeverity(firstReading.getStatus().name());
        excursion.setStatus("RESOLVED");

        return excursion;
    }
}
