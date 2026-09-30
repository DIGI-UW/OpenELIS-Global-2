package org.openelisglobal.microbiology.service;

import java.util.List;

/**
 * The report-projection outcome kept separate from the clinical case release
 * state so callers can distinguish available content from a configured
 * standard-report destination.
 */
public final class MicroReportProjectionResult {

    private final String content;
    private final boolean mappingConfigured;
    private final List<String> projectedResultIds;

    public MicroReportProjectionResult(String content, boolean mappingConfigured, List<String> projectedResultIds) {
        this.content = content;
        this.mappingConfigured = mappingConfigured;
        this.projectedResultIds = List.copyOf(projectedResultIds);
    }

    public String getContent() {
        return content;
    }

    public boolean hasReportableContent() {
        return content != null && !content.trim().isEmpty();
    }

    public boolean isMappingConfigured() {
        return mappingConfigured;
    }

    public List<String> getProjectedResultIds() {
        return projectedResultIds;
    }
}
