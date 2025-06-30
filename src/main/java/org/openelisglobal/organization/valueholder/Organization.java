/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.organization.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.validation.annotations.SafeHtml;

public class Organization extends EnumValueItemImpl implements SimpleBaseEntity<String> {
    private static final long serialVersionUID = 1L;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String city;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String cliaNum;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String id;

    @URL
    private String internetAddress;

    @Pattern(regexp = ValidationHelper.YES_NO_REGEX)
    private String isActive;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String mlsLabFlag;

    @Pattern(regexp = ValidationHelper.YES_NO_REGEX)
    private String mlsSentinelLabFlag;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String multipleUnit;

    @JsonIgnoreProperties({ "organization" })
    private ValueHolderInterface organization;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String organizationName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String orgMltOrgMltId;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String pwsId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String shortName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String state;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String streetAddress;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String zipCode;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String selectedOrgId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String organizationLocalAbbreviation;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String code;

    private Set<OrganizationType> organizationTypes;
    private UUID fhirUuid;

    public Organization() {
        super();
        organization = new ValueHolder();
    }

    public String getCity() {
        return city;
    }

    public String getCliaNum() {
        return cliaNum;
    }

    @Override
    public String getId() {
        return id;
    }

    @JsonIgnore
    public String getConcatOrganizationLocalAbbreviationName() {
        return organizationLocalAbbreviation + "-" + organizationName;
    }

    public String getInternetAddress() {
        return internetAddress;
    }

    @Override
    public String getIsActive() {
        return isActive;
    }

    public String getMlsLabFlag() {
        return mlsLabFlag;
    }

    public String getMlsSentinelLabFlag() {
        return mlsSentinelLabFlag;
    }

    public String getMultipleUnit() {
        return multipleUnit;
    }

    public String getOrgMltOrgMltId() {
        return orgMltOrgMltId;
    }

    public Organization getOrganization() {
        return (Organization) organization.getValue();
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getPwsId() {
        return pwsId;
    }

    public String getShortName() {
        return shortName;
    }

    public String getState() {
        return state;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCliaNum(String cliaNum) {
        this.cliaNum = cliaNum;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public void setInternetAddress(String internetAddress) {
        this.internetAddress = internetAddress;
    }

    @Override
    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public void setMlsLabFlag(String mlsLabFlag) {
        this.mlsLabFlag = mlsLabFlag;
    }

    public void setMlsSentinelLabFlag(String mlsSentinelLabFlag) {
        this.mlsSentinelLabFlag = mlsSentinelLabFlag;
    }

    public void setMultipleUnit(String multipleUnit) {
        this.multipleUnit = multipleUnit;
    }

    public void setOrgMltOrgMltId(String orgMltOrgMltId) {
        this.orgMltOrgMltId = orgMltOrgMltId;
    }

    public void setOrganization(Organization organization) {
        this.organization.setValue(organization);
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public void setPwsId(String pwsId) {
        this.pwsId = pwsId;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public void setSelectedOrgId(String selectedOrgId) {
        this.selectedOrgId = selectedOrgId;
    }

    public String getSelectedOrgId() {
        return selectedOrgId;
    }

    private Set testSections = new HashSet(0);

    public Set getTestSections() {
        return testSections;
    }

    public void setTestSections(Set testSections) {
        this.testSections = testSections;
    }

    public String getOrganizationLocalAbbreviation() {
        return organizationLocalAbbreviation;
    }

    public void setOrganizationLocalAbbreviation(String organizationLocalAbbreviation) {
        this.organizationLocalAbbreviation = organizationLocalAbbreviation;
    }

    @JsonIgnore
    public String getDoubleName() {
        return shortName + " = " + organizationName;
    }

    @Override
    public String toString() {
        return "Organization [id=" + id + ", isActive=" + isActive + ", organizationName=" + organizationName
                + ", organizationLocalAbbreviation=" + organizationLocalAbbreviation + ", shortName=" + shortName + "]";
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Set<OrganizationType> getOrganizationTypes() {
        return organizationTypes;
    }

    public void setOrganizationTypes(Set<OrganizationType> organizationTypes) {
        this.organizationTypes = organizationTypes;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    @JsonIgnore
    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }
}
