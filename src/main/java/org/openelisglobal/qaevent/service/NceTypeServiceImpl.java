package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.NceTypeDAO;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceTypeServiceImpl extends AuditableBaseObjectServiceImpl<NceType, String> implements NceTypeService {

    @Autowired
    protected NceTypeDAO baseObjectDAO;

    NceTypeServiceImpl() {
        super(NceType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceType> getAllNceTypes() {
        return baseObjectDAO.getAllNceType();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceType> getNceTypesByCategoryId(String categoryId) {
        return baseObjectDAO.getNceTypesByCategoryId(categoryId);
    }

    @Override
    protected NceTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
