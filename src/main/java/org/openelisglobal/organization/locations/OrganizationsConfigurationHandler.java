package org.openelisglobal.organization.locations;

import java.util.List;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.organization.locations.LocationsImportApi.Plan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * OGC-1363 (Dependency 6): the organizations import area on the shared catalog
 * loader, so the same {@code organizations-*.csv} file loads at start-up,
 * through the reload API and from the Locations import page. Facilities, wards
 * / depts and sampling sites share one file (section M). The loader keeps the
 * full plan of its last run so the import page can show every outcome, not just
 * counts.
 */
@Component
public class OrganizationsConfigurationHandler extends AbstractCatalogCsvHandler {

    public static final String DOMAIN = "organizations";

    @Autowired
    @Lazy
    private LocationsImportService importService;

    private volatile Plan lastPlan;

    @Override
    public String getDomainName() {
        return DOMAIN;
    }

    @Override
    public int getLoadOrder() {
        return 60;
    }

    @Override
    public String getFileMatcher() {
        return "organizations*.csv";
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "type", "name" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        lastPlan = importService.loadOrganizations(rows, transaction, summary, fileName,
                LocationsImportContext.options());
    }

    public Plan getLastPlan() {
        return lastPlan;
    }
}
