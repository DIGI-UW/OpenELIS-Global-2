package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.service.dto.FreezerExcursionData;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;

public interface FreezerReadingService {

    FreezerReading saveReading(Freezer freezer, OffsetDateTime recordedAt, BigDecimal temperature, BigDecimal humidity,
            BigDecimal temperature2, FreezerReading.Status status, boolean transmissionOk, String errorMessage);

    Optional<FreezerReading> getLatestReading(Long freezerId);

    List<FreezerReading> getRecentReadings(Long freezerId, int limit);

    List<FreezerReading> getReadingsBetween(Long freezerId, OffsetDateTime start, OffsetDateTime end);

    /**
     * Deletes readings older than the given retention cutoff. Used by the scheduled
     * retention cleanup job. Returns the number of rows deleted.
     */
    int deleteReadingsOlderThan(OffsetDateTime cutoff);

    /**
     * Groups a freezer's readings in [start, end] into consecutive WARNING/
     * CRITICAL excursion windows, returning one summary per excursion (start/end
     * time, min/max temperature, duration, severity). A run ends whenever a NORMAL
     * reading is seen or the severity changes.
     */
    List<FreezerExcursionData> findExcursions(Freezer freezer, OffsetDateTime start, OffsetDateTime end);
}
