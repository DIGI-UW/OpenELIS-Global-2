package org.openelisglobal.qaevent.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.qaevent.dao.NceCategoryDAO;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceCategoryServiceImpl extends AuditableBaseObjectServiceImpl<NceCategory, Integer>
        implements NceCategoryService {

    @Autowired
    protected NceCategoryDAO baseObjectDAO;

    NceCategoryServiceImpl() {
        super(NceCategory.class);
        this.auditTrailLog = true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceCategory> getAllNceCategories() {
        return baseObjectDAO.getAllNceCategory();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdValuePair> getActiveCategoriesAsIdValuePairs() {
        List<IdValuePair> result = new ArrayList<>();
        List<NceCategory> categories = baseObjectDAO.getAllNceCategory();
        for (NceCategory cat : categories) {
            Boolean active = cat.getActive();
            if (active == null || Boolean.TRUE.equals(active)) {
                result.add(new IdValuePair(cat.getId() != null ? String.valueOf(cat.getId()) : "",
                        cat.getLocalizedName()));
            }
        }
        return result;
    }

    @Override
    protected NceCategoryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
