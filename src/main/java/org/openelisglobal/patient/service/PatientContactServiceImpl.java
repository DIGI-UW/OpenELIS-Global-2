package org.openelisglobal.patient.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patient.dao.PatientContactDAO;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientContactServiceImpl extends AuditableBaseObjectServiceImpl<PatientContact, String>
        implements PatientContactService {

    @Autowired
    private PatientContactDAO baseObjectDAO;

    public PatientContactServiceImpl() {
        super(PatientContact.class);
    }

    @Override
    protected PatientContactDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<PatientContact> getForPatient(String patientId) {
        return baseObjectDAO.getForPatient(patientId);
    }
}
