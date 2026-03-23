package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;

public interface ThresholdEvaluationService {

    ThresholdProfile resolveActiveProfile(Freezer freezer, OffsetDateTime timestamp);

    FreezerReading.Status evaluateStatus(BigDecimal temperature, BigDecimal humidity, ThresholdProfile profile);
}
