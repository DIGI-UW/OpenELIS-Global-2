package org.openelisglobal.microbiology.service;

import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.openelisglobal.configuration.service.*;
import org.springframework.stereotype.Component;

/**
 * Uses the same breakpoint validation and persistence as administrator imports.
 */
@Component
public class MicroBreakpointConfigurationHandler extends AbstractCatalogCsvHandler {
    private final MicroBreakpointImportService importer;

    public MicroBreakpointConfigurationHandler(MicroBreakpointImportService importer) {
        this.importer = importer;
    }

    public String getDomainName() {
        return "microbiology-breakpoints";
    }

    public int getLoadOrder() {
        return 330;
    }

    protected String[] requiredColumns() {
        return new String[] { "publisher", "version" };
    }

    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Breakpoint configuration is empty");
        }
        // Serialize the shared reader's rows for the existing CSV importer. Validation
        // belongs there so startup and administrator imports cannot diverge.
        var columns = rows.get(0).columns().entrySet().stream().sorted(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey).toList();
        StringBuilder csv = new StringBuilder(CSVFormat.DEFAULT.format(columns.toArray())).append('\n');
        for (CsvRow row : rows) {
            csv.append(CSVFormat.DEFAULT.format(columns.stream().map(row::get).toArray())).append('\n');
        }
        var result = transaction.run(() -> importer.applyConfiguration(csv.toString(), SYS_USER_ID));
        var unchangedRows = new java.util.HashSet<>(result.unchangedRowNumbers);
        for (int i = 0; i < rows.size(); i++) {
            var outcome = unchangedRows.contains(i + 2) ? LoadedRow.skipped("Unchanged breakpoint")
                    : LoadedRow.created(null);
            summary.record(outcome, getClass().getSimpleName(), rows.get(i).lineNumber());
        }
    }
}
