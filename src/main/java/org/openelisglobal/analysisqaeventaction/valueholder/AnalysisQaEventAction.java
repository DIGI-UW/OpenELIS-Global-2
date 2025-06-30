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
package org.openelisglobal.analysisqaeventaction.valueholder;

import java.sql.Date;
import org.openelisglobal.action.valueholder.Action;
import org.openelisglobal.analysisqaevent.valueholder.AnalysisQaEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public class AnalysisQaEventAction extends BaseObject<String> {

    private String id;

    private String analysisQaEventId;

    private ValueHolderInterface analysisQaEvent;

    private String actionId;

    private ValueHolderInterface action;

    private Date createdDate;

    private String createdDateForDisplay;

    // bugzilla 2481
    private SystemUser systemUser;

    private String systemUserId;

    public AnalysisQaEventAction() {
        super();
        this.action = new ValueHolder();
        this.analysisQaEvent = new ValueHolder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    // Action
    public Action getAction() {
        return (Action) this.action.getValue();
    }

    public void setAction(ValueHolderInterface action) {
        this.action = action;
    }

    public void setAction(Action action) {
        this.action.setValue(action);
    }

    protected ValueHolderInterface getActionHolder() {
        return this.action;
    }

    protected void setActionHolder(ValueHolderInterface action) {
        this.action = action;
    }

    // ANALYSIS_QA_EVENT
    public AnalysisQaEvent getAnalysisQaEvent() {
        return (AnalysisQaEvent) this.analysisQaEvent.getValue();
    }

    public void setAnalysisQaEvent(ValueHolderInterface analysisQaEvent) {
        this.analysisQaEvent = analysisQaEvent;
    }

    public void setAnalysisQaEvent(AnalysisQaEvent analysisQaEvent) {
        this.analysisQaEvent.setValue(analysisQaEvent);
    }

    protected ValueHolderInterface getAnalysisQaEventHolder() {
        return this.analysisQaEvent;
    }

    protected void setAnalysisQaEventHolder(ValueHolderInterface analysisQaEvent) {
        this.analysisQaEvent = analysisQaEvent;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
        this.createdDateForDisplay = DateUtil.convertSqlDateToStringDate(createdDate);
    }

    public String getCreatedDateForDisplay() {
        return this.createdDateForDisplay;
    }

    public void setCreatedDateForDisplay(String createdDateForDisplay) {
        this.createdDateForDisplay = createdDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.createdDate = DateUtil.convertStringDateToSqlDate(createdDateForDisplay, locale);
    }

    public String getAnalysisQaEventId() {
        return analysisQaEventId;
    }

    public void setAnalysisQaEventId(String analysisQaEventId) {
        this.analysisQaEventId = analysisQaEventId;
    }

    public void setSystemUser(SystemUser systemUser) {
        this.systemUser = systemUser;
    }

    public SystemUser getSystemUser() {
        return this.systemUser;
    }

    public String getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(String systemUserId) {
        this.systemUserId = systemUserId;
    }
}
