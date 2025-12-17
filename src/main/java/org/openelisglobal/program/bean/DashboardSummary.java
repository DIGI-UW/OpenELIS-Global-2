package org.openelisglobal.program.bean;

import org.openelisglobal.common.rest.provider.form.OrderProgramsDashboardForm;

public class DashboardSummary {

    private int totalEntries;
    private OrderProgramsDashboardForm orderProgramsDashboardForm;

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public OrderProgramsDashboardForm getOrderProgramsDashboardForm() {
        return orderProgramsDashboardForm;
    }

    public void setOrderProgramsDashboardForm(OrderProgramsDashboardForm orderProgramsDashboardForm) {
        this.orderProgramsDashboardForm = orderProgramsDashboardForm;
    }

}
