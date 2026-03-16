package org.openelisglobal.customfield.dto;

public class CustomFieldValueDTO {

    private String customFieldId;
    private String fieldValue;

    public CustomFieldValueDTO() {
    }

    public CustomFieldValueDTO(String customFieldId, String fieldValue) {
        this.customFieldId = customFieldId;
        this.fieldValue = fieldValue;
    }

    public String getCustomFieldId() {
        return customFieldId;
    }

    public void setCustomFieldId(String customFieldId) {
        this.customFieldId = customFieldId;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
}
