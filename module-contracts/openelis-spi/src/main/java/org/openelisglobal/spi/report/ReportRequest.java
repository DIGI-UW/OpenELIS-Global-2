package org.openelisglobal.spi.report;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * What the core hands a {@link ReportProvider}: the report id, the request
 * parameters, the user and the locale.
 */
public final class ReportRequest {

    private final String reportId;
    private final Map<String, String> parameters;
    private final String systemUserId;
    private final Locale locale;

    public ReportRequest(String reportId, Map<String, String> parameters, String systemUserId, Locale locale) {
        this.reportId = Objects.requireNonNull(reportId, "reportId");
        this.parameters = parameters == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        this.systemUserId = systemUserId;
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    /**
     * The report id the user asked for; one of the provider's
     * {@link ReportProvider#reportIds()}.
     */
    public String getReportId() {
        return reportId;
    }

    /**
     * The request parameters as the report form submitted them (accession range,
     * dates, selections).
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /** One parameter, or {@code null} when it was not submitted. */
    public String getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * The id of the user requesting the report; {@code null} for an anonymous
     * request.
     */
    public String getSystemUserId() {
        return systemUserId;
    }

    /** The locale the report should be rendered in. */
    public Locale getLocale() {
        return locale;
    }
}
