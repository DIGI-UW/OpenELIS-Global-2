package org.openelisglobal.organization.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import org.hibernate.validator.constraints.URL;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.validation.annotations.OptionalNotBlank;
import org.openelisglobal.validation.annotations.SafeHtml;

public class OrganizationForm extends BaseForm {
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String id = "";

    @OptionalNotBlank(formFields = { Field.OrgLocalAbrev })
    private String organizationLocalAbbreviation = "";

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String organizationName = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String city = "";

    @OptionalNotBlank(formFields = { Field.OrganizationAddressInfo, Field.ZipCode })
    @Pattern(regexp = "^[a-zA-Z0-9 ]*$")
    private String zipCode = "";

    @OptionalNotBlank(formFields = { Field.MLS })
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String mlsSentinelLabFlag = "";

    @NotBlank
    @Pattern(regexp = ValidationHelper.YES_NO_REGEX)
    private String isActive = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String orgMltOrgMltId = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String selectedOrgId = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String parentOrgName = "";

    @Valid
    private Organization organization;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String shortName = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String multipleUnit = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String streetAddress = "";

    @OptionalNotBlank(formFields = { Field.OrganizationAddressInfo, Field.OrgState })
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String state = "";
    // for display
    private Collection states;

    @URL
    private String internetAddress = "";

    @OptionalNotBlank(formFields = { Field.MLS })
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String mlsLabFlag = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String cliaNum = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String pwsId = "";

    // for display
    private List<OrganizationType> orgTypes;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedTypes;

    private Timestamp lastupdated;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String commune = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String village = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String department = "";

    // for display
    private List<Dictionary> departmentList;

    public OrganizationForm() {
        setFormName("organizationForm");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationLocalAbbreviation() {
        return organizationLocalAbbreviation;
    }

    public void setOrganizationLocalAbbreviation(String organizationLocalAbbreviation) {
        this.organizationLocalAbbreviation = organizationLocalAbbreviation;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getMlsSentinelLabFlag() {
        return mlsSentinelLabFlag;
    }

    public void setMlsSentinelLabFlag(String mlsSentinelLabFlag) {
        this.mlsSentinelLabFlag = mlsSentinelLabFlag;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getOrgMltOrgMltId() {
        return orgMltOrgMltId;
    }

    public void setOrgMltOrgMltId(String orgMltOrgMltId) {
        this.orgMltOrgMltId = orgMltOrgMltId;
    }

    public String getSelectedOrgId() {
        return selectedOrgId;
    }

    public void setSelectedOrgId(String selectedOrgId) {
        this.selectedOrgId = selectedOrgId;
    }

    public String getParentOrgName() {
        return parentOrgName;
    }

    public void setParentOrgName(String parentOrgName) {
        this.parentOrgName = parentOrgName;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getMultipleUnit() {
        return multipleUnit;
    }

    public void setMultipleUnit(String multipleUnit) {
        this.multipleUnit = multipleUnit;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Collection getStates() {
        return states;
    }

    public void setStates(Collection states) {
        this.states = states;
    }

    public String getInternetAddress() {
        return internetAddress;
    }

    public void setInternetAddress(String internetAddress) {
        this.internetAddress = internetAddress;
    }

    public String getMlsLabFlag() {
        return mlsLabFlag;
    }

    public void setMlsLabFlag(String mlsLabFlag) {
        this.mlsLabFlag = mlsLabFlag;
    }

    public String getCliaNum() {
        return cliaNum;
    }

    public void setCliaNum(String cliaNum) {
        this.cliaNum = cliaNum;
    }

    public String getPwsId() {
        return pwsId;
    }

    public void setPwsId(String pwsId) {
        this.pwsId = pwsId;
    }

    public List<OrganizationType> getOrgTypes() {
        return orgTypes;
    }

    public void setOrgTypes(List<OrganizationType> orgTypes) {
        this.orgTypes = orgTypes;
    }

    public List<String> getSelectedTypes() {
        return selectedTypes;
    }

    public void setSelectedTypes(List<String> selectedTypes) {
        this.selectedTypes = selectedTypes;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }

    public String getCommune() {
        return commune;
    }

    public void setCommune(String commune) {
        this.commune = commune;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List<Dictionary> getDepartmentList() {
        return departmentList;
    }

    public void setDepartmentList(List<Dictionary> departmentList) {
        this.departmentList = departmentList;
    }
}
