package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.configuration.service.*;
import org.springframework.stereotype.Component;

@Component
public class MicroAntimicrobialConfigurationHandler extends AbstractCatalogCsvHandler {
    private final MicroReferenceConceptService concepts;

    public MicroAntimicrobialConfigurationHandler(MicroReferenceConceptService concepts) {
        this.concepts = concepts;
    }

    public String getDomainName() {
        return "microbiology-antimicrobials";
    }

    public int getLoadOrder() {
        return 320;
    }

    protected String[] requiredColumns() {
        return new String[] { "source", "code", "displayName", "whonetCode" };
    }

    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        // A failed catalog remains retryable instead of being checksummed as loaded.
        var outcomes = transaction.run(() -> {
            concepts.validateIdentities(rows);
            return rows.stream().map(concepts::loadAntibiotic).toList();
        });
        for (int i = 0; i < rows.size(); i++) {
            summary.record(outcomes.get(i), getClass().getSimpleName(), rows.get(i).lineNumber());
        }
    }
}
