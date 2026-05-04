package org.openelisglobal.sampleitem.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.openelisglobal.validation.annotations.SafeHtml;

public class SampleItemForm extends BaseForm {

    private List<SampleItemEntry> sampleItems;

    private String accessionNumber;

    public SampleItemForm() {
        setFormName("SampleItemForm");
    }

    public static class SampleItemEntry {
        @NotBlank
        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        private String sampleItemIdNumber;

        @NotBlank
        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        private String externalId;

        @NotBlank
        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        private String typeOfSample;

        @NotNull
        private Timestamp collectionDate;

        @NotBlank
        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        private String collector;

        private Double quantity;

        private UnitOfMeasure uom;

        private List<AnalysisEntry> analysis;

        public String getSampleItemIdNumber() {
            return sampleItemIdNumber;
        }

        public void setSampleItemIdNumber(String sampleItemIdNumber) {
            this.sampleItemIdNumber = sampleItemIdNumber;
        }

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public String getTypeOfSample() {
            return typeOfSample;
        }

        public void setTypeOfSample(String typeOfSample) {
            this.typeOfSample = typeOfSample;
        }

        public Timestamp getCollectionDate() {
            return collectionDate;
        }

        public void setCollectionDate(Timestamp collectionDate) {
            this.collectionDate = collectionDate;
        }

        public String getCollector() {
            return collector;
        }

        public void setCollector(String collector) {
            this.collector = collector;
        }

        public Double getQuantity() {
            return quantity;
        }

        public void setQuantity(Double quantity) {
            this.quantity = quantity;
        }

        public UnitOfMeasure getUom() {
            return uom;
        }

        public void setUom(UnitOfMeasure uom) {
            this.uom = uom;
        }

        public List<AnalysisEntry> getAnalysis() {
            return analysis;
        }

        public void setAnalysis(List<AnalysisEntry> analysis) {
            this.analysis = analysis;
        }
    }

    public static class AnalysisEntry {
        private String id;
        private String statusId;
        private Date startedDate;
        private String startedDateForDisplay;
        private TestEntry test;
        private TestSectionEntry testSection;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStatusId() {
            return statusId;
        }

        public void setStatusId(String statusId) {
            this.statusId = statusId;
        }

        public Date getStartedDate() {
            return startedDate;
        }

        public void setStartedDate(Date startedDate) {
            this.startedDate = startedDate;
        }

        public String getStartedDateForDisplay() {
            return startedDateForDisplay;
        }

        public void setStartedDateForDisplay(String startedDateForDisplay) {
            this.startedDateForDisplay = startedDateForDisplay;
        }

        public TestEntry getTest() {
            return test;
        }

        public void setTest(TestEntry test) {
            this.test = test;
        }

        public TestSectionEntry getTestSection() {
            return testSection;
        }

        public void setTestSection(TestSectionEntry testSection) {
            this.testSection = testSection;
        }
    }

    public static class TestEntry {
        private String name;
        private LocalizationEntry localizedTestName;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LocalizationEntry getLocalizedTestName() {
            return localizedTestName;
        }

        public void setLocalizedTestName(LocalizationEntry localizedTestName) {
            this.localizedTestName = localizedTestName;
        }
    }

    public static class TestSectionEntry {
        private String testSectionName;
        private LocalizationEntry localization;

        public String getTestSectionName() {
            return testSectionName;
        }

        public void setTestSectionName(String testSectionName) {
            this.testSectionName = testSectionName;
        }

        public LocalizationEntry getLocalization() {
            return localization;
        }

        public void setLocalization(LocalizationEntry localization) {
            this.localization = localization;
        }
    }

    public static class LocalizationEntry {
        private String localizedValue;

        public String getLocalizedValue() {
            return localizedValue;
        }

        public void setLocalizedValue(String localizedValue) {
            this.localizedValue = localizedValue;
        }
    }

    public List<SampleItemEntry> getSampleItems() {
        return sampleItems;
    }

    public void setSampleItems(List<SampleItemEntry> sampleItems) {
        this.sampleItems = sampleItems;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }
}