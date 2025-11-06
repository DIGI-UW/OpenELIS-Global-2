package org.openelisglobal.observationhistory.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.observationhistory.dao.ObservationHistoryDAO;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class ObservationHistoryServiceImpl extends AuditableBaseObjectServiceImpl<ObservationHistory, String>
        implements ObservationHistoryService {

    public enum ObservationType {
        INITIAL_SAMPLE_CONDITION("initialSampleCondition"), PAYMENT_STATUS("paymentStatus"),
        REQUEST_DATE("requestDate"), NEXT_VISIT_DATE("nextVisitDate"), REFERRING_SITE("referringSite"),
        REFERRERS_PATIENT_ID("referrersPatientId"), BILLING_REFERENCE_NUMBER("billingRefNumber"),
        TEST_LOCATION_CODE("testLocationCode"), TEST_LOCATION_CODE_OTHER("testLocationCodeOther"), PROGRAM("program"),
        HIV_STATUS("hivStatus"), VL_PREGNANCY("vlPregnancy"), VL_SUCKLE("vlSuckle"), TB_ORDER_REASON("TbOrderReason"),
        TB_DIAGNOSTIC_REASON("TbDiagnosticReason"), TB_FOLLOWUP_REASON("TbFollowupReason"),
        TB_FOLLOWUP_PERIOD_LINE1("TbFollowupReasonPeriodLine1"),
        TB_FOLLOWUP_PERIOD_LINE2("TbFollowupReasonPeriodLine2"), TB_ANALYSIS_METHOD("TbAnalysisMethod"),
        TB_SAMPLE_ASPECT("TbSampleAspects");

        private String dbName;

        private ObservationType(String dbName) {
            this.dbName = dbName;
        }

        public String getDatabaseName() {
            return dbName;
        }
    }

    private final Map<ObservationType, String> observationTypeToIdMap = new EnumMap<>(ObservationType.class);

    @Autowired
    private ObservationHistoryDAO baseObjectDAO;

    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;

    ObservationHistoryServiceImpl() {
        super(ObservationHistory.class);
        this.auditTrailLog = true;
    }

    @Override
    protected ObservationHistoryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public String insert(ObservationHistory observationHistory) {
        observationHistory.setValue(observationHistory.getValue().trim());
        return super.insert(observationHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationHistory> getAll(Patient patient, Sample sample) {
        return baseObjectDAO.getAll(patient, sample);
    }

    @Override
    @Transactional(readOnly = true)
    public String getObservationTypeIdForType(ObservationType type) {
        if (observationTypeToIdMap.isEmpty()) {
            initialize();
        }
        return observationTypeToIdMap.get(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationHistory> getObservationsByTypeAndValue(ObservationType type, String value) {
        if (observationTypeToIdMap.isEmpty()) {
            initialize();
        }
        String typeId = getObservationTypeIdForType(type);

        if (!GenericValidator.isBlankOrNull(typeId)) {
            return baseObjectDAO.getObservationHistoriesByValueAndType(value, typeId, ValueType.LITERAL.getCode());
        } else {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getValueForSample(ObservationType type, String sampleId) {
        ObservationHistory observation = getObservationForSample(type, sampleId);
        return getValueForObservation(observation);
    }

    private String getValueForObservation(ObservationHistory observation) {
        if (observation != null) {
            if (observation.getValueType().equals(ObservationHistory.ValueType.LITERAL.getCode())) {
                return observation.getValue();
            } else {
                if (!GenericValidator.isBlankOrNull(observation.getValue())) {
                    return dictionaryService.getDataForId(observation.getValue()).getLocalizedName();
                }
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public String getMostRecentValueForPatient(ObservationType type, String patientId) {
        ObservationHistory observation = getLastObservationForPatient(type, patientId);
        return getValueForObservation(observation);
    }

    @Override
    @Transactional(readOnly = true)
    public String getRawValueForSample(ObservationType type, String sampleId) {
        ObservationHistory observation = getObservationForSample(type, sampleId);
        return observation != null ? observation.getValue() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public ObservationHistory getObservationForSample(ObservationType type, String sampleId) {
        if (observationTypeToIdMap.isEmpty()) {
            initialize();
        }
        String typeId = getObservationTypeIdForType(type);

        if (!GenericValidator.isBlankOrNull(typeId)) {
            return baseObjectDAO.getObservationHistoriesBySampleIdAndType(sampleId, typeId);
        } else {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ObservationHistory getLastObservationForPatient(ObservationType type, String patientId) {
        if (observationTypeToIdMap.isEmpty()) {
            initialize();
        }

        String typeId = getObservationTypeIdForType(type);

        if (!GenericValidator.isBlankOrNull(typeId)) {
            List<ObservationHistory> observationList = baseObjectDAO
                    .getObservationHistoriesByPatientIdAndType(patientId, typeId);
            if (!observationList.isEmpty()) {
                return observationList.get(0); // sorted descending
            }
        }

        return null;
    }

    private void initialize() {
        ObservationHistoryType oht;

        for (ObservationType type : ObservationType.values()) {
            oht = observationHistoryTypeService.getByName(type.getDatabaseName());
            if (oht != null) {
                observationTypeToIdMap.put(type, oht.getId());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ObservationHistory getById(ObservationHistory observation) {
        return getBaseObjectDAO().getById(observation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationHistory> getAll(Patient patient, Sample sample, String observationHistoryTypeId) {
        return getBaseObjectDAO().getAll(patient, sample, observationHistoryTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public ObservationHistory getObservationHistoriesBySampleIdAndType(String sampleId,
            String observationHistoryTypeId) {
        return getBaseObjectDAO().getObservationHistoriesBySampleIdAndType(sampleId, observationHistoryTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationHistory> getObservationHistoriesByPatientIdAndType(String patientId,
            String observationHistoryTypeId) {
        return getBaseObjectDAO().getObservationHistoriesByPatientIdAndType(patientId, observationHistoryTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationHistory> getObservationHistoryByDictonaryValues(String dictionaryValue) {
        return getBaseObjectDAO().getObservationHistoryByDictonaryValues(dictionaryValue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationHistory> getObservationHistoriesBySampleItemId(String sampleItemId) {
        return getBaseObjectDAO().getObservationHistoriesBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationHistory> getObservationHistoriesByValueAndType(String value, String typeId,
            String valueType) {
        return getBaseObjectDAO().getObservationHistoriesByValueAndType(value, typeId, valueType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationHistory> getObservationHistoriesBySampleId(String sampleId) {
        return getBaseObjectDAO().getObservationHistoriesBySampleId(sampleId);
    }
}
