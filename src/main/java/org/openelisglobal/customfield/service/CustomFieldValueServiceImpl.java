package org.openelisglobal.customfield.service;

import java.util.List;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.customfield.dao.CustomFieldValueDAO;
import org.openelisglobal.customfield.valueholder.CustomFieldValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomFieldValueServiceImpl extends BaseObjectServiceImpl<CustomFieldValue, String>
        implements CustomFieldValueService {

    @Autowired
    private CustomFieldValueDAO customFieldValueDAO;

    public CustomFieldValueServiceImpl() {
        super(CustomFieldValue.class);
    }

    @Override
    protected BaseDAO<CustomFieldValue, String> getBaseObjectDAO() {
        return customFieldValueDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomFieldValue> getValuesBySampleId(Integer sampleId) {
        return customFieldValueDAO.getValuesBySampleId(sampleId);
    }
}
