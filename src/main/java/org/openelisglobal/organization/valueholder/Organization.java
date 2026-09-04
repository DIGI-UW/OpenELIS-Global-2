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
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.URL;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;
import org.openelisglobal.validation.annotations.SafeHtml;

@Setter
@Getter
@Entity
@DynamicUpdate
@Table(name = "ORGANIZATION")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Organization extends EnumValueItemImpl implements SimpleBaseEntity<String> {
    private static final long serialVersionUID = 1L;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "CITY", length = 30)
    private String city;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "CLIA_NUM", length = 12)
    private String cliaNum;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    @Column(name = "phone", length = 20)
    private String phone;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "fax", length = 20)
    private String fax;

    @Email
    @Column(name = "email", length = 255)
    private String email;

    @Id
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    @GeneratedValue(generator = "organization_seq_gen")
    @GenericGenerator(name = "organization_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "organization_seq"))
    @Column(name = "ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @URL
    @Column(name = "INTERNET_ADDRESS", length = 40)
    private String internetAddress;

    @Pattern(regexp = ValidationHelper.YES_NO_REGEX)
    @Column(name = "IS_ACTIVE", length = 1)
    private String isActive;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "MLS_LAB_FLAG", length = 1)
    private String mlsLabFlag;

    @Pattern(regexp = ValidationHelper.YES_NO_REGEX)
    @Column(name = "MLS_SENTINEL_LAB_FLAG", length = 1, nullable = false)
    private String mlsSentinelLabFlag;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "MULTIPLE_UNIT", length = 30)
    private String multipleUnit;

    @JsonIgnoreProperties({ "organization" })
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ORG_ID")
    private Organization organization;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "NAME", length = 40, nullable = false)
    private String organizationName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "ORG_MLT_ORG_MLT_ID", length = 10)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String orgMltOrgMltId;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    @Column(name = "PWS_ID", length = 15)
    private String pwsId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "SHORT_NAME", length = 15)
    private String shortName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "STATE", length = 2)
    private String state;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "STREET_ADDRESS", length = 30)
    private String streetAddress;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "ZIP_CODE", length = 10)
    private String zipCode;

    @Transient
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String selectedOrgId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "LOCAL_ABBREV", length = 10)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String organizationLocalAbbreviation;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "code", length = 20)
    private String code;

    @ManyToMany(mappedBy = "organizations", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrganizationType> organizationTypes;

    @Column(name = "fhir_uuid")
    private UUID fhirUuid;

    public Organization() {
        super();
    }

    @Override
    public String getId() {
        return id;
    }

    @JsonIgnore
    public String getConcatOrganizationLocalAbbreviationName() {
        return organizationLocalAbbreviation + "-" + organizationName;
    }

    @Override
    public String getIsActive() {
        return isActive;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    @Transient
    private Set testSections = new HashSet(0);

    @JsonIgnore
    public String getDoubleName() {
        return shortName + " = " + organizationName;
    }

    @Override
    public String toString() {
        return "Organization [id=" + id + ", isActive=" + isActive + ", organizationName=" + organizationName
                + ", organizationLocalAbbreviation=" + organizationLocalAbbreviation + ", shortName=" + shortName + "]";
    }

    @JsonIgnore
    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }
}
