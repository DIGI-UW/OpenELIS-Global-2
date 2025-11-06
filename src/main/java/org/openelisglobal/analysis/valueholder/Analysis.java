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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analysis.valueholder;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteServiceImpl.BoundTo;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;

public class Analysis extends BaseObject<String> implements NoteObject {

    private static final long serialVersionUID = 1L;

    private String id;
    private UUID fhirUuid;
    private ValueHolderInterface sampleItem;
    private String analysisType;
    private ValueHolderInterface testSection;
    private String testSectionName;
    private ValueHolderInterface test;
    private String testName;
    private String revision;
    private String status;
    private Date startedDate = null;
    private String startedDateForDisplay = null;
    private Date completedDate = null;
    private Timestamp enteredDate = null;
    private String completedDateForDisplay = null;
    private Date releasedDate = null;
    private String releasedDateForDisplay = null;
    private Date printedDate = null;
    private String printedDateForDisplay = null;
    private String isReportable;
    private Date soSendReadyDate = null;
    private String soSendReadyDateForDisplay = null;
    private String soClientReference;
    private Date soNotifyReceivedDate = null;
    private String soNotifyReceivedDateForDisplay = null;
    private Date soNotifySendDate = null;
    private String soNotifySendDateForDisplay = null;
    private Date soSendDate = null;
    private String soSendDateForDisplay = null;
    private String soSendEntryBy;
    private Date soSendEntryDate = null;
    private String soSendEntryDateForDisplay = null;
    private ValueHolderInterface parentAnalysis;
    private ValueHolderInterface parentResult;
    private ValueHolderInterface panel;
    private Boolean triggeredReflex = false;
    private Boolean resultCalculated = false;
    private String statusId;
    private String assignedSortedTestTreeDisplayValue;
    private boolean referredOut = false;
    private String sampleTypeName;
    private List<Analysis> children;
    private boolean correctedSincePatientReport;
    private ValueHolderInterface method;

    public Analysis() {
        super();
        sampleItem = new ValueHolder();
        testSection = new ValueHolder();
        test = new ValueHolder();
        parentAnalysis = new ValueHolder();
        parentResult = new ValueHolder();
        panel = new ValueHolder();
        method = new ValueHolder();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getSampleTypeName() {
        return sampleTypeName;
    }

    public void setSampleTypeName(String sampleTypeName) {
        this.sampleTypeName = sampleTypeName;
    }

    public String getAssignedSortedTestTreeDisplayValue() {
        return assignedSortedTestTreeDisplayValue;
    }

    public List<Analysis> getChildren() {
        return children;
    }

    public void setChildren(List<Analysis> children) {
        this.children = children;
    }

    public void setAssignedSortedTestTreeDisplayValue(String assignedSortedTestTreeDisplayValue) {
        this.assignedSortedTestTreeDisplayValue = assignedSortedTestTreeDisplayValue;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public SampleItem getSampleItem() {
        return (SampleItem) sampleItem.getValue();
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem.setValue(sampleItem);

        if (GenericValidator.isBlankOrNull(sampleTypeName) && sampleItem != null
                && sampleItem.getTypeOfSample() != null) {
            setSampleTypeName(sampleItem.getTypeOfSample().getLocalizedName());
        }
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
        completedDateForDisplay = DateUtil.convertSqlDateToStringDate(completedDate);
    }

    public String getCompletedDateForDisplay() {
        return completedDateForDisplay;
    }

    public void setCompletedDateForDisplay(String completedDateForDisplay) {
        this.completedDateForDisplay = completedDateForDisplay;

        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        completedDate = DateUtil.convertStringDateToSqlDate(this.completedDateForDisplay, locale);
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public Date getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(Date startedDate) {
        this.startedDate = startedDate;
        startedDateForDisplay = DateUtil.convertSqlDateToStringDate(startedDate);
    }

    public String getStartedDateForDisplay() {
        return startedDateForDisplay;
    }

    public void setStartedDateForDisplay(String startedDateForDisplay) {
        this.startedDateForDisplay = startedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        startedDate = DateUtil.convertStringDateToSqlDate(this.startedDateForDisplay, locale);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIsReportable() {
        return isReportable;
    }

    public void setIsReportable(String isReportable) {
        this.isReportable = isReportable;
    }

    public Date getPrintedDate() {
        return printedDate;
    }

    public void setPrintedDate(Date printedDate) {
        this.printedDate = printedDate;
        printedDateForDisplay = DateUtil.convertSqlDateToStringDate(printedDate);
    }

    public String getPrintedDateForDisplay() {
        return printedDateForDisplay;
    }

    public void setPrintedDateForDisplay(String printedDateForDisplay) {
        this.printedDateForDisplay = printedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        printedDate = DateUtil.convertStringDateToSqlDate(this.printedDateForDisplay, locale);
    }

    public Date getReleasedDate() {
        return releasedDate;
    }

    public void setReleasedDate(Date releasedDate) {
        this.releasedDate = releasedDate;
        releasedDateForDisplay = DateUtil.convertSqlDateToStringDate(releasedDate);
    }

    public String getReleasedDateForDisplay() {
        return releasedDateForDisplay;
    }

    public void setReleasedDateForDisplay(String releasedDateForDisplay) {
        this.releasedDateForDisplay = releasedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        releasedDate = DateUtil.convertStringDateToSqlDate(this.releasedDateForDisplay, locale);
    }

    public String getSoClientReference() {
        return soClientReference;
    }

    public void setSoClientReference(String soClientReference) {
        this.soClientReference = soClientReference;
    }

    public Date getSoNotifyReceivedDate() {
        return soNotifyReceivedDate;
    }

    public void setSoNotifyReceivedDate(Date soNotifyReceivedDate) {
        this.soNotifyReceivedDate = soNotifyReceivedDate;
        soNotifyReceivedDateForDisplay = DateUtil.convertSqlDateToStringDate(soNotifyReceivedDate);
    }

    public String getSoNotifyReceivedDateForDisplay() {
        return soNotifyReceivedDateForDisplay;
    }

    public void setSoNotifyReceivedDateForDisplay(String soNotifyReceivedDateForDisplay) {
        this.soNotifyReceivedDateForDisplay = soNotifyReceivedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soNotifyReceivedDate = DateUtil.convertStringDateToSqlDate(this.soNotifyReceivedDateForDisplay, locale);
    }

    public Date getSoNotifySendDate() {
        return soNotifySendDate;
    }

    public void setSoNotifySendDate(Date soNotifySendDate) {
        this.soNotifySendDate = soNotifySendDate;
        soNotifySendDateForDisplay = DateUtil.convertSqlDateToStringDate(soNotifySendDate);
    }

    public String getSoNotifySendDateForDisplay() {
        return soNotifySendDateForDisplay;
    }

    public void setSoNotifySendDateForDisplay(String soNotifySendDateForDisplay) {
        this.soNotifySendDateForDisplay = soNotifySendDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soNotifySendDate = DateUtil.convertStringDateToSqlDate(this.soNotifySendDateForDisplay, locale);
    }

    public Date getSoSendDate() {
        return soSendDate;
    }

    public void setSoSendDate(Date soSendDate) {
        this.soSendDate = soSendDate;
        soSendDateForDisplay = DateUtil.convertSqlDateToStringDate(soSendDate);
    }

    public String getSoSendDateForDisplay() {
        return soSendDateForDisplay;
    }

    public void setSoSendDateForDisplay(String soSendDateForDisplay) {
        this.soSendDateForDisplay = soSendDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soSendDate = DateUtil.convertStringDateToSqlDate(this.soSendDateForDisplay, locale);
    }

    public String getSoSendEntryBy() {
        return soSendEntryBy;
    }

    public void setSoSendEntryBy(String soSendEntryBy) {
        this.soSendEntryBy = soSendEntryBy;
    }

    public Date getSoSendEntryDate() {
        return soSendEntryDate;
    }

    public void setSoSendEntryDate(Date soSendEntryDate) {
        this.soSendEntryDate = soSendEntryDate;
        soSendEntryDateForDisplay = DateUtil.convertSqlDateToStringDate(soSendEntryDate);
    }

    public String getSoSendEntryDateForDisplay() {
        return soSendEntryDateForDisplay;
    }

    public void setSoSendEntryDateForDisplay(String soSendEntryDateForDisplay) {
        this.soSendEntryDateForDisplay = soSendEntryDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soSendEntryDate = DateUtil.convertStringDateToSqlDate(this.soSendEntryDateForDisplay, locale);
    }

    public Date getSoSendReadyDate() {
        return soSendReadyDate;
    }

    public void setSoSendReadyDate(Date soSendReadyDate) {
        this.soSendReadyDate = soSendReadyDate;
        soSendReadyDateForDisplay = DateUtil.convertSqlDateToStringDate(soSendReadyDate);
    }

    public String getSoSendReadyDateForDisplay() {
        return soSendReadyDateForDisplay;
    }

    public void setSoSendReadyDateForDisplay(String soSendReadyDateForDisplay) {
        this.soSendReadyDateForDisplay = soSendReadyDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soSendReadyDate = DateUtil.convertStringDateToSqlDate(this.soSendReadyDateForDisplay, locale);
    }

    public TestSection getTestSection() {
        return (TestSection) testSection.getValue();
    }

    public void setTestSection(TestSection testSection) {
        this.testSection.setValue(testSection);
    }

    public Test getTest() {
        return (Test) test.getValue();
    }

    public void setTest(Test test) {
        this.test.setValue(test);
    }

    public String getTestSectionName() {
        return testSectionName;
    }

    public void setTestSectionName(String testSectionName) {
        this.testSectionName = testSectionName;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public Analysis getParentAnalysis() {
        return (Analysis) parentAnalysis.getValue();
    }

    public void setParentAnalysis(Analysis parentAnalysis) {
        this.parentAnalysis.setValue(parentAnalysis);
    }

    public Result getParentResult() {
        return (Result) parentResult.getValue();
    }

    public void setParentResult(Result parentResult) {
        this.parentResult.setValue(parentResult);
    }

    public void setTriggeredReflex(Boolean triggeredReflex) {
        this.triggeredReflex = triggeredReflex;
    }

    public Boolean getTriggeredReflex() {
        return triggeredReflex;
    }

    public Boolean getResultCalculated() {
        return resultCalculated;
    }

    public void setResultCalculated(Boolean resultCalculated) {
        this.resultCalculated = resultCalculated;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setEnteredDate(Timestamp enteredDate) {
        this.enteredDate = enteredDate;
    }

    public Timestamp getEnteredDate() {
        return enteredDate;
    }

    public Panel getPanel() {
        return (Panel) panel.getValue();
    }

    public void setPanel(Panel panel) {
        this.panel.setValue(panel);
    }

    public boolean isReferredOut() {
        return referredOut;
    }

    public void setReferredOut(boolean referredOut) {
        this.referredOut = referredOut;
    }

    public boolean isCorrectedSincePatientReport() {
        return correctedSincePatientReport;
    }

    public void setCorrectedSincePatientReport(boolean correctedSincePatientReport) {
        this.correctedSincePatientReport = correctedSincePatientReport;
    }

    @Override
    public String getTableId() {
        return AnalysisServiceImpl.getTableReferenceId();
    }

    @Override
    public String getObjectId() {
        return getId();
    }

    @Override
    public BoundTo getBoundTo() {
        return BoundTo.ANALYSIS;
    }

    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public Method getMethod() {
        return (Method) method.getValue();
    }

    public void setMethod(Method method) {
        this.method.setValue(method);
    }
}
