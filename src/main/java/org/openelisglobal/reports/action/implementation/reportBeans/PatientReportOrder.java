package org.openelisglobal.reports.action.implementation.reportBeans;

import java.util.ArrayList;
import java.util.List;

/**
 * What the patient report prints once per order rather than per result: the
 * Samples block, the release summary, the accreditation marks resolved for this
 * order's own tests, and the corrected-report reference.
 */
public final class PatientReportOrder {

    private boolean complete = true;
    private List<ReportSampleLine> samples = new ArrayList<>();
    private List<String> validators = new ArrayList<>();
    private String completedTime;
    private List<byte[]> accreditationLogos = new ArrayList<>();
    private List<String> accreditingBodies = new ArrayList<>();
    private boolean corrected = false;
    private String replacedIssueTime;
    private String earliestCollection;
    private boolean collectionsDiffer = false;
    private String orderDate;
    private Integer version;
    private String issueTime;
    private String changeSummary;

    /** Every non-cancelled, non-rejected result on the order is released. */
    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    /** The order's samples in sample-number order. */
    public List<ReportSampleLine> getSamples() {
        return samples;
    }

    public void setSamples(List<ReportSampleLine> samples) {
        this.samples = samples;
    }

    /** Each validator with their latest release time, in order of release. */
    public List<String> getValidators() {
        return validators;
    }

    public void setValidators(List<String> validators) {
        this.validators = validators;
    }

    /** When the last result was released, or null while any result is pending. */
    public String getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(String completedTime) {
        this.completedTime = completedTime;
    }

    public List<byte[]> getAccreditationLogos() {
        return accreditationLogos;
    }

    public void setAccreditationLogos(List<byte[]> accreditationLogos) {
        this.accreditationLogos = accreditationLogos;
    }

    /** The bodies whose accreditation the order's tests fall under. */
    public List<String> getAccreditingBodies() {
        return accreditingBodies;
    }

    public void setAccreditingBodies(List<String> accreditingBodies) {
        this.accreditingBodies = accreditingBodies;
    }

    /** A result on the order changed after its patient report was first issued. */
    public boolean isCorrected() {
        return corrected;
    }

    public void setCorrected(boolean corrected) {
        this.corrected = corrected;
    }

    /** When the report this one replaces was issued, or null when unknown. */
    public String getReplacedIssueTime() {
        return replacedIssueTime;
    }

    public void setReplacedIssueTime(String replacedIssueTime) {
        this.replacedIssueTime = replacedIssueTime;
    }

    /** The collection time of the order's earliest sample, or null. */
    public String getEarliestCollection() {
        return earliestCollection;
    }

    public void setEarliestCollection(String earliestCollection) {
        this.earliestCollection = earliestCollection;
    }

    /** The order's samples were not all collected at the same time. */
    public boolean isCollectionsDiffer() {
        return collectionsDiffer;
    }

    public void setCollectionsDiffer(boolean collectionsDiffer) {
        this.collectionsDiffer = collectionsDiffer;
    }

    /** The requested day of the order, or its receipt time when none was given. */
    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    /**
     * The version number this print creates in the Report Print Queue, or null
     * outside the queue pipeline.
     */
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * When the version this print belongs to was first issued, as the report prints
     * times; null when the print is not a queue version, in which case the render
     * time is used.
     */
    public String getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(String issueTime) {
        this.issueTime = issueTime;
    }

    /**
     * What changed since the replaced version, as the queue recorded it; null when
     * unknown.
     */
    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }
}
