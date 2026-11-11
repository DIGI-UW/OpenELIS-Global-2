package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.springframework.stereotype.Component;

/** Loads WHONET's organism table, Organisms.txt, as WHONET publishes it. */
@Component
public class MicroWhonetOrganismConfigurationHandler extends AbstractCatalogCsvHandler {
    private final MicroWhonetOrganismService whonetOrganisms;

    public MicroWhonetOrganismConfigurationHandler(MicroWhonetOrganismService whonetOrganisms) {
        this.whonetOrganisms = whonetOrganisms;
    }

    @Override
    public String getDomainName() {
        return "microbiology-whonet-organisms";
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
        return 305;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "WHONET_ORG_CODE", "ORGANISM", "TAXONOMIC_STATUS" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        var outcomes = transaction.run(() -> whonetOrganisms.load(rows));
        for (int i = 0; i < rows.size(); i++) {
            summary.record(outcomes.get(i), getClass().getSimpleName(), rows.get(i).lineNumber());
        }
    }
}
