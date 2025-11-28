package org.openelisglobal.genericsample.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.common.form.BaseForm;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericSampleOrderForm extends BaseForm {

    private DefaultFields defaultFields;
    private Questionnaire fhirQuestionnaire;
    private Map<String, Object> fhirResponses;
    private Integer notebookId;

    public GenericSampleOrderForm() {
        setFormName("genericSampleOrderForm");
        this.fhirResponses = new HashMap<>();
    }

    public static class DefaultFields {
        private String labNo;
        private String sampleTypeId;
        private String quantity;
        private String sampleUnitOfMeasure;
        private String from;
        private String collector;
        private String collectionDate;
        private String collectionTime;

        // Getters and setters
        public String getLabNo() {
            return labNo;
        }

        public void setLabNo(String labNo) {
            this.labNo = labNo;
        }

        public String getSampleTypeId() {
            return sampleTypeId;
        }

        public void setSampleTypeId(String sampleTypeId) {
            this.sampleTypeId = sampleTypeId;
        }

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }

        public String getSampleUnitOfMeasure() {
            return sampleUnitOfMeasure;
        }

        public void setSampleUnitOfMeasure(String sampleUnitOfMeasure) {
            this.sampleUnitOfMeasure = sampleUnitOfMeasure;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getCollector() {
            return collector;
        }

        public void setCollector(String collector) {
            this.collector = collector;
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
    }

    public DefaultFields getDefaultFields() {
        return defaultFields;
    }

    public void setDefaultFields(DefaultFields defaultFields) {
        this.defaultFields = defaultFields;
    }

    public Questionnaire getFhirQuestionnaire() {
        return fhirQuestionnaire;
    }

    public void setFhirQuestionnaire(Questionnaire fhirQuestionnaire) {
        this.fhirQuestionnaire = fhirQuestionnaire;
    }

    public Map<String, Object> getFhirResponses() {
        return fhirResponses;
    }

    public void setFhirResponses(Map<String, Object> fhirResponses) {
        this.fhirResponses = fhirResponses;
    }

    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }
}
