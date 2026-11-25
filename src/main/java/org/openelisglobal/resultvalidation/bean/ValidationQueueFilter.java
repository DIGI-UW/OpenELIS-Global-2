package org.openelisglobal.resultvalidation.bean;

import org.apache.commons.validator.GenericValidator;

/**
 * OGC-1418: the Validation queue's search, every criterion optional and all of
 * them combined. A lab number range is {@code labNumberFrom} to
 * {@code labNumberTo}; one lab number is a range of one; a range with no end is
 * every lab number from {@code labNumberFrom} on (the old "load next"). Dates
 * are the analysis start date, inclusive, in the site's date format.
 */
public class ValidationQueueFilter {

    private String labNumberFrom;
    private String labNumberTo;
    private String testSectionId;
    private String fromDate;
    private String toDate;
    private String patientId;

    public ValidationQueueFilter() {
    }

    public ValidationQueueFilter(String labNumberFrom, String labNumberTo, String testSectionId, String fromDate,
            String toDate, String patientId) {
        this.labNumberFrom = trim(labNumberFrom);
        this.labNumberTo = trim(labNumberTo);
        this.testSectionId = trim(testSectionId);
        this.fromDate = trim(fromDate);
        this.toDate = trim(toDate);
        this.patientId = trim(patientId);
    }

    /** Whether any criterion is set; an empty search loads nothing. */
    public boolean isEmpty() {
        return blank(labNumberFrom) && blank(testSectionId) && blank(fromDate) && blank(toDate) && blank(patientId);
    }

    /** One lab number and nothing else: the order's own queue, as before. */
    public boolean isSingleLabNumberOnly() {
        return isSingleLabNumber() && blank(testSectionId) && blank(fromDate) && blank(toDate) && blank(patientId);
    }

    public boolean isSingleLabNumber() {
        return !blank(labNumberFrom) && labNumberFrom.equals(labNumberTo);
    }

    /**
     * Whether the lab number lies in the range. Lab numbers of one format are
     * compared as text of equal length, as the accession query does.
     */
    public boolean matchesLabNumber(String labNumber) {
        if (blank(labNumberFrom)) {
            return true;
        }
        if (labNumber == null || labNumber.length() != labNumberFrom.length()
                || labNumber.compareTo(labNumberFrom) < 0) {
            return false;
        }
        return blank(labNumberTo) || labNumber.compareTo(labNumberTo) <= 0;
    }

    public String getLabNumberFrom() {
        return labNumberFrom;
    }

    public void setLabNumberFrom(String labNumberFrom) {
        this.labNumberFrom = trim(labNumberFrom);
    }

    public String getLabNumberTo() {
        return labNumberTo;
    }

    public void setLabNumberTo(String labNumberTo) {
        this.labNumberTo = trim(labNumberTo);
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = trim(testSectionId);
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = trim(fromDate);
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = trim(toDate);
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = trim(patientId);
    }

    private static boolean blank(String value) {
        return GenericValidator.isBlankOrNull(value);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
