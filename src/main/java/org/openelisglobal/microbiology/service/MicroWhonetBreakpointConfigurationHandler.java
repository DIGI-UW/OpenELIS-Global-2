package org.openelisglobal.microbiology.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.springframework.stereotype.Component;

@Component
public class MicroWhonetBreakpointConfigurationHandler extends AbstractCatalogCsvHandler {
    private final MicroWhonetBreakpointService whonetBreakpoints;
    private final MicroWhonetBreakpointEditionService editions;
    private final MicroBreakpointService breakpoints;
    private final MicroBreakpointAdminService breakpointAdmin;

    public MicroWhonetBreakpointConfigurationHandler(MicroWhonetBreakpointService whonetBreakpoints,
            MicroWhonetBreakpointEditionService editions, MicroBreakpointService breakpoints,
            MicroBreakpointAdminService breakpointAdmin) {
        this.whonetBreakpoints = whonetBreakpoints;
        this.editions = editions;
        this.breakpoints = breakpoints;
        this.breakpointAdmin = breakpointAdmin;
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

    // Ships with the reference package, so Import Catalog doesn't offer it.
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
            activateTheDefaultEditionWhenNoneIsActive();
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

    private void activateTheDefaultEditionWhenNoneIsActive() {
        String defaultEdition = breakpoints.getDefaultStandard().standardId;
        if (defaultEdition != null && breakpoints.getActiveStandards().isEmpty()) {
            breakpointAdmin.activate(defaultEdition, Date.valueOf(LocalDate.now()), "1");
        }
    }
}
