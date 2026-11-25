package org.openelisglobal.spi.report;

import java.util.Set;
import org.openelisglobal.spi.ExtensionPoint;

/**
 * The {@code report} extension point. A provider answers for a fixed set of
 * report ids; the core's report factory asks the extension registry for the
 * provider of a requested id before it falls back to its built-in reports.
 *
 * <p>
 * Multiple providers may be installed; report ids must be unique across them,
 * which the registry checks when the providers are registered.
 */
public interface ReportProvider extends ExtensionPoint {

    /** The report ids this provider produces; never empty. */
    Set<String> reportIds();

    /**
     * Produces the report. Called inside the core's request handling; the provider
     * may inject core services and read the request parameters, but it must not
     * open its own transaction.
     */
    ReportOutput run(ReportRequest request) throws ReportException;
}
