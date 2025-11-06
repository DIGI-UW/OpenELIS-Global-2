package org.openelisglobal.analyzerimport.form;

import java.util.List;
import org.openelisglobal.analyzerimport.action.beans.NamedAnalyzerTestMapping;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.validation.annotations.SafeHtml;

public class AnalyzerTestNameMenuForm extends AdminOptionMenuForm<NamedAnalyzerTestMapping> {
    /** */
    private static final long serialVersionUID = -5470283912736977696L;

    // for display
    private List<NamedAnalyzerTestMapping> menuList;

    private List<@SafeHtml(level = SafeHtml.SafeListLevel.NONE) String> selectedIDs;

    public AnalyzerTestNameMenuForm() {
        setFormName("analyzerTestNameMenuForm");
    }

    @Override
    public List<NamedAnalyzerTestMapping> getMenuList() {
        return menuList;
    }

    @Override
    public void setMenuList(List<NamedAnalyzerTestMapping> menuList) {
        this.menuList = menuList;
    }

    @Override
    public List<String> getSelectedIDs() {
        return selectedIDs;
    }

    @Override
    public void setSelectedIDs(List<String> selectedIDs) {
        this.selectedIDs = selectedIDs;
    }
}
