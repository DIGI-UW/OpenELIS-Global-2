package org.openelisglobal.systemuser.form;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.systemuser.valueholder.UnifiedSystemUser;
import org.openelisglobal.validation.annotations.SafeHtml;

public class UnifiedSystemUserMenuForm extends AdminOptionMenuForm<UnifiedSystemUser> {
    /** */
    private static final long serialVersionUID = 4853994882374119915L;

    // for display
    private List<UnifiedSystemUser> menuList;

    // for display
    private List<IdValuePair> testSections;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedIDs;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String searchString = "";

    public UnifiedSystemUserMenuForm() {
        setFormName("unifiedSystemUserMenuForm");
    }

    @Override
    public List<UnifiedSystemUser> getMenuList() {
        return menuList;
    }

    @Override
    public void setMenuList(List<UnifiedSystemUser> menuList) {
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

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public List<IdValuePair> getTestSections() {
        return testSections;
    }

    public void setTestSections(List<IdValuePair> testSections) {
        this.testSections = testSections;
    }
}
