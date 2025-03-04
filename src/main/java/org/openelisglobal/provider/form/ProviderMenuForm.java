package org.openelisglobal.provider.form;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.validation.annotations.SafeHtml;

public class ProviderMenuForm extends AdminOptionMenuForm<Provider> {

    // for display
    private List<Provider> menuList;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedIDs;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String searchString = "";

    public ProviderMenuForm() {
        setFormName("providerMenuForm");
    }

    @Override
    public List<Provider> getMenuList() {
        return menuList;
    }

    @Override
    public void setMenuList(List<Provider> menuList) {
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

    private List<Provider> providers;

    public List<Provider> getProviders() {
        return providers;
    }

    public void setProviders(List<Provider> providers) {
        this.providers = providers;
    }
}
