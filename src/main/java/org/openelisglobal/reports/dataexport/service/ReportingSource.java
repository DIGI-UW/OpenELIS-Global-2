package org.openelisglobal.reports.dataexport.service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;

/**
 * Export-source SPI (OGC-479): a pluggable provider that declares the variables
 * it can emit and streams rows for a snapshot request.
 *
 * <p>
 * Exempt from per-method privilege gating rather than gated, because it is a
 * provider contract, not a user-facing service. Implementations are selected by
 * the export orchestrator and never injected into a controller; authorization
 * belongs on the export endpoint that chooses a source and on the services it
 * reads through, not on every provider that might implement this. The package
 * currently has no implementations and no callers — it is scaffolding for the
 * configured export contracts — so gating it now would invent a privilege for a
 * caller that does not yet exist.
 *
 * <p>
 * If an implementation is ever injected into a controller directly, remove this
 * exemption and gate the methods instead.
 */
@CrossDomainService(callers = "export orchestrator (OGC-479 configured export contracts) — a provider SPI"
        + " selected by the exporter, never injected into a controller; no implementations yet")
public interface ReportingSource {
    String id();

    List<ReportingVariable> catalog();

    default List<String> defaultResultStatuses() {
        return List.of();
    }

    void validateConfiguration(ReportSourceConfig configuration);

    long write(Writer output, ExportSnapshot request) throws IOException;
}
