package org.openelisglobal.dictionary.form;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.validation.annotations.SafeHtml;

public class DictionaryMenuForm extends AdminOptionMenuForm<Dictionary> {
    /** */
    private static final long serialVersionUID = -1585240883233995437L;

    // for display
    private List<Dictionary> menuList;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedIDs;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String searchString = "";

    public DictionaryMenuForm() {
        setFormName("dictionaryMenuForm");
    }

    @Override
    public List<Dictionary> getMenuList() {
        return menuList;
    }

    @Override
    public void setMenuList(List<Dictionary> menuList) {
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
