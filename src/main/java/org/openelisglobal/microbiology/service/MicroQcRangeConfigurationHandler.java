package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroQcRange;
import org.springframework.stereotype.Component;

@Component
public class MicroQcRangeConfigurationHandler extends AbstractCatalogCsvHandler {

    private final MicroQcRangeService qcRanges;
    private final MicroAntibioticDAO antibiotics;
    private final MicroBreakpointStandardDAO standards;

    public MicroQcRangeConfigurationHandler(MicroQcRangeService qcRanges, MicroAntibioticDAO antibiotics,
            MicroBreakpointStandardDAO standards) {
        this.qcRanges = qcRanges;
        this.antibiotics = antibiotics;
        this.standards = standards;
    }

    @Override
    public String getDomainName() {
        return "microbiology-qc-ranges";
    }

    @Override
    public int getLoadOrder() {
        return 360;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "qc_strain", "agent", "method", "guideline", "version", "low", "high" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        int[] currentLine = { 0 };
        List<LoadedRow<String>> outcomes;
        try {
            outcomes = transaction.run(() -> {
                List<LoadedRow<String>> results = new ArrayList<>();
                for (CsvRow row : rows) {
                    currentLine[0] = row.lineNumber();
                    results.add(loadRow(row));
                }
                return results;
            });
        } finally {
            flushUnresolvedReferences(fileName, currentLine[0]);
        }
        for (int i = 0; i < outcomes.size(); i++) {
            summary.record(outcomes.get(i), getClass().getSimpleName(), rows.get(i).lineNumber());
        }
    }

    private LoadedRow<String> loadRow(CsvRow row) {
        String strain = required(row, "qc_strain");
        String code = required(row, "agent");
        MicroAntibiotic antibiotic = antibiotics.findByWhonetCodeIgnoreCase(code).orElseThrow(
                () -> new IllegalArgumentException("Line " + row.lineNumber() + ": unknown antibiotic " + code));
        MicroAstMethod method = method(row);
        String guideline = required(row, "guideline");
        String version = required(row, "version");
        MicroBreakpointStandard standard = standards.findByAuthorityAndVersion(guideline, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Line " + row.lineNumber() + ": unknown breakpoint standard " + guideline + " " + version));
        String diskContent = row.isBlank("disk_content") ? null : row.get("disk_content");
        BigDecimal low = decimal(row, "low");
        BigDecimal high = decimal(row, "high");

        Optional<MicroQcRange> existing = qcRanges.find(strain, antibiotic.getId(), method, diskContent,
                standard.getId());
        if (!qcRanges.upsert(strain, antibiotic.getId(), method, diskContent, standard.getId(), low, high)) {
            return LoadedRow.skipped("QC range unchanged");
        }
        String id = qcRanges.find(strain, antibiotic.getId(), method, diskContent, standard.getId())
                .map(MicroQcRange::getId).orElse(null);
        return existing.isPresent() ? LoadedRow.updated(id) : LoadedRow.created(id);
    }

    private static MicroAstMethod method(CsvRow row) {
        String value = required(row, "method");
        try {
            return MicroAstMethod.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Line " + row.lineNumber() + ": method must be MIC or ZONE, was '" + value + "'");
        }
    }

    private static BigDecimal decimal(CsvRow row, String column) {
        String value = required(row, column);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Line " + row.lineNumber() + ": " + column + " must be a number, was '" + value + "'");
        }
    }

    private static String required(CsvRow row, String column) {
        if (row.isBlank(column)) {
            throw new IllegalArgumentException("Line " + row.lineNumber() + ": " + column + " is required");
        }
        return row.get(column).trim();
    }
}
