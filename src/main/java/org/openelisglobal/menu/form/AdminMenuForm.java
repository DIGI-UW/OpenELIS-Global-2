package org.openelisglobal.menu.form;

import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.menu.valueholder.AdminMenuItem;

public class AdminMenuForm extends BaseForm {

    private List<AdminMenuItem> adminMenuItems;

    private String totalRecordCount = "";
    private String fromRecordCount = "1";
    private String toRecordCount = "20";

    public List<AdminMenuItem> getAdminMenuItems() {
        return adminMenuItems;
    }

    public void setAdminMenuItems(List<AdminMenuItem> adminMenuItems) {
        this.adminMenuItems = adminMenuItems;
    }

    public String getTotalRecordCount() {
        return totalRecordCount;
    }

    public void setTotalRecordCount(String totalRecordCount) {
        this.totalRecordCount = totalRecordCount;
    }

    public String getFromRecordCount() {
        return fromRecordCount;
    }

    public void setFromRecordCount(String fromRecordCount) {
        this.fromRecordCount = fromRecordCount;
    }

    public String getToRecordCount() {
        return toRecordCount;
    }

    public void setToRecordCount(String toRecordCount) {
        this.toRecordCount = toRecordCount;
    }
}
