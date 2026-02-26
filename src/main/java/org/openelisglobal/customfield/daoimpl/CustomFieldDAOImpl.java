package org.openelisglobal.customfield.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.customfield.dao.CustomFieldDAO;
import org.openelisglobal.customfield.valueholder.CustomField;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CustomFieldDAOImpl extends BaseDAOImpl<CustomField, String> implements CustomFieldDAO {

    public CustomFieldDAOImpl() {
        super(CustomField.class);
    }
}
