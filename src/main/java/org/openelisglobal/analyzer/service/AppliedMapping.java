package org.openelisglobal.analyzer.service;

/**
 * Applied mapping result
 * 
 */
public class AppliedMapping {
    private String analyzerFieldName;
    private String openelisFieldId;
    private String openelisFieldType;
    private String mappedValue;
    private String mappingId;

    public String getAnalyzerFieldName() {
        return analyzerFieldName;
    }

    public void setAnalyzerFieldName(String analyzerFieldName) {
        this.analyzerFieldName = analyzerFieldName;
    }

    public String getOpenelisFieldId() {
        return openelisFieldId;
    }

    public void setOpenelisFieldId(String openelisFieldId) {
        this.openelisFieldId = openelisFieldId;
    }

    public String getOpenelisFieldType() {
        return openelisFieldType;
    }

    public void setOpenelisFieldType(String openelisFieldType) {
        this.openelisFieldType = openelisFieldType;
    }

    public String getMappedValue() {
        return mappedValue;
    }

    public void setMappedValue(String mappedValue) {
        this.mappedValue = mappedValue;
    }

    public String getMappingId() {
        return mappingId;
    }

    public void setMappingId(String mappingId) {
        this.mappingId = mappingId;
    }
}
