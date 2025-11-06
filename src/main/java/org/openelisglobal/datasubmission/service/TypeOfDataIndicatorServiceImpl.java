package org.openelisglobal.datasubmission.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.datasubmission.dao.TypeOfDataIndicatorDAO;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TypeOfDataIndicatorServiceImpl extends AuditableBaseObjectServiceImpl<TypeOfDataIndicator, String>
        implements TypeOfDataIndicatorService {
    @Autowired
    protected TypeOfDataIndicatorDAO baseObjectDAO;

    TypeOfDataIndicatorServiceImpl() {
        super(TypeOfDataIndicator.class);
    }

    @Override
    protected TypeOfDataIndicatorDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TypeOfDataIndicator typeOfIndicator) {
        getBaseObjectDAO().getData(typeOfIndicator);
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfDataIndicator getTypeOfDataIndicator(String id) {
        return getBaseObjectDAO().getTypeOfDataIndicator(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfDataIndicator> getAllTypeOfDataIndicator() {
        return getBaseObjectDAO().getAllTypeOfDataIndicator();
    }
}
