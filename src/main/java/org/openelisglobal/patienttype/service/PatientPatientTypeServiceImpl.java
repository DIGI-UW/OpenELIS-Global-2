package org.openelisglobal.patienttype.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patient.service.PatientTypeService;
import org.openelisglobal.patienttype.dao.PatientPatientTypeDAO;
import org.openelisglobal.patienttype.valueholder.PatientPatientType;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientPatientTypeServiceImpl extends AuditableBaseObjectServiceImpl<PatientPatientType, String>
        implements PatientPatientTypeService {
    @Autowired
    protected PatientPatientTypeDAO baseObjectDAO;
    @Autowired
    private PatientTypeService patientTypeService;

    PatientPatientTypeServiceImpl() {
        super(PatientPatientType.class);
    }

    @Override
    protected PatientPatientTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientPatientType getPatientPatientTypeForPatient(String id) {
        return getMatch("patientId", id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientType getPatientTypeForPatient(String id) {
        PatientPatientType patientPatientType = getPatientPatientTypeForPatient(id);

        if (patientPatientType != null) {
            PatientType patientType = new PatientType();
            patientType.setId(patientPatientType.getPatientTypeId());
            patientTypeService.getData(patientType);

            return patientType;
        }
        return null;
    }
}
