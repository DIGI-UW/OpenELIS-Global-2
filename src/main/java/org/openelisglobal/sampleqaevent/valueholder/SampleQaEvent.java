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
package org.openelisglobal.sampleqaevent.valueholder;

import java.sql.Date;
import java.sql.Timestamp;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteServiceImpl.BoundTo;
import org.openelisglobal.qaevent.valueholder.QaEvent;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * @author benzd1 bugzilla 2510
 */
public class SampleQaEvent extends BaseObject<String> implements NoteObject {

    /** */
    private static final long serialVersionUID = 1L;

    private String id;

    private String qaEventId;

    private ValueHolderInterface qaEvent;

    private String sampleId;

    private ValueHolderInterface sample;

    private String sampleItemId;

    private ValueHolderInterface sampleItem;

    private Date completedDate;

    private Timestamp enteredDate;

    private String completedDateForDisplay;

    public SampleQaEvent() {
        sample = new ValueHolder();
        sampleItem = new ValueHolder();
        qaEvent = new ValueHolder();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public Sample getSample() {
        return (Sample) sample.getValue();
    }

    public void setSample(Sample sample) {
        this.sample.setValue(sample);
    }

    public SampleItem getSampleItem() {
        return (SampleItem) sampleItem.getValue();
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem.setValue(sampleItem);
    }

    public QaEvent getQaEvent() {
        return (QaEvent) qaEvent.getValue();
    }

    public void setQaEvent(QaEvent qaEvent) {
        this.qaEvent.setValue(qaEvent);
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getQaEventId() {
        return qaEventId;
    }

    public void setQaEventId(String qaEventId) {
        this.qaEventId = qaEventId;
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
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        completedDate = DateUtil.convertStringDateToSqlDate(completedDateForDisplay, locale);
    }

    public Timestamp getEnteredDate() {
        return enteredDate;
    }

    public void setEnteredDate(Timestamp enteredDate) {
        this.enteredDate = enteredDate;
    }

    @Override
    public String getTableId() {
        return QAService.TABLE_REFERENCE_ID;
    }

    @Override
    public String getObjectId() {
        return getId();
    }

    @Override
    public BoundTo getBoundTo() {
        return BoundTo.QA_EVENT;
    }
}
