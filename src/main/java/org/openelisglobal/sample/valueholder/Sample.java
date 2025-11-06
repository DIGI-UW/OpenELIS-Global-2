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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
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

public class Sample extends EnumValueItemImpl implements NoteObject {

    private static final long serialVersionUID = 1407388492068629053L;

    private String id;
    private UUID fhirUuid;
    private String accessionNumber;
    private String packageId;
    private String domain;
    private String nextItemSequence;
    private String revision;
    private Date enteredDate;
    private String enteredDateForDisplay;
    private Timestamp receivedTimestamp;
    private String receivedDateForDisplay;
    private String receivedTimeForDisplay;
    private String referredCultureFlag;
    private Timestamp collectionDate;
    private String collectionDateForDisplay;
    private String collectionTimeForDisplay;
    private String clientReference;
    private String status;
    private Date releasedDate;
    private String releasedDateForDisplay;
    private String stickerReceivedFlag;
    private String sysUserId;
    private String barCode;
    private Date transmissionDate;
    private String transmissionDateForDisplay;
    private ValueHolderInterface systemUser;
    private String referringId;
    private String clinicalOrderId;
    private Boolean isConfirmation = false;
    private OrderPriority priority;

    // testing one-to-many
    // this is for HSE I and II - ability to enter up to two projects
    private List sampleProjects;

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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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
}
