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
import java.sql.Date;
import java.util.Objects;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
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
public class Test extends EnumValueItemImpl {

    private static final long serialVersionUID = 1L;

    private String id;

    private String methodName;

    private ValueHolderInterface method;

    private String labelName;

    private ValueHolderInterface label;

    private String testTrailerName;

    private ValueHolderInterface testTrailer;

    private String testSectionName;

    private ValueHolderInterface testSection;

    private String scriptletName;

    private ValueHolderInterface scriptlet;

    private String description;

    private String loinc;

    private String stickerRequiredFlag;

    private String alternateTestDisplayValue;

    private Date activeBeginDate = null;

    private String activeBeginDateForDisplay;

    private Date activeEndDate = null;

    private String activeEndDateForDisplay;

    private String isReportable;

    private String timeHolding;

    private String timeWait;

    private String timeAverage;

    private String timeWarning;

    private String timeMax;

    private String labelQuantity;

    private UnitOfMeasure unitOfMeasure;

    private String sortOrder;

    private String localCode;

    private Boolean orderable;

    private ValueHolder localizedTestName;

    private ValueHolder localizedReportingName;

    private ValueHolder localizedTestSectionName;

    private ValueHolder localizedReportingTestSectionName;

    private String guid;

    @JsonIgnore
    private ValueHolder defaultTestResult;

    private boolean inLabOnly;

    // should we notify patient of a finalized result
    private Boolean notifyResults;

    private Boolean antimicrobialResistance;

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
        this.method = new ValueHolder();
        this.label = new ValueHolder();
        this.testTrailer = new ValueHolder();
        this.testSection = new ValueHolder();
        this.scriptlet = new ValueHolder();
        localizedTestName = new ValueHolder();
        localizedReportingName = new ValueHolder();
        localizedTestSectionName = new ValueHolder();
        localizedReportingTestSectionName = new ValueHolder();
        this.defaultTestResult = new ValueHolder();
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

    public Date getActiveBeginDate() {
        return activeBeginDate;
    }

    public void setActiveBeginDate(Date activeBeginDate) {
        this.activeBeginDate = activeBeginDate;
        if (activeBeginDate != null) {
            this.activeBeginDateForDisplay = DateUtil.convertSqlDateToStringDate(activeBeginDate);
        }
    }

    public String getActiveBeginDateForDisplay() {
        return activeBeginDateForDisplay;
    }

    public void setActiveBeginDateForDisplay(String activeBeginDateForDisplay) {
        this.activeBeginDateForDisplay = activeBeginDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.activeBeginDate = DateUtil.convertStringDateToSqlDate(this.activeBeginDateForDisplay, locale);
    }

    public Date getActiveEndDate() {
        return activeEndDate;
    }

    public void setActiveEndDate(Date activeEndDate) {
        this.activeEndDate = activeEndDate;
        if (activeEndDate != null) {
            this.activeEndDateForDisplay = DateUtil.convertSqlDateToStringDate(activeEndDate);
        }
    }

    public String getActiveEndDateForDisplay() {
        return activeEndDateForDisplay;
    }

    public void setActiveEndDateForDisplay(String activeEndDateForDisplay) {
        this.activeEndDateForDisplay = activeEndDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.activeEndDate = DateUtil.convertStringDateToSqlDate(this.activeEndDateForDisplay, locale);
    }

    /**
     * This is descriptive only and should not be displayed to the end user
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Description of this test
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
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

    public String getIsReportable() {
        return isReportable;
    }

    public void setIsReportable(String isReportable) {
        this.isReportable = isReportable;
    }

    public String getLabelQuantity() {
        return labelQuantity;
    }

    public void setLabelQuantity(String labelQuantity) {
        this.labelQuantity = labelQuantity;
    }

    public String getLoinc() {
        return loinc;
    }

    public void setLoinc(String loinc) {
        this.loinc = loinc;
    }

    public String getStickerRequiredFlag() {
        return stickerRequiredFlag;
    }

    public void setStickerRequiredFlag(String stickerRequiredFlag) {
        this.stickerRequiredFlag = stickerRequiredFlag;
    }

    public String getTimeAverage() {
        return timeAverage;
    }

    public void setTimeAverage(String timeAverage) {
        this.timeAverage = timeAverage;
    }

    public String getTimeHolding() {
        return timeHolding;
    }

    public void setTimeHolding(String timeHolding) {
        this.timeHolding = timeHolding;
    }

    public String getTimeMax() {
        return timeMax;
    }

    public void setTimeMax(String timeMax) {
        this.timeMax = timeMax;
    }

    public String getTimeWait() {
        return timeWait;
    }

    public void setTimeWait(String timeWait) {
        this.timeWait = timeWait;
    }

    public String getTimeWarning() {
        return timeWarning;
    }

    public void setTimeWarning(String timeWarning) {
        this.timeWarning = timeWarning;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public String getTestTrailerName() {
        return testTrailerName;
    }

    public void setTestTrailerName(String testTrailerName) {
        this.testTrailerName = testTrailerName;
    }

    public String getTestSectionName() {
        return testSectionName;
    }

    public void setTestSectionName(String testSectionName) {
        this.testSectionName = testSectionName;
    }

    public String getScriptletName() {
        return scriptletName;
    }

    public void setScriptletName(String scriptletName) {
        this.scriptletName = scriptletName;
    }

    public void setMethod(Method method) {
        this.method.setValue(method);
    }

    public Method getMethod() {
        return (Method) this.method.getValue();
    }

    public void setLabel(Label label) {
        this.label.setValue(label);
    }

    public Label getLabel() {
        return (Label) this.label.getValue();
    }

    public void setTestTrailer(TestTrailer testTrailer) {
        this.testTrailer.setValue(testTrailer);
    }

    public TestTrailer getTestTrailer() {
        return (TestTrailer) this.testTrailer.getValue();
    }

    public void setTestSection(TestSection testSection) {
        this.testSection.setValue(testSection);
    }

    public TestSection getTestSection() {
        return (TestSection) this.testSection.getValue();
    }

    public Localization getLocalizedTestSectionName() {
        return (Localization) localizedTestSectionName.getValue();
    }

    public void setLocalizedTestSectionName(Localization localizedName) {
        this.localizedTestSectionName.setValue(localizedName);
    }

    public Localization getLocalizedReportingTestSectionName() {
        return (Localization) localizedReportingTestSectionName.getValue();
    }

    public void setLocalizedReportingTestSectionName(Localization localizedReportingName) {
        this.localizedReportingTestSectionName.setValue(localizedReportingName);
    }

    public void setScriptlet(Scriptlet scriptlet) {
        this.scriptlet.setValue(scriptlet);
    }

    public Scriptlet getScriptlet() {
        return (Scriptlet) this.scriptlet.getValue();
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return this.unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
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

    public void setAlternateTestDisplayValue(String alternateTestDisplayValue) {
        this.alternateTestDisplayValue = alternateTestDisplayValue;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return TestServiceImpl.getUserLocalizedTestName(this);
    }

    public void setLocalCode(String localCode) {
        this.localCode = localCode;
    }

    public String getLocalCode() {
        return localCode;
    }

    public Boolean getOrderable() {
        return orderable;
    }

    public void setOrderable(Boolean orderable) {
        this.orderable = orderable;
    }

    public Localization getLocalizedTestName() {
        return (Localization) localizedTestName.getValue();
    }

    public void setLocalizedTestName(Localization localizedName) {
        this.localizedTestName.setValue(localizedName);
    }

    public Localization getLocalizedReportingName() {
        return (Localization) localizedReportingName.getValue();
    }

    public void setLocalizedReportingName(Localization localizedReportingName) {
        this.localizedReportingName.setValue(localizedReportingName);
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Override
    public String getName() {
        return getLocalizedTestName().getLocalizedValue();
    }

    public TestResult getDefaultTestResult() {
        return (TestResult) defaultTestResult.getValue();
    }

    public void setDefaultTestResult(TestResult defaultTestResult) {
        this.defaultTestResult.setValue(defaultTestResult);
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

    public boolean isInLabOnly() {
        return inLabOnly;
    }

    public void setInLabOnly(boolean inLabOnly) {
        this.inLabOnly = inLabOnly;
    }

    public void setAntimicrobialResistance(Boolean antimicrobialResistance) {
        this.antimicrobialResistance = antimicrobialResistance;
    }

    public Boolean getAntimicrobialResistance() {
        return antimicrobialResistance;
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
