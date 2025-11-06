package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qaevent.dao.NceCategoryDAO;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceCategoryServiceImpl extends AuditableBaseObjectServiceImpl<NceCategory, String>
        implements NceCategoryService {

    @Autowired
    protected NceCategoryDAO baseObjectDAO;

    NceCategoryServiceImpl() {
        super(NceCategory.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceCategory> getAllNceCategories() {
        List<NceCategory> nceCategoryList = baseObjectDAO.getAllNceCategory();
        for (NceCategory category : nceCategoryList) {
            category.setName(MessageUtil.getMessage(category.getDisplayKey()));
        }
        return nceCategoryList;
    }

    @Override
    protected NceCategoryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
