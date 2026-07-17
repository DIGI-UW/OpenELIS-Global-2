package org.openelisglobal.qaevent.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.qaevent.dao.NceTypeDAO;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceTypeServiceImpl extends AuditableBaseObjectServiceImpl<NceType, Integer> implements NceTypeService {

    @Autowired
    protected NceTypeDAO baseObjectDAO;

    NceTypeServiceImpl() {
        super(NceType.class);
        this.auditTrailLog = true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceType> getAllNceTypes() {
        return baseObjectDAO.getAllNceType();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceType> getNceTypesByCategoryId(Integer categoryId) {
        return baseObjectDAO.getNceTypesByCategoryId(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdValuePair> getActiveTypesAsIdValuePairs() {
        List<IdValuePair> result = new ArrayList<>();
        List<NceType> types = baseObjectDAO.getAllNceType();
        for (NceType type : types) {
            if (type.getActive() == null || Boolean.TRUE.equals(type.getActive())) {
                result.add(new IdValuePair(String.valueOf(type.getId()), type.getLocalizedName()));
            }
        }
        return result;
    }

    @Override
    protected NceTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
