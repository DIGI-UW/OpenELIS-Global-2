package org.openelisglobal.batchworkplan.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/** One QC run as a batch tile shows it (OGC-427 FR-QC-006/007). */
public record QcRunResponse(String id, Timestamp runDateTime, String performedBy, BigDecimal value, String unit,
        BigDecimal zScore, String resultStatus, String outcome, List<String> ruleCodes) {
}
