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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.sql.Date;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.label.valueholder.Label;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testtrailer.valueholder.TestTrailer;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

/**
 * @author benzd1
 */

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "TEST")
@AttributeOverrides({ @AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED")),
        @AttributeOverride(name = "name", column = @Column(name = "name")) })
public class Test extends EnumValueItemImpl {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "test_seq_gen")
    @GenericGenerator(name = "test_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "test_seq"))
    @Column(name = "ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Transient
    private String methodName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "METHOD_ID")
    private Method method;

    @Transient
    private String labelName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "LABEL_ID")
    private Label label;

    @Transient
    private String testTrailerName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TEST_TRAILER_ID")
    private TestTrailer testTrailer;

    @Transient
    private String testSectionName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TEST_SECTION_ID")
    private TestSection testSection;

    @Transient
    private String scriptletName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SCRIPTLET_ID")
    private Scriptlet scriptlet;

    @Column(name = "DESCRIPTION", length = 60, nullable = false, unique = true)
    private String description;

    @Column(name = "NORMALIZED_DESCRIPTION", length = 225)
    private String normalizedDescription;

    @Column(name = "LOINC", length = 240)
    private String loinc;

    // OGC-949 M1 / OGC-936: test catalog v2.5 domain (the AMR flag reuses the
    // existing antimicrobialResistance field below — no parallel column)
    @Column(name = "DOMAIN", length = 20)
    private String domain = "CLINICAL";

    @Column(name = "STICKER_REQ_FLAG", length = 1)
    private String stickerRequiredFlag;

    @Transient
    private String alternateTestDisplayValue;

    @Column(name = "ACTIVE_BEGIN", length = 7)
    private Date activeBeginDate = null;

    @Transient
    private String activeBeginDateForDisplay;

    @Column(name = "ACTIVE_END", length = 7)
    private Date activeEndDate = null;

    @Transient
    private String activeEndDateForDisplay;

    @Column(name = "IS_REPORTABLE", length = 1)
    private String isReportable;

    @Column(name = "TIME_HOLDING", precision = 22, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String timeHolding;

    @Column(name = "TIME_WAIT", precision = 22, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String timeWait;

    @Column(name = "TIME_TA_AVERAGE", precision = 22, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String timeAverage;

    @Column(name = "TIME_TA_WARNING", precision = 22, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String timeWarning;

    @Column(name = "TIME_TA_MAX", precision = 22, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String timeMax;

    @Column(name = "LABEL_QTY", precision = 22, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String labelQuantity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UOM_ID")
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "SORT_ORDER", length = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sortOrder;

    @Column(name = "LOCAL_CODE", length = 10, unique = true)
    private String localCode;

    @Column(name = "orderable")
    private Boolean orderable;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "name_localization_id")
    private Localization localizedTestName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporting_name_localization_id")
    private Localization localizedReportingName;

    @Transient
    private TestSection localizedTestSectionName;

    @Transient
    private TestSection localizedReportingTestSectionName;

    @Column(name = "guid")
    private String guid;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "default_test_result_id")
    private TestResult defaultTestResult;

    @Column(name = "in_lab_only")
    private boolean inLabOnly;

    // should we notify patient of a finalized result
    @Column(name = "notify_results")
    private Boolean notifyResults;

    @Column(name = "antimicrobial_resistance")
    private Boolean antimicrobialResistance;

    @Column(name = "IS_ACTIVE", length = 1)
    private String isActive = IActionConstants.YES;

    @Override
    public String getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Test() {
        super();
    }

    @Override
    public void setId(String id) {
        this.id = id;
        this.key = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setActiveBeginDate(Date activeBeginDate) {
        this.activeBeginDate = activeBeginDate;
        if (activeBeginDate != null) {
            this.activeBeginDateForDisplay = DateUtil.convertSqlDateToStringDate(activeBeginDate);
        }
    }

    public void setActiveBeginDateForDisplay(String activeBeginDateForDisplay) {
        this.activeBeginDateForDisplay = activeBeginDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.activeBeginDate = DateUtil.convertStringDateToSqlDate(this.activeBeginDateForDisplay, locale);
    }

    public void setActiveEndDate(Date activeEndDate) {
        this.activeEndDate = activeEndDate;
        if (activeEndDate != null) {
            this.activeEndDateForDisplay = DateUtil.convertSqlDateToStringDate(activeEndDate);
        }
    }

    public void setActiveEndDateForDisplay(String activeEndDateForDisplay) {
        this.activeEndDateForDisplay = activeEndDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.activeEndDate = DateUtil.convertStringDateToSqlDate(this.activeEndDateForDisplay, locale);
    }

    @Override
    public String getIsActive() {
        return isActive;
    }

    public boolean isActive() {
        return "Y".equals(isActive);
    }

    @Override
    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getTestDisplayValue() {
        if (!StringUtil.isNullorNill(getLocalizedName())) {
            return getLocalizedName() + "-" + description;
        } else {
            return description;
        }
    }

    public String getAlternateTestDisplayValue() {
        if (!StringUtil.isNullorNill(this.description)) {
            alternateTestDisplayValue = description + "-" + getLocalizedName();
        } else {
            alternateTestDisplayValue = getLocalizedName();
        }
        return alternateTestDisplayValue;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return TestServiceImpl.getUserLocalizedTestName(this);
    }

    @Override
    public String getName() {
        Localization localizedName = getLocalizedTestName();
        if (localizedName != null && localizedName.getLocalizedValue() != null) {
            return localizedName.getLocalizedValue();
        }
        return description;
    }

    public Boolean isNotifyResults() {
        if (notifyResults == null) {
            return false;
        }
        return notifyResults;
    }

    public void setNotifyResults(boolean notifyResults) {
        this.notifyResults = notifyResults;
    }

    public String getAugmentedTestName() {
        return TestServiceImpl.getLocalizedTestNameWithType(this.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Test that = (Test) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
