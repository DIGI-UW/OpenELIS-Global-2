package org.openelisglobal.coldstorage.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.dao.FreezerReadingDAO;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
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
}
