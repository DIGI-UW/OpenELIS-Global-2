package org.openelisglobal.program.service;

/** Renders a pathology case's report as a PDF. */
public interface PathologyReportRenderer {

    /**
     * @param amended print the report's corrected-report line, for a final issued
     *                after the case was reopened
     * @throws RuntimeException when the report cannot be rendered
     */
    byte[] render(Integer pathologySampleId, String sysUserId, ReportPaths paths, boolean amended);
}
