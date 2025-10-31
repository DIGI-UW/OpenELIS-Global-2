/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.sample.bean;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.form.SamplePatientEntryForm.SamplePatientEntryBatch;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.samplebatchentry.form.SampleBatchEntryForm;
import org.openelisglobal.validation.annotations.OptionalNotBlank;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.annotations.ValidTime;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class SampleOrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
            SampleEditForm.SampleEdit.class })
    private String newRequesterName;

    // for display
    private Collection orderTypes;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String orderType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class })
    private String externalOrderNumber;

    @NotBlank(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class })
    @ValidAccessionNumber(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
            SampleEditForm.SampleEdit.class })
    private String labNo;

    @OptionalNotBlank(formFields = { Field.SampleEntryUseRequestDate }, groups = {
            SamplePatientEntryForm.SamplePatientEntry.class, SampleEditForm.SampleEdit.class })
    @ValidDate(relative = DateRelation.PAST, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String requestDate;

    @NotBlank(groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
            SampleEditForm.SampleEdit.class })
    @ValidDate(relative = DateRelation.PAST, groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
            SampleEditForm.SampleEdit.class })
    private String receivedDateForDisplay;

    @ValidTime(groups = { SampleBatchEntryForm.SampleBatchEntrySetup.class,
            SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
            SampleEditForm.SampleEdit.class })
    private String receivedTime;

    @ValidDate(relative = DateRelation.FUTURE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String nextVisitDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String requesterSampleID;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String referringPatientNumber;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String referringSiteId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String referringSiteDepartmentId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String referringSiteCode;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String referringSiteName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String referringSiteDepartmentName;

    // for display
    private List<IdValuePair> referringSiteList;

    // for display
    private List<IdValuePair> referringSiteDepartmentList;

    // for display
    private List<IdValuePair> providersList;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class })
    private String providerId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class })
    private String providerPersonId;

    @ValidName(nameType = NameType.FIRST_NAME, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String providerFirstName;

    @ValidName(nameType = NameType.LAST_NAME, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String providerLastName;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String providerWorkPhone;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String providerFax;

    @Email(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
            SampleEditForm.SampleEdit.class })
    private String providerEmail;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String facilityAddressStreet;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String facilityAddressCommune;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String facilityPhone;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String facilityFax;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String paymentOptionSelection;

    // for display
    private Collection paymentOptions;

    @NotNull(groups = { SamplePatientEntryForm.SamplePatientEntry.class, SamplePatientEntryBatch.class,
            SampleEditForm.SampleEdit.class })
    private Boolean modified = false;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class })
    private String sampleId;

    private boolean readOnly = false;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String billingReferenceNumber;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String testLocationCode;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String otherLocationCode;

    // for display
    private Collection testLocationCodeList;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class, SampleEditForm.SampleEdit.class })
    private String program;

    // for display
    private Collection programList;

    private String contactTracingIndexName;

    private String provisionalClinicalDiagnosis;
    private String contactTracingIndexRecordNumber;

    private QuestionnaireResponse additionalQuestions;

    private String programId;

    // for display
    private List<IdValuePair> priorityList;

    public List<IdValuePair> getPriorityList() {
        return priorityList;
    }

    public void setPriorityList(List<IdValuePair> priorityList) {
        this.priorityList = priorityList;
    }

    private OrderPriority priority;

    public OrderPriority getPriority() {
        return priority;
    }

    public void setPriority(OrderPriority priority) {
        this.priority = priority;
    }

    public String getNewRequesterName() {
        return newRequesterName;
    }

    public void setNewRequesterName(String newRequesterName) {
        this.newRequesterName = newRequesterName;
    }

    public Collection getOrderTypes() {
        return orderTypes;
    }

    public void setOrderTypes(Collection orderTypes) {
        this.orderTypes = orderTypes;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getProvisionalClinicalDiagnosis() {
        return provisionalClinicalDiagnosis;
    }

    public void setProvisionalClinicalDiagnosis(String provisionalClinicalDiagnosis) {
        this.provisionalClinicalDiagnosis = provisionalClinicalDiagnosis;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getExternalOrderNumber() {
        return externalOrderNumber;
    }

    public void setExternalOrderNumber(String externalOrderNumber) {
        this.externalOrderNumber = externalOrderNumber;
    }

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getReceivedDateForDisplay() {
        return receivedDateForDisplay;
    }

    public void setReceivedDateForDisplay(String receivedDateForDisplay) {
        this.receivedDateForDisplay = receivedDateForDisplay;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(String receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getNextVisitDate() {
        return nextVisitDate;
    }

    public void setNextVisitDate(String nextVisitDate) {
        this.nextVisitDate = nextVisitDate;
    }

    public String getRequesterSampleID() {
        return requesterSampleID;
    }

    public void setRequesterSampleID(String requesterSampleID) {
        this.requesterSampleID = requesterSampleID;
    }

    public String getReferringPatientNumber() {
        return referringPatientNumber;
    }

    public void setReferringPatientNumber(String referringPatientNumber) {
        this.referringPatientNumber = referringPatientNumber;
    }

    public String getReferringSiteId() {
        return referringSiteId;
    }

    public void setReferringSiteId(String referringSiteId) {
        this.referringSiteId = referringSiteId;
    }

    public String getReferringSiteCode() {
        return referringSiteCode;
    }

    public void setReferringSiteCode(String referringSiteCode) {
        this.referringSiteCode = referringSiteCode;
    }

    public String getReferringSiteName() {
        return referringSiteName;
    }

    public void setReferringSiteName(String referringSiteName) {
        this.referringSiteName = referringSiteName;
    }

    public List<IdValuePair> getReferringSiteList() {
        return referringSiteList;
    }

    public void setReferringSiteList(List<IdValuePair> referringSiteList) {
        this.referringSiteList = referringSiteList;
    }

    public List<IdValuePair> getReferringSiteDepartmentList() {
        return referringSiteDepartmentList;
    }

    public void setReferringSiteDepartmentList(List<IdValuePair> referringSiteDepartmentList) {
        this.referringSiteDepartmentList = referringSiteDepartmentList;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderPersonId() {
        return providerPersonId;
    }

    public void setProviderPersonId(String providerPersonId) {
        this.providerPersonId = providerPersonId;
    }

    public String getProviderFirstName() {
        return providerFirstName;
    }

    public void setProviderFirstName(String providerFirstName) {
        this.providerFirstName = providerFirstName;
    }

    public String getProviderLastName() {
        return providerLastName;
    }

    public void setProviderLastName(String providerLastName) {
        this.providerLastName = providerLastName;
    }

    public String getProviderWorkPhone() {
        return providerWorkPhone;
    }

    public void setProviderWorkPhone(String providerWorkPhone) {
        this.providerWorkPhone = providerWorkPhone;
    }

    public String getProviderFax() {
        return providerFax;
    }

    public void setProviderFax(String providerFax) {
        this.providerFax = providerFax;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public void setProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    public String getFacilityAddressStreet() {
        return facilityAddressStreet;
    }

    public void setFacilityAddressStreet(String facilityAddressStreet) {
        this.facilityAddressStreet = facilityAddressStreet;
    }

    public String getFacilityAddressCommune() {
        return facilityAddressCommune;
    }

    public void setFacilityAddressCommune(String facilityAddressCommune) {
        this.facilityAddressCommune = facilityAddressCommune;
    }

    public String getFacilityPhone() {
        return facilityPhone;
    }

    public void setFacilityPhone(String facilityPhone) {
        this.facilityPhone = facilityPhone;
    }

    public String getFacilityFax() {
        return facilityFax;
    }

    public void setFacilityFax(String facilityFax) {
        this.facilityFax = facilityFax;
    }

    public String getPaymentOptionSelection() {
        return paymentOptionSelection;
    }

    public void setPaymentOptionSelection(String paymentOptionSelection) {
        this.paymentOptionSelection = paymentOptionSelection;
    }

    public Collection getPaymentOptions() {
        return paymentOptions;
    }

    public void setPaymentOptions(Collection paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    public String getOtherLocationCode() {
        return otherLocationCode;
    }

    public void setOtherLocationCode(String otherLocationCode) {
        this.otherLocationCode = otherLocationCode;
    }

    public Boolean getModified() {
        return modified;
    }

    public void setModified(Boolean modified) {
        this.modified = modified;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getBillingReferenceNumber() {
        return billingReferenceNumber;
    }

    public void setBillingReferenceNumber(String billingReferenceNumber) {
        this.billingReferenceNumber = billingReferenceNumber;
    }

    public String getTestLocationCode() {
        return testLocationCode;
    }

    public void setTestLocationCode(String testLocationCode) {
        this.testLocationCode = testLocationCode;
    }

    public Collection getTestLocationCodeList() {
        return testLocationCodeList;
    }

    public void setTestLocationCodeList(Collection testLocationCodeList) {
        this.testLocationCodeList = testLocationCodeList;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public Collection getProgramList() {
        return programList;
    }

    public void setProgramList(Collection programList) {
        this.programList = programList;
    }

    public String getContactTracingIndexName() {
        return contactTracingIndexName;
    }

    public void setContactTracingIndexName(String contactTracingIndexName) {
        this.contactTracingIndexName = contactTracingIndexName;
    }

    public String getContactTracingIndexRecordNumber() {
        return contactTracingIndexRecordNumber;
    }

    public void setContactTracingIndexRecordNumber(String contactTracingIndexRecordNumber) {
        this.contactTracingIndexRecordNumber = contactTracingIndexRecordNumber;
    }

    public String getReferringSiteDepartmentId() {
        return referringSiteDepartmentId;
    }

    public void setReferringSiteDepartmentId(String referringSiteDepartmentId) {
        this.referringSiteDepartmentId = referringSiteDepartmentId;
    }

    public String getReferringSiteDepartmentName() {
        return referringSiteDepartmentName;
    }

    public void setReferringSiteDepartmentName(String referringSiteDepartmentName) {
        this.referringSiteDepartmentName = referringSiteDepartmentName;
    }

    public List<IdValuePair> getProvidersList() {
        return providersList;
    }

    public void setProvidersList(List<IdValuePair> providersList) {
        this.providersList = providersList;
    }

    public QuestionnaireResponse getAdditionalQuestions() {
        return additionalQuestions;
    }

    public void setAdditionalQuestions(QuestionnaireResponse additionalQuestions) {
        this.additionalQuestions = additionalQuestions;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }
}
