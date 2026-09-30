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
package org.openelisglobal.sample.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteServiceImpl.BoundTo;
import org.openelisglobal.sample.service.SampleServiceImpl;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "SAMPLE")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Sample extends EnumValueItemImpl implements NoteObject {

    private static final long serialVersionUID = 1407388492068629053L;

    @Id
    @GeneratedValue(generator = "sample_seq_gen")
    @GenericGenerator(name = "sample_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "sample_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "fhir_uuid")
    private UUID fhirUuid;

    @Column(name = "ACCESSION_NUMBER", precision = 20, nullable = false, unique = true)
    private String accessionNumber;

    @Column(name = "PACKAGE_ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String packageId;

    @Column(name = "DOMAIN", length = 1)
    private String domain;

    @Column(name = "NEXT_ITEM_SEQUENCE", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String nextItemSequence;

    // S-09 (OGC-580) Resample linkage: original <-> replacement order
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "resampled_from_sample_id", precision = 10)
    private String resampledFromSampleId;

    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "resampled_to_sample_id", precision = 10)
    private String resampledToSampleId;

    // OGC-776 (S-15e) LHU report-level amendment

    @Column(name = "amends_lhu_number")
    private String amendsLhuNumber;

    @Column(name = "amendment_number")
    private Integer amendmentNumber;

    @Column(name = "amendment_reason")
    private String amendmentReason;

    @Column(name = "REVISION", precision = 22, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String revision;

    @Column(name = "ENTERED_DATE", length = 7, nullable = false)
    private Date enteredDate;

    @Transient
    private String enteredDateForDisplay;

    @Column(name = "RECEIVED_DATE", length = 7, nullable = false)
    private Timestamp receivedTimestamp;

    @Transient
    private String receivedDateForDisplay;

    @Transient
    private String receivedTimeForDisplay;

    @Column(name = "SPEC_OR_ISOLATE", length = 1)
    private String referredCultureFlag;

    @Column(name = "COLLECTION_DATE", length = 7)
    private Timestamp collectionDate;

    @Transient
    private String collectionDateForDisplay;

    @Transient
    private String collectionTimeForDisplay;

    @Column(name = "CLIENT_REFERENCE", length = 20)
    private String clientReference;

    @Column(name = "required_by")
    private Timestamp requiredBy;

    @Column(name = "STATUS", length = 1)
    private String status;

    @Column(name = "RELEASED_DATE", length = 7)
    private Date releasedDate;

    @Transient
    private String releasedDateForDisplay;

    @Column(name = "STICKER_RCVD_FLAG", length = 1)
    private String stickerReceivedFlag;

    @Column(name = "SYS_USER_ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sysUserId;

    @Column(name = "BARCODE", length = 20)
    private String barCode;

    @Column(name = "TRANSMISSION_DATE", length = 7)
    private Date transmissionDate;

    @Transient
    private String transmissionDateForDisplay;

    @Transient
    private ValueHolderInterface systemUser;

    @Column(name = "referring_id")
    private String referringId;

    @Column(name = "clinical_order_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String clinicalOrderId;

    @Column(name = "is_confirmation")
    private Boolean isConfirmation = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_PRIORITY")
    private OrderPriority priority;

    @Column(name = "gps_latitude")
    private Double gpsLatitude;

    @Column(name = "gps_longitude")
    private Double gpsLongitude;

    @Column(name = "gps_accuracy_meters")
    private Integer gpsAccuracyMeters;

    @Column(name = "gps_capture_method", length = 10)
    private String gpsCaptureMethod;

    @Column(name = "gps_capture_timestamp")
    private Timestamp gpsCaptureTimestamp;

    @Column(name = "storage_skipped")
    private Boolean storageSkipped = false;

    @Column(name = "consent_provided")
    private Boolean consentGiven = false;

    @Column(name = "consent_reference_no", length = 100)
    private String consentFormReference;

    @Column(name = "consent_recorded_at")
    private Timestamp consentRecordedAt;

    @Column(name = "consent_recorded_by", length = 225)
    private String consentRecordedBy;

    // testing one-to-many
    // this is for HSE I and II - ability to enter up to two projects
    @Transient
    private List sampleProjects;

    @Column(name = "status_id", precision = 10)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String statusId;

    public Sample() {
        super();
        systemUser = new ValueHolder();
        sampleProjects = new ArrayList();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getClientReference() {
        return clientReference;
    }

    public void setClientReference(String clientReference) {
        this.clientReference = clientReference;
    }

    public Timestamp getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Timestamp collectionDate) {
        this.collectionDate = collectionDate;
        collectionDateForDisplay = DateUtil.convertTimestampToStringDate(collectionDate);
        collectionTimeForDisplay = DateUtil.convertTimestampToStringTime(collectionDate);
    }

    public Timestamp getRequiredBy() {
        return requiredBy;
    }

    public void setRequiredBy(Timestamp requiredBy) {
        this.requiredBy = requiredBy;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getResampledFromSampleId() {
        return resampledFromSampleId;
    }

    public void setResampledFromSampleId(String resampledFromSampleId) {
        this.resampledFromSampleId = resampledFromSampleId;
    }

    public String getResampledToSampleId() {
        return resampledToSampleId;
    }

    public void setResampledToSampleId(String resampledToSampleId) {
        this.resampledToSampleId = resampledToSampleId;
    }

    public String getAmendsLhuNumber() {
        return amendsLhuNumber;
    }

    public void setAmendsLhuNumber(String amendsLhuNumber) {
        this.amendsLhuNumber = amendsLhuNumber;
    }

    public Integer getAmendmentNumber() {
        return amendmentNumber;
    }

    public void setAmendmentNumber(Integer amendmentNumber) {
        this.amendmentNumber = amendmentNumber;
    }

    public String getAmendmentReason() {
        return amendmentReason;
    }

    public void setAmendmentReason(String amendmentReason) {
        this.amendmentReason = amendmentReason;
    }

    public Date getEnteredDate() {
        return enteredDate;
    }

    public void setEnteredDate(Date enteredDate) {
        this.enteredDate = enteredDate;
        enteredDateForDisplay = DateUtil.convertSqlDateToStringDate(enteredDate);
    }

    public String getNextItemSequence() {
        return nextItemSequence;
    }

    public void setNextItemSequence(String nextItemSequence) {
        this.nextItemSequence = nextItemSequence;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public Date getReceivedDate() {
        return receivedTimestamp != null ? DateUtil.convertTimestampToSqlDate(receivedTimestamp) : null;
    }

    public void setReceivedDate(Date receivedDate) {
        receivedDateForDisplay = DateUtil.convertSqlDateToStringDate(receivedDate);
        receivedTimestamp = DateUtil.convertSqlDateToTimestamp(receivedDate);
    }

    public String getReceivedTimeForDisplay() {
        return receivedTimestamp != null ? DateUtil.convertTimestampToStringConfiguredHourTime(receivedTimestamp)
                : null;
    }

    public String getReceived24HourTimeForDisplay() {
        return receivedTimestamp != null ? DateUtil.convertTimestampToStringHourTime(receivedTimestamp) : null;
    }

    public String getReferredCultureFlag() {
        return referredCultureFlag;
    }

    public void setReferredCultureFlag(String referredCultureFlag) {
        this.referredCultureFlag = referredCultureFlag;
    }

    public Date getReleasedDate() {
        return releasedDate;
    }

    public void setReleasedDate(Date releasedDate) {
        this.releasedDate = releasedDate;
        releasedDateForDisplay = DateUtil.convertSqlDateToStringDate(releasedDate);
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStickerReceivedFlag() {
        return stickerReceivedFlag;
    }

    public void setStickerReceivedFlag(String stickerReceivedFlag) {
        this.stickerReceivedFlag = stickerReceivedFlag;
    }

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public SystemUser getSystemUser() {
        return (SystemUser) systemUser.getValue();
    }

    protected ValueHolderInterface getSystemUserHolder() {
        return systemUser;
    }

    public void setSystemUser(SystemUser systemUser) {
        this.systemUser.setValue(systemUser);
    }

    protected void setSystemUserHolder(ValueHolderInterface systemUser) {
        this.systemUser = systemUser;
    }

    public Date getTransmissionDate() {
        return transmissionDate;
    }

    public void setTransmissionDate(Date transmissionDate) {
        this.transmissionDate = transmissionDate;
        transmissionDateForDisplay = DateUtil.convertSqlDateToStringDate(transmissionDate);
    }

    public String getCollectionDateForDisplay() {
        if (GenericValidator.isBlankOrNull(collectionDateForDisplay)) {
            return collectionDate != null ? DateUtil.convertTimestampToStringDate(collectionDate) : null;
        }
        return collectionDateForDisplay;
    }

    public void setCollectionDateForDisplay(String collectionDateForDisplay) {
        this.collectionDateForDisplay = collectionDateForDisplay;
        collectionDate = DateUtil.convertStringDateToTruncatedTimestamp(collectionDateForDisplay);
    }

    public String getEnteredDateForDisplay() {
        if (GenericValidator.isBlankOrNull(enteredDateForDisplay)) {
            return enteredDate != null ? DateUtil.convertSqlDateToStringDate(enteredDate) : null;
        }
        return enteredDateForDisplay;
    }

    public void setEnteredDateForDisplay(String enteredDateForDisplay) {
        this.enteredDateForDisplay = enteredDateForDisplay;
        enteredDate = DateUtil.convertStringDateToSqlDate(enteredDateForDisplay);
    }

    public String getReceivedDateForDisplay() {
        if (GenericValidator.isBlankOrNull(receivedDateForDisplay)) {
            return receivedTimestamp != null ? DateUtil.convertTimestampToStringDate(receivedTimestamp) : null;
        }
        return receivedDateForDisplay;
    }

    public void setReceivedDateForDisplay(String receivedDateForDisplay) {
        this.receivedDateForDisplay = receivedDateForDisplay;
    }

    public String getReleasedDateForDisplay() {
        if (GenericValidator.isBlankOrNull(releasedDateForDisplay)) {
            return releasedDate != null ? DateUtil.convertSqlDateToStringDate(releasedDate) : null;
        }
        return releasedDateForDisplay;
    }

    public void setReleasedDateForDisplay(String releasedDateForDisplay) {
        this.releasedDateForDisplay = releasedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        releasedDate = DateUtil.convertStringDateToSqlDate(releasedDateForDisplay, locale);
    }

    public String getTransmissionDateForDisplay() {
        if (GenericValidator.isBlankOrNull(transmissionDateForDisplay)) {
            return transmissionDate != null ? DateUtil.convertSqlDateToStringDate(transmissionDate) : null;
        }
        return transmissionDateForDisplay;
    }

    public void setTransmissionDateForDisplay(String transmissionDateForDisplay) {
        this.transmissionDateForDisplay = transmissionDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        transmissionDate = DateUtil.convertStringDateToSqlDate(transmissionDateForDisplay, locale);
    }

    public void setCollectionTimeForDisplay(String collectionTimeForDisplay) {
        this.collectionTimeForDisplay = collectionTimeForDisplay;
        collectionDate = DateUtil.convertStringTimeToTimestamp(collectionDate, collectionTimeForDisplay);
    }

    public String getCollectionTimeForDisplay() {
        if (GenericValidator.isBlankOrNull(collectionTimeForDisplay)) {
            return collectionDate != null ? DateUtil.convertTimestampToStringTime(collectionDate) : null;
        }
        return collectionTimeForDisplay;
    }

    public List getSampleProjects() {
        return sampleProjects;
    }

    public void setSampleProjects(List sampleProjects) {
        this.sampleProjects = sampleProjects;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getStatusId() {
        return statusId;
    }

    public Timestamp getReceivedTimestamp() {
        return receivedTimestamp;
    }

    public void setReceivedTimestamp(Timestamp receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
        // also update String date

        receivedDateForDisplay = DateUtil.convertTimestampToStringDate(receivedTimestamp);

        // also update String time
        receivedTimeForDisplay = DateUtil.convertTimestampToStringTime(receivedTimestamp);
    }

    public String getReferringId() {
        return referringId;
    }

    public void setReferringId(String referringId) {
        this.referringId = referringId;
    }

    public String getClinicalOrderId() {
        return clinicalOrderId;
    }

    public void setClinicalOrderId(String clinicalOrderId) {
        this.clinicalOrderId = clinicalOrderId;
    }

    public Boolean getIsConfirmation() {
        return isConfirmation;
    }

    public void setIsConfirmation(Boolean isConfirmation) {
        this.isConfirmation = isConfirmation;
    }

    @Override
    public String getTableId() {
        return SampleServiceImpl.getTableReferenceId();
    }

    @Override
    public String getObjectId() {
        return getId();
    }

    @Override
    public BoundTo getBoundTo() {
        return BoundTo.SAMPLE;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }

    public OrderPriority getPriority() {
        return priority;
    }

    public void setPriority(OrderPriority priority) {
        this.priority = priority;
    }

    // GPS coordinates getters and setters
    public Double getGpsLatitude() {
        return gpsLatitude;
    }

    public void setGpsLatitude(Double gpsLatitude) {
        this.gpsLatitude = gpsLatitude;
    }

    public Double getGpsLongitude() {
        return gpsLongitude;
    }

    public void setGpsLongitude(Double gpsLongitude) {
        this.gpsLongitude = gpsLongitude;
    }

    public Integer getGpsAccuracyMeters() {
        return gpsAccuracyMeters;
    }

    public void setGpsAccuracyMeters(Integer gpsAccuracyMeters) {
        this.gpsAccuracyMeters = gpsAccuracyMeters;
    }

    public String getGpsCaptureMethod() {
        return gpsCaptureMethod;
    }

    public void setGpsCaptureMethod(String gpsCaptureMethod) {
        this.gpsCaptureMethod = gpsCaptureMethod;
    }

    public Timestamp getGpsCaptureTimestamp() {
        return gpsCaptureTimestamp;
    }

    public void setGpsCaptureTimestamp(Timestamp gpsCaptureTimestamp) {
        this.gpsCaptureTimestamp = gpsCaptureTimestamp;
    }

    public Boolean getStorageSkipped() {
        return storageSkipped;
    }

    public void setStorageSkipped(Boolean storageSkipped) {
        this.storageSkipped = storageSkipped;
    }

    public Boolean getConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(Boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    public String getConsentFormReference() {
        return consentFormReference;
    }

    public void setConsentFormReference(String consentFormReference) {
        this.consentFormReference = consentFormReference;
    }

    public Timestamp getConsentRecordedAt() {
        return consentRecordedAt;
    }

    public void setConsentRecordedAt(Timestamp consentRecordedAt) {
        this.consentRecordedAt = consentRecordedAt;
    }

    public String getConsentRecordedBy() {
        return consentRecordedBy;
    }

    public void setConsentRecordedBy(String consentRecordedBy) {
        this.consentRecordedBy = consentRecordedBy;
    }

    public String getGpsCoordinatesDisplay() {
        if (gpsLatitude != null && gpsLongitude != null) {
            return String.format("%.6f, %.6f", gpsLatitude, gpsLongitude);
        }
        return null;
    }

    public String getGpsAccuracyDisplay() {
        if (gpsAccuracyMeters != null && gpsAccuracyMeters > 0) {
            return String.format("±%dm", gpsAccuracyMeters);
        }
        return null;
    }

    public boolean hasGpsCoordinates() {
        return gpsLatitude != null && gpsLongitude != null;
    }
}
