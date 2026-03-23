package org.openelisglobal.patient.merge.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.patient.merge.valueholder.PatientMergeAudit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PatientMergeAuditDAOImpl extends BaseDAOImpl<PatientMergeAudit, Long> implements PatientMergeAuditDAO {

    public PatientMergeAuditDAOImpl() {
        super(PatientMergeAudit.class);
    }
}
