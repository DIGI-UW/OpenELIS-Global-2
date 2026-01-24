package org.openelisglobal.reports.adhoc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.reports.adhoc.dto.AvailableFieldsDTO;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO.DataType;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO.FilterOperator;
import org.springframework.stereotype.Service;

@Service
public class AdHocFieldDefinitionServiceImpl implements AdHocFieldDefinitionService {

    private static final List<FilterOperator> STRING_OPERATORS = Arrays.asList(FilterOperator.EQUALS,
            FilterOperator.NOT_EQUALS, FilterOperator.CONTAINS, FilterOperator.STARTS_WITH, FilterOperator.IS_NULL,
            FilterOperator.IS_NOT_NULL);

    private static final List<FilterOperator> DATE_OPERATORS = Arrays.asList(FilterOperator.EQUALS,
            FilterOperator.NOT_EQUALS, FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN,
            FilterOperator.GREATER_OR_EQUAL, FilterOperator.LESS_OR_EQUAL, FilterOperator.BETWEEN,
            FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);

    private static final List<FilterOperator> ENUM_OPERATORS = Arrays.asList(FilterOperator.EQUALS,
            FilterOperator.NOT_EQUALS, FilterOperator.IN, FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);

    private final Map<String, ReportFieldDTO> fieldCache = new HashMap<>();
    private List<ReportFieldDTO> patientFields;
    private List<ReportFieldDTO> sampleFields;

    public AdHocFieldDefinitionServiceImpl() {
        initializeFields();
    }

    private void initializeFields() {
        patientFields = createPatientFields();
        sampleFields = createSampleFields();

        patientFields.forEach(f -> fieldCache.put(f.getFieldId(), f));
        sampleFields.forEach(f -> fieldCache.put(f.getFieldId(), f));
    }

    private List<ReportFieldDTO> createPatientFields() {
        List<ReportFieldDTO> fields = new ArrayList<>();

        fields.add(createField("patient.nationalId", "National ID", "patient", DataType.STRING, true, STRING_OPERATORS,
                "pat.nationalId", "adhoc.field.patient.nationalId"));

        fields.add(createField("patient.externalId", "External ID", "patient", DataType.STRING, true, STRING_OPERATORS,
                "pat.externalId", "adhoc.field.patient.externalId"));

        fields.add(createField("patient.gender", "Gender", "patient", DataType.STRING, true, ENUM_OPERATORS,
                "pat.gender", "adhoc.field.patient.gender"));

        fields.add(createField("patient.birthDate", "Date of Birth", "patient", DataType.DATE, true, DATE_OPERATORS,
                "pat.birthDate", "adhoc.field.patient.birthDate"));

        fields.add(createField("patient.firstName", "First Name", "patient", DataType.STRING, true, STRING_OPERATORS,
                "per.firstName", "adhoc.field.patient.firstName"));

        fields.add(createField("patient.lastName", "Last Name", "patient", DataType.STRING, true, STRING_OPERATORS,
                "per.lastName", "adhoc.field.patient.lastName"));

        return fields;
    }

    private List<ReportFieldDTO> createSampleFields() {
        List<ReportFieldDTO> fields = new ArrayList<>();

        fields.add(createField("sample.accessionNumber", "Lab Number", "sample", DataType.STRING, true,
                STRING_OPERATORS, "s.accessionNumber", "adhoc.field.sample.accessionNumber"));

        fields.add(createField("sample.collectionDate", "Collection Date", "sample", DataType.DATETIME, true,
                DATE_OPERATORS, "s.collectionDate", "adhoc.field.sample.collectionDate"));

        fields.add(createField("sample.receivedDate", "Received Date", "sample", DataType.DATETIME, true,
                DATE_OPERATORS, "s.receivedTimestamp", "adhoc.field.sample.receivedDate"));

        fields.add(createField("sample.enteredDate", "Entered Date", "sample", DataType.DATE, true, DATE_OPERATORS,
                "s.enteredDate", "adhoc.field.sample.enteredDate"));

        fields.add(createField("sample.statusId", "Status", "sample", DataType.STRING, true, ENUM_OPERATORS,
                "s.statusId", "adhoc.field.sample.status"));

        fields.add(createField("sample.priority", "Priority", "sample", DataType.ENUM, true, ENUM_OPERATORS,
                "s.priority", "adhoc.field.sample.priority"));

        fields.add(createField("sample.clientReference", "Client Reference", "sample", DataType.STRING, true,
                STRING_OPERATORS, "s.clientReference", "adhoc.field.sample.clientReference"));

        return fields;
    }

    private ReportFieldDTO createField(String fieldId, String displayName, String entityName, DataType dataType,
            boolean filterable, List<FilterOperator> operators, String propertyPath, String messageKey) {
        ReportFieldDTO field = new ReportFieldDTO(fieldId, displayName, entityName, dataType, filterable, operators,
                propertyPath);
        field.setMessageKey(messageKey);
        return field;
    }

    @Override
    public AvailableFieldsDTO getAvailableFields() {
        return new AvailableFieldsDTO(patientFields, sampleFields);
    }

    @Override
    public List<ReportFieldDTO> getPatientFields() {
        return new ArrayList<>(patientFields);
    }

    @Override
    public List<ReportFieldDTO> getSampleFields() {
        return new ArrayList<>(sampleFields);
    }

    @Override
    public ReportFieldDTO getFieldById(String fieldId) {
        return fieldCache.get(fieldId);
    }

    @Override
    public boolean validateFieldIds(List<String> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return false;
        }
        return fieldIds.stream().allMatch(fieldCache::containsKey);
    }
}
