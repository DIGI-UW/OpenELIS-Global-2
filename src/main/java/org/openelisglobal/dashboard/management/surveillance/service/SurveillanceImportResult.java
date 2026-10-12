package org.openelisglobal.dashboard.management.surveillance.service;

import java.util.List;

public record SurveillanceImportResult(int rowsAdded, int rowsUpdated, List<SkippedRow> rowsSkipped) {

    public record SkippedRow(int row, String reason) {
    }
}
