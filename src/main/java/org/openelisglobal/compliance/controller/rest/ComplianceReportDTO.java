package org.openelisglobal.compliance.controller.rest;

import java.util.List;

public class ComplianceReportDTO {

    private int ineligibleCount;
    private int generatedCount;
    private int notYetGeneratedCount;
    private List<ComplianceReportOrderDTO> orders;

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
