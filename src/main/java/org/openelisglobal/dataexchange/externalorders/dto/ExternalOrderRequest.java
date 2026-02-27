package org.openelisglobal.dataexchange.externalorders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.validator.ValidationHelper;

/**
 * Minimal external order payload.
 *
 * This DTO is intentionally small and focused on the quintessential fields
 * required to place an order via the existing SamplePatientEntry pipeline.
 *
 * We can expand this later as requirements grow without impacting the
 * internal order creation flow.
 */
public class ExternalOrderRequest {

    @NotBlank
    private String externalOrderNumber;

    @NotBlank
    private String patientGuid;

    private String priority;

    /** Existing referring site organization ID (foreign key to Organization). */
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String referringSiteId;

    private String referringSiteName;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String referringSiteDepartmentId;

    /** Existing provider person ID (foreign key to Person/Provider). */
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String providerPersonId;

    private String providerFirstName;

    private String providerLastName;

    private String providerWorkPhone;

    private String providerFax;

    private String providerEmail;

    /** Optional received date (yyyy-MM-dd), mapped to receivedDateForDisplay. */
    private String receivedDate;

    /** Optional received time (HH:mm), mapped to receivedTime. */
    private String receivedTime;

    /** Optional request date (yyyy-MM-dd). */
    private String requestDate;

    private String programId;

    @Valid
    @NotEmpty
    private List<ExternalOrderSample> samples = new ArrayList<>();

    public String getExternalOrderNumber() {
        return externalOrderNumber;
    }

    public void setExternalOrderNumber(String externalOrderNumber) {
        this.externalOrderNumber = externalOrderNumber;
    }

    public String getPatientGuid() {
        return patientGuid;
    }

    public void setPatientGuid(String patientGuid) {
        this.patientGuid = patientGuid;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getReferringSiteId() {
        return referringSiteId;
    }

    public void setReferringSiteId(String referringSiteId) {
        this.referringSiteId = referringSiteId;
    }

    public String getReferringSiteName() {
        return referringSiteName;
    }

    public void setReferringSiteName(String referringSiteName) {
        this.referringSiteName = referringSiteName;
    }

    public String getReferringSiteDepartmentId() {
        return referringSiteDepartmentId;
    }

    public void setReferringSiteDepartmentId(String referringSiteDepartmentId) {
        this.referringSiteDepartmentId = referringSiteDepartmentId;
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

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(String receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public List<ExternalOrderSample> getSamples() {
        return samples;
    }

    public void setSamples(List<ExternalOrderSample> samples) {
        this.samples = samples;
    }

    public static class ExternalOrderSample {

        private String sampleTypeId;

        @Valid
        private List<ExternalOrderTestRef> tests;

        @Valid
        private List<ExternalOrderTestRef> removedTests;

        @Valid
        private List<ExternalOrderPanelRef> panels;

        @Valid
        private List<ExternalOrderPanelRef> removedPanels;

        private String collectionDate;

        private String collectionTime;

        private String collector;

        private String quantity;

        private String uom;

        public String getSampleTypeId() {
            return sampleTypeId;
        }

        public void setSampleTypeId(String sampleTypeId) {
            this.sampleTypeId = sampleTypeId;
        }

        public List<ExternalOrderTestRef> getTests() {
            return tests;
        }

        public void setTests(List<ExternalOrderTestRef> tests) {
            this.tests = tests;
        }

        public List<ExternalOrderTestRef> getRemovedTests() {
            return removedTests;
        }

        public void setRemovedTests(List<ExternalOrderTestRef> removedTests) {
            this.removedTests = removedTests;
        }

        public List<ExternalOrderPanelRef> getPanels() {
            return panels;
        }

        public void setPanels(List<ExternalOrderPanelRef> panels) {
            this.panels = panels;
        }

        public List<ExternalOrderPanelRef> getRemovedPanels() {
            return removedPanels;
        }

        public void setRemovedPanels(List<ExternalOrderPanelRef> removedPanels) {
            this.removedPanels = removedPanels;
        }

        public String getCollectionDate() {
            return collectionDate;
        }

        public void setCollectionDate(String collectionDate) {
            this.collectionDate = collectionDate;
        }

        public String getCollectionTime() {
            return collectionTime;
        }

        public void setCollectionTime(String collectionTime) {
            this.collectionTime = collectionTime;
        }

        public String getCollector() {
            return collector;
        }

        public void setCollector(String collector) {
            this.collector = collector;
        }

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }

        public String getUom() {
            return uom;
        }

        public void setUom(String uom) {
            this.uom = uom;
        }
    }

    public static class ExternalOrderTestRef {

        private String testGuid;

        private String loinc;

        public String getTestGuid() {
            return testGuid;
        }

        public void setTestGuid(String testGuid) {
            this.testGuid = testGuid;
        }

        public String getLoinc() {
            return loinc;
        }

        public void setLoinc(String loinc) {
            this.loinc = loinc;
        }
    }

    public static class ExternalOrderPanelRef {

        private String panelGuid;

        private String loinc;

        public String getPanelGuid() {
            return panelGuid;
        }

        public void setPanelGuid(String panelGuid) {
            this.panelGuid = panelGuid;
        }

        public String getLoinc() {
            return loinc;
        }

        public void setLoinc(String loinc) {
            this.loinc = loinc;
        }
    }
}

