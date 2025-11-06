package org.openelisglobal.organization.form;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.validation.annotations.SafeHtml;

public class OrganizationMenuForm extends AdminOptionMenuForm<Organization> {
    /** */
    private static final long serialVersionUID = 3849998051592681962L;

    // for display
    private List<Organization> menuList;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedIDs;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String searchString = "";

    public OrganizationMenuForm() {
        setFormName("organizationMenuForm");
    }

    @Override
    public List<Organization> getMenuList() {
        return menuList;
    }

    @Override
    public void setMenuList(List<Organization> menuList) {
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
}
