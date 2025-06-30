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
package org.openelisglobal.sampleqaeventaction.valueholder;

import java.sql.Date;
import org.openelisglobal.action.valueholder.Action;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * @author benzd1 bugzilla 2510
 */
public class SampleQaEventAction extends BaseObject<String> {

    private String id;

    private String sampleQaEventId;

    private ValueHolderInterface sampleQaEvent;

    private String actionId;

    private ValueHolderInterface action;

    private Date createdDate;

    private String createdDateForDisplay;

    // bugzilla 2481
    private SystemUser systemUser;

    private String systemUserId;

    public SampleQaEventAction() {
        super();
        this.action = new ValueHolder();
        this.sampleQaEvent = new ValueHolder();
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

    // SAMPLE_QA_EVENT
    public SampleQaEvent getSampleQaEvent() {
        return (SampleQaEvent) this.sampleQaEvent.getValue();
    }

    public void setSampleQaEvent(ValueHolderInterface sampleQaEvent) {
        this.sampleQaEvent = sampleQaEvent;
    }

    public void setSampleQaEvent(SampleQaEvent sampleQaEvent) {
        this.sampleQaEvent.setValue(sampleQaEvent);
    }

    protected ValueHolderInterface getSampleQaEventHolder() {
        return this.sampleQaEvent;
    }

    protected void setSampleQaEventHolder(ValueHolderInterface sampleQaEvent) {
        this.sampleQaEvent = sampleQaEvent;
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

    public String getSampleQaEventId() {
        return sampleQaEventId;
    }

    public void setSampleQaEventId(String sampleQaEventId) {
        this.sampleQaEventId = sampleQaEventId;
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
