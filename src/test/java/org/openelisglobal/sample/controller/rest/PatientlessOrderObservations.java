package org.openelisglobal.sample.controller.rest;

import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.sample.valueholder.Sample;

final class PatientlessOrderObservations {
    private PatientlessOrderObservations() {
    }

    static void create(Sample sample, ObservationHistoryService service, ObservationHistoryTypeService types) {
        create(sample, service, types, ObservationType.ENV_WORKFLOW_TYPE, "environmental");
        create(sample, service, types, ObservationType.VS_COLLECTION_SITE_NAME, "CPHL");
        service.refreshTypeIdCache();
    }

    private static void create(Sample sample, ObservationHistoryService service, ObservationHistoryTypeService types,
            ObservationType type, String value) {
        ObservationHistoryType reference = types.getByName(type.getDatabaseName());
        if (reference == null) {
            reference = new ObservationHistoryType();
            reference.setTypeName(type.getDatabaseName());
            reference.setDescription(type.getDatabaseName());
            reference.setId(types.insert(reference));
        }
        ObservationHistory observation = new ObservationHistory();
        observation.setSampleId(sample.getId());
        observation.setObservationHistoryTypeId(reference.getId());
        observation.setValueType(ObservationHistory.ValueType.LITERAL);
        observation.setValue(value);
        service.insert(observation);
    }
}
