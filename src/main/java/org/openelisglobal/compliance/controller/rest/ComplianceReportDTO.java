package org.openelisglobal.compliance.controller.rest;

import java.util.List;

public class ComplianceReportDTO {

    private int ineligibleCount;
    private int generatedCount;
    private int notYetGeneratedCount;
    private List<ComplianceReportOrderDTO> orders;
    private Integer labNumberMatches;
    private boolean labNumberLimited;

    /**
     * Environmental orders whose lab number matched, before the other filters; null
     * when no lab number was searched. Lets the page tell an unknown lab number
     * from an order the filters hide.
     */
    public Integer getLabNumberMatches() {
        return labNumberMatches;
    }

    public void setLabNumberMatches(Integer labNumberMatches) {
        this.labNumberMatches = labNumberMatches;
    }

    /** True when the lab number matched more orders than are listed. */
    public boolean isLabNumberLimited() {
        return labNumberLimited;
    }

    public void setLabNumberLimited(boolean labNumberLimited) {
        this.labNumberLimited = labNumberLimited;
    }

    public int getIneligibleCount() {
        return ineligibleCount;
    }

    public void setIneligibleCount(int ineligibleCount) {
        this.ineligibleCount = ineligibleCount;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(int generatedCount) {
        this.generatedCount = generatedCount;
    }

    public int getNotYetGeneratedCount() {
        return notYetGeneratedCount;
    }

    public void setNotYetGeneratedCount(int notYetGeneratedCount) {
        this.notYetGeneratedCount = notYetGeneratedCount;
    }

    public List<ComplianceReportOrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<ComplianceReportOrderDTO> orders) {
        this.orders = orders;
    }
}
