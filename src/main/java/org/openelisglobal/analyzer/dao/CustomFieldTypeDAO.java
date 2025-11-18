package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.common.dao.BaseDAO;

public interface CustomFieldTypeDAO extends BaseDAO<CustomFieldType, String> {
    List<CustomFieldType> findAllActive();

    CustomFieldType findByName(String name);

    CustomFieldType findByTypeName(String typeName);
}
