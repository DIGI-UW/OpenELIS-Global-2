package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;

public interface FreezerReadingService {

    FreezerReading saveReading(Freezer freezer, OffsetDateTime recordedAt, BigDecimal temperature, BigDecimal humidity,
            FreezerReading.Status status, boolean transmissionOk, String errorMessage);

    Optional<FreezerReading> getLatestReading(Long freezerId);

    List<FreezerReading> getRecentReadings(Long freezerId, int limit);

    List<FreezerReading> getReadingsBetween(Long freezerId, OffsetDateTime start, OffsetDateTime end);
}
