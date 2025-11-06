package org.openelisglobal.qaevent.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.QaObservationTypeDAO;
import org.openelisglobal.qaevent.valueholder.QaObservationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QaObservationTypeServiceImpl extends AuditableBaseObjectServiceImpl<QaObservationType, String>
        implements QaObservationTypeService {
    @Autowired
    protected QaObservationTypeDAO baseObjectDAO;

    QaObservationTypeServiceImpl() {
        super(QaObservationType.class);
    }

    @Override
    protected QaObservationTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public QaObservationType getQaObservationTypeByName(String typeName) {
        return getBaseObjectDAO().getQaObservationTypeByName(typeName);
    }
}
