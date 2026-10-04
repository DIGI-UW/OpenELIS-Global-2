package org.openelisglobal.organization.locations;

import java.util.List;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.organization.locations.LocationsApi.Query;
import org.openelisglobal.organization.locations.LocationsImportApi.Options;
import org.openelisglobal.organization.locations.LocationsImportApi.Plan;
import org.openelisglobal.organization.locations.LocationsImportApi.RecentRun;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

/**
 * OGC-1363 (sections F, G and M): the CSV import of organizations, wards,
 * sampling sites and geographic areas, with a preview that saves nothing, the
 * decision queue, the possible-rename check and the scoped Replace mode; the
 * export of a filtered list in the same format; and the templates.
 */
public interface LocationsImportService {

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    Plan preview(List<MultipartFile> files, List<String> areas, Options options, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    Plan apply(List<MultipartFile> files, List<String> areas, Options options, String sysUserId);

    /** Called by the organizations loader for every file it reads, in any mode. */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    Plan loadOrganizations(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName,
            Options options);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<RecentRun> recentRuns();

    /** The CSV result report of a run: every row with its outcome. */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    String report(String runId);

    /** The header row of an area's format with one example row. */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    String template(String area);

    /**
     * The rows matching the list filters, or the given ids, in the organizations
     * format.
     */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    String export(Query query, List<String> ids);

    /** Every geographic area as the flattened values file (Name%Code per level). */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    String exportAreas();
}
