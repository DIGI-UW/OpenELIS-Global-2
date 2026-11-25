package org.openelisglobal.compliance.service;

import org.openelisglobal.compliance.valueholder.ComplianceStatus;

/**
 * One line of the results section of an environmental results report
 * (OGC-1617): a validated result with its unit and method, and the guideline
 * value and status of a standard that sets a limit for the test. A test no
 * linked standard covers has an empty guideline and no status.
 *
 * @param sampleNumber the sample's position on the order
 * @param sampleType   the sample type
 */
public record EnvironmentalReportRow(String sampleNumber, String sampleType, String parameter, String result,
        String unit, String method, String guideline, ComplianceStatus status) {
}
