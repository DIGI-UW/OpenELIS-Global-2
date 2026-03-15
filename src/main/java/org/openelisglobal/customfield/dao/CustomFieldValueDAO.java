package org.openelisglobal.customfield.dao;

import java.util.List;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.customfield.valueholder.CustomFieldValue;

public interface CustomFieldValueDAO extends BaseDAO<CustomFieldValue, String> {

    List<CustomFieldValue> getValuesBySampleId(Integer sampleId);
}
