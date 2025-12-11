package org.openelisglobal.program.bean;

import org.openelisglobal.common.rest.provider.form.GenericProgramDashboardForm;

public class DashboardSummary {

    private int totalEntries;
    private GenericProgramDashboardForm genericProgramDashboardForm;

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public GenericProgramDashboardForm getGenericProgramDashboardForm() {
        return genericProgramDashboardForm;
    }

    public void setGenericProgramDashboardForm(GenericProgramDashboardForm genericProgramDashboardForm) {
        this.genericProgramDashboardForm = genericProgramDashboardForm;
    }

}
