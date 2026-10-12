package org.openelisglobal.dashboard.management.surveillance.service;

import java.math.BigDecimal;

public record SurveillanceMappingView(Long id, String programCode, String testId, String testName, String loinc,
        String metricLabel, String metricKey, String matchMode, String positiveResultCodes,
        String denominatorResultCodes, BigDecimal numericThreshold, int displayOrder, boolean active) {
}
