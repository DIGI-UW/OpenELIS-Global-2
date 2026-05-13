package org.openelisglobal.common.management.form;

import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.management.valueholder.SampleTypeDisplay;

public class SampleTypeManagementForm extends BaseForm {
    private List<SampleTypeDisplay> menuList;
    private String fromRecordCount;
    private String toRecordCount;
    private String totalRecordCount;
    private String searchString = "";

    public SampleTypeManagementForm() {
        setFormName("sampleTypeManagementForm");
    }

    public List<SampleTypeDisplay> getMenuList() {
        return menuList;
    }

    public void setMenuList(List<SampleTypeDisplay> menuList) {
        this.menuList = menuList;
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

    public String getTotalRecordCount() {
        return totalRecordCount;
    }

    public void setTotalRecordCount(String totalRecordCount) {
        this.totalRecordCount = totalRecordCount;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }
}
