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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteServiceImpl.BoundTo;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "ANALYSIS")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Analysis extends BaseObject<String> implements NoteObject {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "analysis_seq_gen")
    @GenericGenerator(name = "analysis_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "analysis_seq"))
    @Column(name = "ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "fhir_uuid")
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SAMPITEM_ID")
    private SampleItem sampleItem;

    @Column(name = "ANALYSIS_TYPE", length = 10, nullable = false)
    private String analysisType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TEST_SECT_ID")
    private TestSection testSection;

    @Transient
    private String testSectionName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TEST_ID")
    private Test test;

    @Transient
    private String testName;

    @Column(name = "REVISION", precision = 22, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String revision;

    @Column(name = "STATUS", length = 3)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String status;

    @Column(name = "STARTED_DATE", length = 7)
    private Timestamp startedDate = null;

    @Transient
    private String startedDateForDisplay = null;

    @Column(name = "COMPLETED_DATE", length = 7)
    private Timestamp completedDate = null;

    @Column(name = "ENTRY_DATE", length = 7)
    private Timestamp enteredDate = null;

    @Transient
    private String completedDateForDisplay = null;

    @Column(name = "RELEASED_DATE", length = 7)
    private Timestamp releasedDate = null;

    @Transient
    private String releasedDateForDisplay = null;

    @Column(name = "PRINTED_DATE", length = 7)
    private Date printedDate = null;

    @Transient
    private String printedDateForDisplay = null;

    @Column(name = "IS_REPORTABLE", length = 1)
    private String isReportable;

    @Column(name = "SO_SEND_READY_DATE", length = 7)
    private Date soSendReadyDate = null;

    @Transient
    private String soSendReadyDateForDisplay = null;

    @Column(name = "SO_CLIENT_REFERENCE", length = 240)
    private String soClientReference;

    @Column(name = "SO_NOTIFY_RECEIVED_DATE", length = 7)
    private Date soNotifyReceivedDate = null;

    @Transient
    private String soNotifyReceivedDateForDisplay = null;

    @Column(name = "SO_NOTIFY_SEND_DATE", length = 7)
    private Date soNotifySendDate = null;

    @Transient
    private String soNotifySendDateForDisplay = null;

    @Column(name = "SO_SEND_DATE", length = 7)
    private Date soSendDate = null;

    @Transient
    private String soSendDateForDisplay = null;

    @Column(name = "SO_SEND_ENTRY_BY", length = 240)
    private String soSendEntryBy;

    @Column(name = "SO_SEND_ENTRY_DATE", length = 7)
    private Date soSendEntryDate = null;

    @Transient
    private String soSendEntryDateForDisplay = null;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PARENT_ANALYSIS_ID")
    private Analysis parentAnalysis;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PARENT_RESULT_ID")
    private Result parentResult;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "panel_id")
    private Panel panel;

    /** Mutually exclusive with {@link #sampleItem} (DB CHECK constraint). */
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "vector_pool_id", precision = 10, scale = 0)
    private String vectorPoolId;

    @Column(name = "reflex_trigger")
    private Boolean triggeredReflex = false;

    @Column(name = "result_calculated")
    private Boolean resultCalculated = false;

    @Column(name = "status_id", precision = 10)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String statusId;

    @Transient
    private String assignedSortedTestTreeDisplayValue;

    @Column(name = "referred_out")
    private boolean referredOut = false;

    @Column(name = "type_of_sample_name")
    private String sampleTypeName;

    @Transient
    private List<Analysis> children;

    @Column(name = "corrected")
    private boolean correctedSincePatientReport;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "method_id")
    private Method method;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "result_file_id", unique = true, nullable = true)
    private ResultFile resultFile;

    @Column(name = "analyzer_id", precision = 10)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analyzerId;

    public Analysis() {
        super();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;

        if (GenericValidator.isBlankOrNull(sampleTypeName) && sampleItem != null
                && sampleItem.getTypeOfSample() != null) {
            setSampleTypeName(sampleItem.getTypeOfSample().getLocalizedName());
        }
    }

    public void setCompletedDate(Timestamp completedDate) {
        this.completedDate = completedDate;
        updateCompletedDateForDisplay();
    }

    private void updateCompletedDateForDisplay() {
        this.completedDateForDisplay = this.completedDate != null
                ? DateUtil.convertSqlDateToStringDate(new Date(this.completedDate.getTime()))
                : null;
    }

    @PostLoad
    private void postLoad() {
        updateCompletedDateForDisplay();
    }

    /** @deprecated Use {@link #setCompletedDate(Timestamp)} instead */
    @Deprecated
    @JsonIgnore
    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate != null ? new Timestamp(completedDate.getTime()) : null;
        completedDateForDisplay = DateUtil.convertSqlDateToStringDate(completedDate);
    }

    public void setCompletedDateForDisplay(String completedDateForDisplay) {
        this.completedDateForDisplay = completedDateForDisplay;
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        Date sqlDate = DateUtil.convertStringDateToSqlDate(this.completedDateForDisplay, locale);
        completedDate = sqlDate != null ? new Timestamp(sqlDate.getTime()) : null;
    }

    @JsonSetter
    public void setStartedDate(Timestamp startedDate) {
        this.startedDate = startedDate;
        startedDateForDisplay = startedDate != null
                ? DateUtil.convertSqlDateToStringDate(new Date(startedDate.getTime()))
                : null;
    }

    /** @deprecated Use {@link #setStartedDate(Timestamp)} instead */
    @Deprecated
    @JsonIgnore
    public void setStartedDate(Date startedDate) {
        this.startedDate = startedDate != null ? new Timestamp(startedDate.getTime()) : null;
        startedDateForDisplay = DateUtil.convertSqlDateToStringDate(startedDate);
    }

    public void setStartedDateForDisplay(String startedDateForDisplay) {
        this.startedDateForDisplay = startedDateForDisplay;
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        Date sqlDate = DateUtil.convertStringDateToSqlDate(this.startedDateForDisplay, locale);
        startedDate = sqlDate != null ? new Timestamp(sqlDate.getTime()) : null;
    }

    public void setPrintedDate(Date printedDate) {
        this.printedDate = printedDate;
        printedDateForDisplay = DateUtil.convertSqlDateToStringDate(printedDate);
    }

    public void setPrintedDateForDisplay(String printedDateForDisplay) {
        this.printedDateForDisplay = printedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        printedDate = DateUtil.convertStringDateToSqlDate(this.printedDateForDisplay, locale);
    }

    @JsonSetter
    public void setReleasedDate(Timestamp releasedDate) {
        this.releasedDate = releasedDate;
        releasedDateForDisplay = releasedDate != null
                ? DateUtil.convertSqlDateToStringDate(new Date(releasedDate.getTime()))
                : null;
    }

    /** @deprecated Use {@link #setReleasedDate(Timestamp)} instead */
    @Deprecated
    @JsonIgnore
    public void setReleasedDate(Date releasedDate) {
        this.releasedDate = releasedDate != null ? new Timestamp(releasedDate.getTime()) : null;
        releasedDateForDisplay = DateUtil.convertSqlDateToStringDate(releasedDate);
    }

    public void setReleasedDateForDisplay(String releasedDateForDisplay) {
        this.releasedDateForDisplay = releasedDateForDisplay;
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        Date sqlDate = DateUtil.convertStringDateToSqlDate(this.releasedDateForDisplay, locale);
        releasedDate = sqlDate != null ? new Timestamp(sqlDate.getTime()) : null;
    }

    public void setSoNotifyReceivedDate(Date soNotifyReceivedDate) {
        this.soNotifyReceivedDate = soNotifyReceivedDate;
        soNotifyReceivedDateForDisplay = DateUtil.convertSqlDateToStringDate(soNotifyReceivedDate);
    }

    public void setSoNotifyReceivedDateForDisplay(String soNotifyReceivedDateForDisplay) {
        this.soNotifyReceivedDateForDisplay = soNotifyReceivedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soNotifyReceivedDate = DateUtil.convertStringDateToSqlDate(this.soNotifyReceivedDateForDisplay, locale);
    }

    public void setSoNotifySendDate(Date soNotifySendDate) {
        this.soNotifySendDate = soNotifySendDate;
        soNotifySendDateForDisplay = DateUtil.convertSqlDateToStringDate(soNotifySendDate);
    }

    public void setSoNotifySendDateForDisplay(String soNotifySendDateForDisplay) {
        this.soNotifySendDateForDisplay = soNotifySendDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soNotifySendDate = DateUtil.convertStringDateToSqlDate(this.soNotifySendDateForDisplay, locale);
    }

    public void setSoSendDate(Date soSendDate) {
        this.soSendDate = soSendDate;
        soSendDateForDisplay = DateUtil.convertSqlDateToStringDate(soSendDate);
    }

    public void setSoSendDateForDisplay(String soSendDateForDisplay) {
        this.soSendDateForDisplay = soSendDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soSendDate = DateUtil.convertStringDateToSqlDate(this.soSendDateForDisplay, locale);
    }

    public void setSoSendEntryDate(Date soSendEntryDate) {
        this.soSendEntryDate = soSendEntryDate;
        soSendEntryDateForDisplay = DateUtil.convertSqlDateToStringDate(soSendEntryDate);
    }

    public void setSoSendEntryDateForDisplay(String soSendEntryDateForDisplay) {
        this.soSendEntryDateForDisplay = soSendEntryDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soSendEntryDate = DateUtil.convertStringDateToSqlDate(this.soSendEntryDateForDisplay, locale);
    }

    public void setSoSendReadyDate(Date soSendReadyDate) {
        this.soSendReadyDate = soSendReadyDate;
        soSendReadyDateForDisplay = DateUtil.convertSqlDateToStringDate(soSendReadyDate);
    }

    public void setSoSendReadyDateForDisplay(String soSendReadyDateForDisplay) {
        this.soSendReadyDateForDisplay = soSendReadyDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        soSendReadyDate = DateUtil.convertStringDateToSqlDate(this.soSendReadyDateForDisplay, locale);
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
}
