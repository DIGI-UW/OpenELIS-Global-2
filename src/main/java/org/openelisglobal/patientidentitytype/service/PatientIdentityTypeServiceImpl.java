package org.openelisglobal.patientidentitytype.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patientidentitytype.dao.PatientIdentityTypeDAO;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientIdentityTypeServiceImpl extends AuditableBaseObjectServiceImpl<PatientIdentityType, String>
        implements PatientIdentityTypeService {
    @Autowired
    protected PatientIdentityTypeDAO baseObjectDAO;

    public PatientIdentityTypeServiceImpl() {
        super(PatientIdentityType.class);
    }

    @Override
    protected PatientIdentityTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientIdentityType getNamedIdentityType(String name) {
        return getBaseObjectDAO().getNamedIdentityType(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientIdentityType> getAllPatientIdenityTypes() {
        return getBaseObjectDAO().getAllPatientIdenityTypes();
    }

    @Override
    public String insert(PatientIdentityType patientIdentityType) {
        if (duplicatePatientIdentityTypeExists(patientIdentityType)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + patientIdentityType.getIdentityType());
        }
        return super.insert(patientIdentityType);
    }

    @Override
    public PatientIdentityType save(PatientIdentityType patientIdentityType) {
        if (duplicatePatientIdentityTypeExists(patientIdentityType)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + patientIdentityType.getIdentityType());
        }
        return super.save(patientIdentityType);
    }

    @Override
    public PatientIdentityType update(PatientIdentityType patientIdentityType) {
        if (duplicatePatientIdentityTypeExists(patientIdentityType)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + patientIdentityType.getIdentityType());
        }
        return super.update(patientIdentityType);
    }

    private boolean duplicatePatientIdentityTypeExists(PatientIdentityType patientIdentityType) {
        return baseObjectDAO.duplicatePatientIdentityTypeExists(patientIdentityType);
    }
}
