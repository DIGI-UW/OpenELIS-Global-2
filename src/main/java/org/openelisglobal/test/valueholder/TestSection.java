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
package org.openelisglobal.test.valueholder;

import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;

public class TestSection extends EnumValueItemImpl {

    private static final long serialVersionUID = -1574344492809195601L;

    private String id;

    private String isExternal;

    private Timestamp lastupdated;

    private String testSectionName;

    private String description;

    private ValueHolderInterface organization;

    private String selectedOrganizationId;

    private int sortOrderInt;

    private String selectedParentTestSectionId;

    private ValueHolderInterface parentTestSection;

    private ValueHolderInterface localization;

    private String isActive;

    public TestSection() {
        super();
        organization = new ValueHolder();
        parentTestSection = new ValueHolder();
        localization = new ValueHolder();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getIsExternal() {
        return isExternal;
    }

    public String getTestSectionName() {
        return testSectionName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public void setIsExternal(String isExternal) {
        this.isExternal = isExternal;
    }

    public void setTestSectionName(String testSectionName) {
        this.testSectionName = testSectionName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Organization getOrganization() {
        return (Organization) organization.getValue();
    }

    public void setOrganization(Organization organization) {
        this.organization.setValue(organization);
    }

    public void setSelectedOrganizationId(String selectedOrganizationId) {
        this.selectedOrganizationId = selectedOrganizationId;
    }

    public String getSelectedOrganizationId() {
        return selectedOrganizationId;
    }

    @Override
    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }

    @Override
    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public TestSection getParentTestSection() {
        return (TestSection) parentTestSection.getValue();
    }

    public void setParentTestSection(TestSection parentTestSection) {
        this.parentTestSection.setValue(parentTestSection);
    }

    public void setSelectedParentTestSectionId(String selectedParentTestSectionId) {
        this.selectedParentTestSectionId = selectedParentTestSectionId;
    }

    public String getSelectedParentTestSectionId() {
        return selectedParentTestSectionId;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return SpringContext.getBean(TestSectionService.class).getUserLocalizedTesSectionName(this);
    }

    public int getSortOrderInt() {
        return sortOrderInt;
    }

    public void setSortOrderInt(int sortOrderInt) {
        this.sortOrderInt = sortOrderInt;
    }

    @Override
    public String getIsActive() {
        return isActive;
    }

    @Override
    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public Localization getLocalization() {
        return (Localization) localization.getValue();
    }

    public void setLocalization(Localization localization) {
        this.localization.setValue(localization);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TestSection that = (TestSection) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
