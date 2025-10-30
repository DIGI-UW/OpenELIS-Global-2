package org.openelisglobal.sampleitem.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
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

        private List<Analysis> analysis;

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

        public List<Analysis> getAnalysis() {
            return analysis;
        }

        public void setAnalysis(List<Analysis> analysis) {
            this.analysis = analysis;
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