package org.openelisglobal.patient.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patienttype.dao.PatientTypeDAO;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientTypeServiceImpl extends AuditableBaseObjectServiceImpl<PatientType, String>
        implements PatientTypeService {

    @Autowired
    private PatientTypeDAO baseObjectDAO;

    public PatientTypeServiceImpl() {
        super(PatientType.class);
    }

    @Override
    protected PatientTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientType> getAllPatientTypes() throws LIMSRuntimeException {
        return getBaseObjectDAO().getAllPatientTypes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientType> getPageOfPatientType(int startingRecNo) throws LIMSRuntimeException {
        return getBaseObjectDAO().getPageOfPatientType(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(PatientType patientType) throws LIMSRuntimeException {
        getBaseObjectDAO().getData(patientType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientType> getPatientTypes(String filter) throws LIMSRuntimeException {
        return getBaseObjectDAO().getPatientTypes(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientType getPatientTypeByName(PatientType patientType) throws LIMSRuntimeException {
        return getBaseObjectDAO().getPatientTypeByName(patientType);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPatientTypeCount() throws LIMSRuntimeException {
        return getBaseObjectDAO().getTotalPatientTypeCount();
    }

    @Override
    public String insert(PatientType patientType) {
        if (duplicatePatientTypeExists(patientType)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + patientType.getDescription());
        }
        return super.insert(patientType);
    }

    @Override
    public PatientType save(PatientType patientType) {
        if (duplicatePatientTypeExists(patientType)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + patientType.getDescription());
        }
        return super.save(patientType);
    }

    @Override
    public PatientType update(PatientType patientType) {
        if (duplicatePatientTypeExists(patientType)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + patientType.getDescription());
        }
        return super.update(patientType);
    }

    private boolean duplicatePatientTypeExists(PatientType patientType) {
        return baseObjectDAO.duplicatePatientTypeExists(patientType);
    }
}
