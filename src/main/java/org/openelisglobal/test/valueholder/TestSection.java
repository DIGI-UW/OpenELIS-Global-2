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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "TEST_SECTION")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class TestSection extends EnumValueItemImpl {

    private static final long serialVersionUID = -1574344492809195601L;

    @Id
    @GeneratedValue(generator = "test_section_seq_gen")
    @GenericGenerator(name = "test_section_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "test_section_seq"))
    @Column(name = "ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "IS_EXTERNAL", length = 1)
    private String isExternal;

    @Column(name = "NAME", length = 20)
    private String testSectionName;

    @Column(name = "DESCRIPTION", length = 60, nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORG_ID")
    private Organization organization;

    @Transient
    private String selectedOrganizationId;

    @Column(name = "sort_order")
    private int sortOrderInt;

    @Transient
    private String selectedParentTestSectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_TEST_SECTION")
    private TestSection parentTestSection;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "name_localization_id")
    private Localization localization;

    @Column(name = "is_active")
    private String isActive;

    // OGC-1020 (R1): lab-unit domain drives the Results page rendering
    // (CLINICAL / ENVIRONMENTAL / VECTOR); mirrors the OGC-936 Test.domain
    // pattern
    @Column(name = "DOMAIN", length = 20)
    private String domain = "CLINICAL";

    @Column(name = "display_key", length = 60)
    private String nameKey;

    @Override
    public String getNameKey() {
        return nameKey;
    }

    @Override
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public TestSection() {
        super();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return SpringContext.getBean(TestSectionService.class).getUserLocalizedTesSectionName(this);
    }

    @Override
    public String getIsActive() {
        return isActive;
    }

    @Override
    public void setIsActive(String isActive) {
        this.isActive = isActive;
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
