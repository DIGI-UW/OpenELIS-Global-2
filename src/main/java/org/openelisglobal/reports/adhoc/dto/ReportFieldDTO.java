package org.openelisglobal.reports.adhoc.dto;

import java.util.List;

public class ReportFieldDTO {

    private String fieldId;
    private String displayName;
    private String entityName;
    private DataType dataType;
    private boolean filterable;
    private List<FilterOperator> operators;
    private String propertyPath;
    private String messageKey;

    public ReportFieldDTO() {
    }

    public ReportFieldDTO(String fieldId, String displayName, String entityName, DataType dataType, boolean filterable,
            List<FilterOperator> operators, String propertyPath) {
        this.fieldId = fieldId;
        this.displayName = displayName;
        this.entityName = entityName;
        this.dataType = dataType;
        this.filterable = filterable;
        this.operators = operators;
        this.propertyPath = propertyPath;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public List<FilterOperator> getOperators() {
        return operators;
    }

    public void setOperators(List<FilterOperator> operators) {
        this.operators = operators;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public void setPropertyPath(String propertyPath) {
        this.propertyPath = propertyPath;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public enum DataType {
        STRING, DATE, DATETIME, INTEGER, DECIMAL, BOOLEAN, ENUM
    }

    public enum FilterOperator {
        EQUALS("=", "Equals"), NOT_EQUALS("!=", "Not Equals"), CONTAINS("LIKE", "Contains"),
        STARTS_WITH("LIKE", "Starts With"), ENDS_WITH("LIKE", "Ends With"), GREATER_THAN(">", "Greater Than"),
        LESS_THAN("<", "Less Than"), GREATER_OR_EQUAL(">=", "Greater or Equal"), LESS_OR_EQUAL("<=", "Less or Equal"),
        BETWEEN("BETWEEN", "Between"), IN("IN", "In"), IS_NULL("IS NULL", "Is Empty"),
        IS_NOT_NULL("IS NOT NULL", "Is Not Empty");

        private final String hqlOperator;
        private final String displayName;

        FilterOperator(String hqlOperator, String displayName) {
            this.hqlOperator = hqlOperator;
            this.displayName = displayName;
        }

        public String getHqlOperator() {
            return hqlOperator;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
