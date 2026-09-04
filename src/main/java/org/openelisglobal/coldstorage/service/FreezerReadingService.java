package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FreezerReadingService {

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    FreezerReading saveReading(Freezer freezer, OffsetDateTime recordedAt, BigDecimal temperature, BigDecimal humidity,
            FreezerReading.Status status, boolean transmissionOk, String errorMessage);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    Optional<FreezerReading> getLatestReading(Long freezerId);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<FreezerReading> getRecentReadings(Long freezerId, int limit);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<FreezerReading> getReadingsBetween(Long freezerId, OffsetDateTime start, OffsetDateTime end);
}
