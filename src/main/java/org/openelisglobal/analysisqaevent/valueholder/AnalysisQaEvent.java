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
package org.openelisglobal.analysisqaevent.valueholder;

import java.sql.Date;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.qaevent.valueholder.QaEvent;

public class AnalysisQaEvent extends BaseObject<String> {

    private String id;

    private String qaEventId;

    private ValueHolderInterface qaEvent;

    private String analysisId;

    private ValueHolderInterface analysis;

    private Date completedDate;

    private String completedDateForDisplay;

    private String analysisQaEventDisplayValue;

    public AnalysisQaEvent() {
        super();
        this.analysis = new ValueHolder();
        this.qaEvent = new ValueHolder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    // ANALYSIS
    public Analysis getAnalysis() {
        return (Analysis) this.analysis.getValue();
    }

    public void setAnalysis(ValueHolderInterface analysis) {
        this.analysis = analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis.setValue(analysis);
    }

    protected ValueHolderInterface getAnalysisHolder() {
        return this.analysis;
    }

    protected void setAnalysisHolder(ValueHolderInterface analysis) {
        this.analysis = analysis;
    }

    // QA_EVENT
    public QaEvent getQaEvent() {
        return (QaEvent) this.qaEvent.getValue();
    }

    public void setQaEvent(ValueHolderInterface qaEvent) {
        this.qaEvent = qaEvent;
    }

    public void setQaEvent(QaEvent qaEvent) {
        this.qaEvent.setValue(qaEvent);
    }

    protected ValueHolderInterface getQaEventHolder() {
        return this.qaEvent;
    }

    protected void setQaEventHolder(ValueHolderInterface qaEvent) {
        this.qaEvent = qaEvent;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
        this.completedDateForDisplay = DateUtil.convertSqlDateToStringDate(completedDate);
    }

    public String getCompletedDateForDisplay() {
        return completedDateForDisplay;
    }

    public void setCompletedDateForDisplay(String completedDateForDisplay) {
        this.completedDateForDisplay = completedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.completedDate = DateUtil.convertStringDateToSqlDate(completedDateForDisplay, locale);
    }

    public String getQaEventId() {
        return qaEventId;
    }

    public void setQaEventId(String qaEventId) {
        this.qaEventId = qaEventId;
    }

    public String getAnalysisQaEventDisplayValue() {
        if (analysis != null && qaEvent != null) {
            Analysis analysis = getAnalysis();
            String testDisplayValue = analysis.getTest().getTestDisplayValue();
            QaEvent qaEvent = getQaEvent();
            String qaEventDisplayValue = qaEvent.getQaEventDisplayValue();
            analysisQaEventDisplayValue = testDisplayValue + " | " + qaEventDisplayValue;
        } else {
            analysisQaEventDisplayValue = "NO VALUES AVAILABLE";
        }
        return analysisQaEventDisplayValue;
    }
}
