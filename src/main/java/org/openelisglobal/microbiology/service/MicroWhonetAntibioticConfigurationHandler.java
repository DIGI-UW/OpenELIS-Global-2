package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.springframework.stereotype.Component;

/** Loads WHONET's antibiotic table, Antibiotics.txt, as WHONET publishes it. */
@Component
public class MicroWhonetAntibioticConfigurationHandler extends AbstractCatalogCsvHandler {
    private final MicroWhonetAntibioticService whonetAntibiotics;

    public MicroWhonetAntibioticConfigurationHandler(MicroWhonetAntibioticService whonetAntibiotics) {
        this.whonetAntibiotics = whonetAntibiotics;
    }

    @Override
    public String getDomainName() {
        return "microbiology-whonet-antibiotics";
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
        return 306;
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
        return new String[] { "WHONET_ABX_CODE", "ANTIBIOTIC" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        var outcomes = transaction.run(() -> whonetAntibiotics.load(rows));
        for (int i = 0; i < rows.size(); i++) {
            summary.record(outcomes.get(i), getClass().getSimpleName(), rows.get(i).lineNumber());
        }
    }
}
