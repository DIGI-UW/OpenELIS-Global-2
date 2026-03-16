package org.openelisglobal.customfield.daoimpl;

import java.util.List;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.customfield.dao.CustomFieldValueDAO;
import org.openelisglobal.customfield.valueholder.CustomFieldValue;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CustomFieldValueDAOImpl extends BaseDAOImpl<CustomFieldValue, String>
        implements CustomFieldValueDAO {

    public CustomFieldValueDAOImpl() {
        super(CustomFieldValue.class);
    }

    @Override
    public List<CustomFieldValue> getValuesBySampleId(Integer sampleId) {
        return getAllMatching("sampleId", sampleId);
    }
}
