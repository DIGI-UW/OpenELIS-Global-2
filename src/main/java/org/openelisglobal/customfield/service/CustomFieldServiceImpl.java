package org.openelisglobal.customfield.service;

import java.util.List;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.customfield.dao.CustomFieldDAO;
import org.openelisglobal.customfield.valueholder.CustomField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomFieldServiceImpl extends BaseObjectServiceImpl<CustomField, String>
        implements CustomFieldService {

    @Autowired
    private CustomFieldDAO customFieldDAO;

    public CustomFieldServiceImpl() {
        super(CustomField.class);
    }

    @Override
    protected BaseDAO<CustomField, String> getBaseObjectDAO() {
        return customFieldDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomField> getActiveCustomFields() {
        return customFieldDAO.getAllMatchingOrdered("isActive", true, "sortOrder", false);
    }
}
