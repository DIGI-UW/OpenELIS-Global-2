package org.openelisglobal.reports.adhoc.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.reports.adhoc.dto.AdHocReportDefinitionDTO;
import org.openelisglobal.reports.adhoc.dto.FilterCriteriaDTO;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO.DataType;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO.FilterOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdHocQueryBuilderServiceImpl implements AdHocQueryBuilderService {

    @Autowired
    private AdHocFieldDefinitionService fieldDefinitionService;

    @Override
    public QueryResult buildQuery(AdHocReportDefinitionDTO definition) {
        StringBuilder hql = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();
        AtomicInteger paramIndex = new AtomicInteger(0);

        hql.append(buildSelectClause(definition));
        hql.append(buildFromClause(definition));

        String whereClause = buildWhereClause(definition, parameters, paramIndex);
        if (!whereClause.isEmpty()) {
            hql.append(" WHERE ").append(whereClause);
        }

        if (!GenericValidator.isBlankOrNull(definition.getSortBy())) {
            ReportFieldDTO sortField = fieldDefinitionService.getFieldById(definition.getSortBy());
            if (sortField != null) {
                hql.append(" ORDER BY ").append(sortField.getPropertyPath());
                hql.append(" ").append(definition.getSortOrder().name());
            }
        } else {
            if (definition.hasSampleFields()) {
                hql.append(" ORDER BY s.accessionNumber ASC");
            } else {
                hql.append(" ORDER BY pat.id ASC");
            }
        }

        return new QueryResult(hql.toString(), parameters);
    }

    @Override
    public QueryResult buildCountQuery(AdHocReportDefinitionDTO definition) {
        StringBuilder hql = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();
        AtomicInteger paramIndex = new AtomicInteger(0);

        if (definition.hasSampleFields()) {
            hql.append("SELECT COUNT(DISTINCT s.id)");
        } else {
            hql.append("SELECT COUNT(DISTINCT pat.id)");
        }

        hql.append(buildFromClause(definition));

        String whereClause = buildWhereClause(definition, parameters, paramIndex);
        if (!whereClause.isEmpty()) {
            hql.append(" WHERE ").append(whereClause);
        }

        return new QueryResult(hql.toString(), parameters);
    }

    private String buildSelectClause(AdHocReportDefinitionDTO definition) {
        StringBuilder select = new StringBuilder("SELECT ");
        List<String> selectedFields = definition.getSelectedFields();

        for (int i = 0; i < selectedFields.size(); i++) {
            ReportFieldDTO field = fieldDefinitionService.getFieldById(selectedFields.get(i));
            if (field != null) {
                if (i > 0) {
                    select.append(", ");
                }
                select.append(field.getPropertyPath());
            }
        }

        return select.toString();
    }

    private String buildFromClause(AdHocReportDefinitionDTO definition) {
        StringBuilder from = new StringBuilder(" FROM ");
        boolean hasPatient = definition.hasPatientFields() || hasPatientFilter(definition);
        boolean hasSample = definition.hasSampleFields() || hasSampleFilter(definition);

        if (hasSample && hasPatient) {
            from.append("Sample s, SampleHuman sh, Patient pat, Person per");
        } else if (hasSample) {
            from.append("Sample s, SampleHuman sh, Patient pat, Person per");
        } else {
            from.append("Patient pat, Person per");
        }

        return from.toString();
    }

    private String buildWhereClause(AdHocReportDefinitionDTO definition, Map<String, Object> parameters,
            AtomicInteger paramIndex) {
        StringBuilder where = new StringBuilder();
        boolean hasPatient = definition.hasPatientFields() || hasPatientFilter(definition);
        boolean hasSample = definition.hasSampleFields() || hasSampleFilter(definition);

        if (hasSample) {
            where.append("sh.sampleId = s.id AND sh.patientId = pat.id AND pat.person.id = per.id");
        } else if (hasPatient) {
            where.append("pat.person.id = per.id");
        }

        for (FilterCriteriaDTO filter : definition.getFilters()) {
            ReportFieldDTO field = fieldDefinitionService.getFieldById(filter.getFieldId());
            if (field != null) {
                String condition = buildFilterCondition(field, filter, parameters, paramIndex);
                if (!condition.isEmpty()) {
                    if (where.length() > 0) {
                        where.append(" AND ");
                    }
                    where.append(condition);
                }
            }
        }

        return where.toString();
    }

    private String buildFilterCondition(ReportFieldDTO field, FilterCriteriaDTO filter, Map<String, Object> parameters,
            AtomicInteger paramIndex) {
        String propertyPath = field.getPropertyPath();
        FilterOperator operator = filter.getOperator();
        String paramName = "param" + paramIndex.incrementAndGet();
        DataType dataType = field.getDataType();
        boolean isDateField = (dataType == DataType.DATE || dataType == DataType.DATETIME);

        switch (operator) {
        case EQUALS:
            if (isDateField) {
                parameters.put(paramName, parseDateTime(filter.getValue()));
                String paramName2 = "param" + paramIndex.incrementAndGet();
                parameters.put(paramName2, getEndOfDay(filter.getValue()));
                return propertyPath + " >= :" + paramName + " AND " + propertyPath + " < :" + paramName2;
            }
            parameters.put(paramName, convertValue(filter.getValue(), dataType));
            return propertyPath + " = :" + paramName;

        case NOT_EQUALS:
            parameters.put(paramName, convertValue(filter.getValue(), dataType));
            return propertyPath + " != :" + paramName;

        case CONTAINS:
            parameters.put(paramName, "%" + filter.getValue() + "%");
            return "LOWER(" + propertyPath + ") LIKE LOWER(:" + paramName + ")";

        case STARTS_WITH:
            parameters.put(paramName, filter.getValue() + "%");
            return "LOWER(" + propertyPath + ") LIKE LOWER(:" + paramName + ")";

        case ENDS_WITH:
            parameters.put(paramName, "%" + filter.getValue());
            return "LOWER(" + propertyPath + ") LIKE LOWER(:" + paramName + ")";

        case GREATER_THAN:
            if (isDateField) {
                parameters.put(paramName, getEndOfDay(filter.getValue()));
                return propertyPath + " >= :" + paramName;
            }
            parameters.put(paramName, convertValue(filter.getValue(), dataType));
            return propertyPath + " > :" + paramName;

        case LESS_THAN:
            if (isDateField) {
                parameters.put(paramName, parseDateTime(filter.getValue()));
                return propertyPath + " < :" + paramName;
            }
            parameters.put(paramName, convertValue(filter.getValue(), dataType));
            return propertyPath + " < :" + paramName;

        case GREATER_OR_EQUAL:
            if (isDateField) {
                parameters.put(paramName, parseDateTime(filter.getValue()));
                return propertyPath + " >= :" + paramName;
            }
            parameters.put(paramName, convertValue(filter.getValue(), dataType));
            return propertyPath + " >= :" + paramName;

        case LESS_OR_EQUAL:
            if (isDateField) {
                parameters.put(paramName, getEndOfDay(filter.getValue()));
                return propertyPath + " < :" + paramName;
            }
            parameters.put(paramName, convertValue(filter.getValue(), dataType));
            return propertyPath + " <= :" + paramName;

        case BETWEEN:
            String paramName2 = "param" + paramIndex.incrementAndGet();
            if (isDateField) {
                parameters.put(paramName, parseDateTime(filter.getValue()));
                parameters.put(paramName2, getEndOfDay(filter.getValueTo()));
                return propertyPath + " >= :" + paramName + " AND " + propertyPath + " < :" + paramName2;
            }
            parameters.put(paramName, convertValue(filter.getValue(), dataType));
            parameters.put(paramName2, convertValue(filter.getValueTo(), dataType));
            return propertyPath + " BETWEEN :" + paramName + " AND :" + paramName2;

        case IS_NULL:
            return propertyPath + " IS NULL";

        case IS_NOT_NULL:
            return propertyPath + " IS NOT NULL";

        case IN:
            String[] values = filter.getValue().split(",");
            parameters.put(paramName, java.util.Arrays.asList(values));
            return propertyPath + " IN (:" + paramName + ")";

        default:
            return "";
        }
    }

    private java.sql.Timestamp getEndOfDay(String value) {
        java.sql.Date date = parseDate(value);
        if (date == null) {
            return null;
        }
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        return new java.sql.Timestamp(cal.getTimeInMillis());
    }

    private Object convertValue(String value, DataType dataType) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            switch (dataType) {
            case DATE:
                return parseDate(value);
            case DATETIME:
                return parseDateTime(value);
            case INTEGER:
                return Integer.parseInt(value.trim());
            case DECIMAL:
                return Double.parseDouble(value.trim());
            case BOOLEAN:
                return Boolean.parseBoolean(value.trim());
            default:
                return value;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private java.sql.Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        if (value.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            try {
                String datePart = value.length() > 10 ? value.substring(0, 10) : value;
                return java.sql.Date.valueOf(datePart);
            } catch (Exception e) {
                // Fall through to DateUtil
            }
        }

        return DateUtil.convertStringDateToSqlDate(value);
    }

    private java.sql.Timestamp parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        if (value.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            try {
                String datePart = value.length() > 10 ? value.substring(0, 10) : value;
                java.sql.Date date = java.sql.Date.valueOf(datePart);
                return new java.sql.Timestamp(date.getTime());
            } catch (Exception e) {
                // Fall through to DateUtil
            }
        }

        return DateUtil.convertStringDateToTruncatedTimestamp(value);
    }

    private boolean hasPatientFilter(AdHocReportDefinitionDTO definition) {
        return definition.getFilters().stream().anyMatch(f -> f.getFieldId().startsWith("patient."));
    }

    private boolean hasSampleFilter(AdHocReportDefinitionDTO definition) {
        return definition.getFilters().stream().anyMatch(f -> f.getFieldId().startsWith("sample."));
    }
}
