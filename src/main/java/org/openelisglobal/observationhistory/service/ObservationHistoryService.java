package org.openelisglobal.observationhistory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.Sample;

public interface ObservationHistoryService extends BaseObjectService<ObservationHistory, String> {
    ObservationHistory getById(ObservationHistory observation);

    List<ObservationHistory> getAll(Patient patient, Sample sample, String observationHistoryTypeId);

    List<ObservationHistory> getAll(Patient patient, Sample sample);

    ObservationHistory getObservationHistoriesBySampleIdAndType(String sampleId, String observationHistoryTypeId);

    List<ObservationHistory> getObservationHistoriesByPatientIdAndType(String patientId,
            String observationHistoryTypeId);

    List<ObservationHistory> getObservationHistoryByDictonaryValues(String dictionaryValue);

    List<ObservationHistory> getObservationHistoriesBySampleItemId(String sampleItemId);

    List<ObservationHistory> getObservationHistoriesByValueAndType(String value, String typeId, String valueType);

    List<ObservationHistory> getObservationHistoriesBySampleId(String sampleId);

    ObservationHistory getObservationForSample(ObservationType requestDate, String id);

    String getObservationTypeIdForType(ObservationType type);

    List<ObservationHistory> getObservationsByTypeAndValue(ObservationType type, String value);

    String getValueForSample(ObservationType type, String sampleId);

    String getMostRecentValueForPatient(ObservationType type, String patientId);

    String getRawValueForSample(ObservationType type, String sampleId);

    ObservationHistory getLastObservationForPatient(ObservationType type, String patientId);
}
