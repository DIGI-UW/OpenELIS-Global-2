package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.springframework.stereotype.Component;

/** Loads WHONET's breakpoint table, Breakpoints.txt, as WHONET publishes it. */
@Component
public class MicroWhonetBreakpointConfigurationHandler extends AbstractCatalogCsvHandler {
    private final MicroWhonetBreakpointService whonetBreakpoints;
    private final MicroWhonetBreakpointEditionService editions;

    public MicroWhonetBreakpointConfigurationHandler(MicroWhonetBreakpointService whonetBreakpoints,
            MicroWhonetBreakpointEditionService editions) {
        this.whonetBreakpoints = whonetBreakpoints;
        this.editions = editions;
    }

    @Override
    public String getDomainName() {
        return "microbiology-whonet-breakpoints";
    }

    @Override
    public String getFileExtension() {
        return "txt";
    }

    @Override
    protected boolean tabSeparated() {
        return true;
    }

    @Override
    public int getLoadOrder() {
        return 307;
    }

    /**
     * The table comes with the reference package, so Import Catalog does not offer
     * it.
     */
    @Override
    public boolean supportsDryRun() {
        return false;
    }

    @Override
    protected String[] requiredColumns() {
        return MicroWhonetBreakpointService.REQUIRED;
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        List<String> unknownAntibiotics = new ArrayList<>();
        var outcomes = transaction.run(() -> {
            var loaded = whonetBreakpoints.load(rows);
            unknownAntibiotics.addAll(editions.publish(fileName));
            return loaded;
        });
        if (!unknownAntibiotics.isEmpty()) {
            LogEvent.logWarn(getClass().getSimpleName(), "load",
                    fileName + ": rules left out of the new editions, no such antibiotic: " + unknownAntibiotics);
        }
        for (int i = 0; i < rows.size(); i++) {
            summary.record(outcomes.get(i), getClass().getSimpleName(), rows.get(i).lineNumber());
        }
    }
}
