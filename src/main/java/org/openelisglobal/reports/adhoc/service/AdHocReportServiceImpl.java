package org.openelisglobal.reports.adhoc.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reports.adhoc.dto.AdHocReportDefinitionDTO;
import org.openelisglobal.reports.adhoc.dto.AdHocReportResultDTO;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO;
import org.openelisglobal.reports.adhoc.service.AdHocQueryBuilderService.QueryResult;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdHocReportServiceImpl implements AdHocReportService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 10000;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AdHocFieldDefinitionService fieldDefinitionService;

    @Autowired
    private AdHocQueryBuilderService queryBuilderService;

    @Autowired
    private AdHocPdfGeneratorService pdfGeneratorService;

    @Autowired
    private StatusOfSampleService statusOfSampleService;

    private Map<String, String> statusNameCache = null;

    @Override
    public AdHocReportResultDTO executeReport(AdHocReportDefinitionDTO definition) {
        validateReportDefinition(definition);

        AdHocReportResultDTO result = new AdHocReportResultDTO();

        for (String fieldId : definition.getSelectedFields()) {
            ReportFieldDTO field = fieldDefinitionService.getFieldById(fieldId);
            if (field != null) {
                result.addColumn(field.getFieldId(), field.getDisplayName(), field.getDataType());
            }
        }

        QueryResult countQueryResult = queryBuilderService.buildCountQuery(definition);
        long totalCount = executeCountQuery(countQueryResult);
        result.setTotalCount(totalCount);

        QueryResult queryResult = queryBuilderService.buildQuery(definition);
        int limit = definition.getLimit() != null ? Math.min(definition.getLimit(), MAX_LIMIT) : DEFAULT_LIMIT;
        int offset = definition.getOffset() != null ? definition.getOffset() : 0;
        int statusFieldIndex = findStatusFieldIndex(definition.getSelectedFields());

        List<List<Object>> rows = executeQuery(queryResult, limit, offset, definition.getSelectedFields().size(),
                statusFieldIndex);
        result.setRows(rows);
        result.setReturnedCount(rows.size());
        result.setHasMore(offset + rows.size() < totalCount);

        LogEvent.logInfo(this.getClass().getSimpleName(), "executeReport", "Executed ad-hoc report: "
                + definition.getSelectedFields().size() + " fields, " + rows.size() + " rows");

        return result;
    }

    @Override
    public byte[] generatePdfReport(AdHocReportDefinitionDTO definition) {
        validateReportDefinition(definition);

        if (definition.getLimit() == null) {
            definition.setLimit(MAX_LIMIT);
        }

        AdHocReportResultDTO reportData = executeReport(definition);
        return pdfGeneratorService.generatePdf(definition, reportData);
    }

    @Override
    public boolean validateReportDefinition(AdHocReportDefinitionDTO definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Report definition cannot be null");
        }

        if (definition.getSelectedFields() == null || definition.getSelectedFields().isEmpty()) {
            throw new IllegalArgumentException("At least one field must be selected");
        }

        if (!fieldDefinitionService.validateFieldIds(definition.getSelectedFields())) {
            throw new IllegalArgumentException("One or more selected fields are invalid");
        }

        if (definition.getFilters() != null) {
            for (var filter : definition.getFilters()) {
                if (fieldDefinitionService.getFieldById(filter.getFieldId()) == null) {
                    throw new IllegalArgumentException("Invalid filter field: " + filter.getFieldId());
                }
            }
        }

        return true;
    }

    private long executeCountQuery(QueryResult queryResult) {
        try {
            Query query = entityManager.createQuery(queryResult.getHql());
            setQueryParameters(query, queryResult.getParameters());
            Object result = query.getSingleResult();
            return ((Number) result).longValue();
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "executeCountQuery",
                    "Error executing count query: " + e.getMessage());
            throw new RuntimeException("Error executing count query", e);
        }
    }

    private List<List<Object>> executeQuery(QueryResult queryResult, int limit, int offset, int columnCount,
            int statusFieldIndex) {
        List<List<Object>> rows = new ArrayList<>();

        try {
            Query query = entityManager.createQuery(queryResult.getHql());
            setQueryParameters(query, queryResult.getParameters());
            query.setMaxResults(limit);
            query.setFirstResult(offset);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            for (Object[] row : results) {
                List<Object> formattedRow = new ArrayList<>();
                for (int i = 0; i < row.length; i++) {
                    Object value = row[i];
                    if (i == statusFieldIndex && value != null) {
                        value = getStatusName(value.toString());
                    }
                    formattedRow.add(formatValue(value));
                }
                rows.add(formattedRow);
            }

            if (results.isEmpty() && columnCount == 1) {
                @SuppressWarnings("unchecked")
                List<Object> singleColumnResults = query.getResultList();
                for (Object value : singleColumnResults) {
                    List<Object> singleRow = new ArrayList<>();
                    if (statusFieldIndex == 0 && value != null) {
                        value = getStatusName(value.toString());
                    }
                    singleRow.add(formatValue(value));
                    rows.add(singleRow);
                }
            }

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "executeQuery",
                    "Error executing query: " + e.getMessage());
            throw new RuntimeException("Error executing report query", e);
        }

        return rows;
    }

    private int findStatusFieldIndex(List<String> selectedFields) {
        for (int i = 0; i < selectedFields.size(); i++) {
            if ("sample.statusId".equals(selectedFields.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private String getStatusName(String statusId) {
        if (statusId == null || statusId.trim().isEmpty()) {
            return "";
        }

        if (statusNameCache == null) {
            initializeStatusCache();
        }

        String statusName = statusNameCache.get(statusId);
        return statusName != null ? statusName : "Unknown (" + statusId + ")";
    }

    private synchronized void initializeStatusCache() {
        if (statusNameCache != null) {
            return;
        }

        statusNameCache = new HashMap<>();
        try {
            List<StatusOfSample> allStatuses = statusOfSampleService.getAllStatusOfSamples();
            for (StatusOfSample status : allStatuses) {
                if (status.getId() != null) {
                    String displayName = status.getStatusOfSampleName();
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = status.getStatusType();
                    }
                    if (displayName != null && !displayName.isEmpty()) {
                        statusNameCache.put(status.getId(), displayName);
                    }
                }
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "initializeStatusCache",
                    "Loaded " + statusNameCache.size() + " status mappings");
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "initializeStatusCache",
                    "Error loading status cache: " + e.getMessage());
            addFallbackStatusMappings();
        }
    }

    private void addFallbackStatusMappings() {
        statusNameCache.put("1", "Test Entered");
        statusNameCache.put("2", "Testing Started");
        statusNameCache.put("3", "Testing finished");
        statusNameCache.put("4", "Not Tested");
        statusNameCache.put("6", "Finalized");
        statusNameCache.put("7", "Biologist Rejection");
        statusNameCache.put("12", "NonConforming");
        statusNameCache.put("14", "Test Canceled");
        statusNameCache.put("15", "Technical Acceptance");
        statusNameCache.put("16", "Technical Rejected");
        statusNameCache.put("19", "SampleCanceled");
        statusNameCache.put("20", "SampleEntered");
    }

    private void setQueryParameters(Query query, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private Object formatValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Timestamp) {
            synchronized (DATETIME_FORMAT) {
                return DATETIME_FORMAT.format((Timestamp) value);
            }
        }

        if (value instanceof Date) {
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.format((Date) value);
            }
        }

        if (value instanceof java.util.Date) {
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.format((java.util.Date) value);
            }
        }

        return value.toString();
    }
}
