package org.openelisglobal.siteinformation.form;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

public class SiteInformationMenuForm extends AdminOptionMenuForm<SiteInformation> {
    /** */
    private static final long serialVersionUID = 3557902905876309224L;

    // for display
    private List<SiteInformation> menuList;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedIDs;

    // in validator
    private String siteInfoDomainName; // = "SiteInformation";

    public SiteInformationMenuForm() {
        // setFormName("siteInformationMenuForm");
    }

    @Override
    public List<SiteInformation> getMenuList() {
        return menuList;
    }

    @Override
    public void setMenuList(List<SiteInformation> menuList) {
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

    public String getSiteInfoDomainName() {
        return siteInfoDomainName;
    }

    public void setSiteInfoDomainName(String siteInfoDomainName) {
        this.siteInfoDomainName = siteInfoDomainName;
    }
}
