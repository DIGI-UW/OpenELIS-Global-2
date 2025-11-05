package org.openelisglobal.sampleitem.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.validation.annotations.SafeHtml;

public class SampleItemAliquotForm extends BaseForm {

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String accessionNumber;

    @Valid
    @NotNull
    private List<SampleItem> sampleItems;

    public SampleItemAliquotForm() {
        setFormName("sampleItemAliquotForm");
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public List<SampleItem> getSampleItems() {
        return sampleItems;
    }

    public void setSampleItems(List<SampleItem> sampleItems) {
        this.sampleItems = sampleItems;
    }

    public static class SampleItem {

        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        private String externalId;

        @Valid
        @NotNull
        private List<Aliquot> aliquots;

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public List<Aliquot> getAliquots() {
            return aliquots;
        }

        public void setAliquots(List<Aliquot> aliquots) {
            this.aliquots = aliquots;
        }
    }

    public static class Aliquot {

        @NotBlank
        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        private String externalId;

        @NotNull
        private Double quantity;

        @NotNull
        private List<@NotBlank String> analyses;

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public Double getQuantity() {
            return quantity;
        }

        public void setQuantity(Double quantity) {
            this.quantity = quantity;
        }

        public List<String> getAnalyses() {
            return analyses;
        }

        public void setAnalyses(List<String> analyses) {
            this.analyses = analyses;
        }
    }

    public interface SampleItemAliquot {
    }
}