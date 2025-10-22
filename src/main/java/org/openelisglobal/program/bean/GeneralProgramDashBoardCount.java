package org.openelisglobal.program.bean;

public class GeneralProgramDashBoardCount {
    Long totalPrograms;
    Long programsWithOrders;
    Long totalOrders;

    public Long getTotalPrograms() {
        return totalPrograms;
    }

    public void setTotalPrograms(Long totalPrograms) {
        this.totalPrograms = totalPrograms;
    }

    public Long getProgramsWithOrders() {
        return programsWithOrders;
    }

    public void setProgramsWithOrders(Long programsWithOrders) {
        this.programsWithOrders = programsWithOrders;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }
}
