package org.openelisglobal.configuration.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openelisglobal.common.util.CsvParsingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Common shape of the catalog attribute loaders (result components, ranges,
 * terminology, sample handling, reflex rules): a header addressed by column
 * name, rows handed to the subclass together with a per-row transaction runner
 * and a summary, unresolved references flushed after every unit of work, and
 * the file closed with a SUMMARY line. Subclasses decide the unit of work: one
 * row, or one group of rows that describes a single record.
 */
public abstract class AbstractCatalogCsvHandler implements DomainConfigurationHandler {

    protected static final String SYS_USER_ID = "1";

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    @Autowired(required = false)
    private UnresolvedReferenceService unresolvedReferenceService;

    private volatile CsvLoadSummary lastSummary;

    /** Column names every file of this domain must carry. */
    protected abstract String[] requiredColumns();

    /**
     * Loads the data rows. Implementations wrap each unit of work in
     * {@code transaction.run(...)}, record its outcome on the summary and call
     * {@link #flushUnresolvedReferences(String, int)} afterwards.
     */
    protected abstract void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary,
            String fileName);

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public boolean supportsDryRun() {
        return true;
    }

    @Override
    public CsvLoadSummary getLastSummary() {
        return lastSummary;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        processConfiguration(inputStream, fileName, false);
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName, boolean dryRun) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException(getDomainName() + " configuration file " + fileName + " is empty");
        }
        Map<String, Integer> columns = indexColumns(CsvParsingUtil.parseCsvLine(headerLine));
        for (String required : requiredColumns()) {
            if (!columns.containsKey(required.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        getDomainName() + " configuration file " + fileName + " must have a '" + required + "' column");
            }
        }

        List<CsvRow> rows = new ArrayList<>();
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            rows.add(new CsvRow(columns, CsvParsingUtil.parseCsvLine(line), lineNumber));
        }

        CsvLoadSummary summary = new CsvLoadSummary(getDomainName(), fileName);
        ImportRunContext.clearPending();
        load(rows, new RowTransactionRunner(transactionManager, dryRun), summary, fileName);
        summary.log(getClass().getSimpleName());
        lastSummary = summary;
    }

    /**
     * Persists the references the resolver could not satisfy while the last unit of
     * work ran. Called after the row transaction has ended, so the records survive
     * a rolled-back row and a dry run alike.
     */
    protected void flushUnresolvedReferences(String fileName, int lineNumber) {
        if (unresolvedReferenceService != null) {
            unresolvedReferenceService.recordPending(getDomainName(), fileName, lineNumber);
        }
    }

    private static Map<String, Integer> indexColumns(String[] headers) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i] == null ? "" : headers[i].trim().toLowerCase(Locale.ROOT);
            if (!header.isEmpty() && !columns.containsKey(header)) {
                columns.put(header, i);
            }
        }
        return columns;
    }
}
