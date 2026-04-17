package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ThresholdEvaluationService {

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    ThresholdProfile resolveActiveProfile(Freezer freezer, OffsetDateTime timestamp);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    FreezerReading.Status evaluateStatus(BigDecimal temperature, BigDecimal humidity, ThresholdProfile profile);
}
