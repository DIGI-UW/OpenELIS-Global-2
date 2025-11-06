package org.openelisglobal.qaevent.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.QaObservationDAO;
import org.openelisglobal.qaevent.valueholder.QaObservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QaObservationServiceImpl extends AuditableBaseObjectServiceImpl<QaObservation, String>
        implements QaObservationService {
    @Autowired
    protected QaObservationDAO baseObjectDAO;

    QaObservationServiceImpl() {
        super(QaObservation.class);
    }

    @Override
    protected QaObservationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public QaObservation getQaObservationByTypeAndObserved(String typeName, String observedType, String observedId) {
        return getBaseObjectDAO().getQaObservationByTypeAndObserved(typeName, observedType, observedId);
    }
}
