package org.openelisglobal.customfield.service;

import java.util.List;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.customfield.valueholder.CustomFieldValue;

public interface CustomFieldValueService extends BaseObjectService<CustomFieldValue, String> {

    List<CustomFieldValue> getValuesBySampleId(Integer sampleId);
}
