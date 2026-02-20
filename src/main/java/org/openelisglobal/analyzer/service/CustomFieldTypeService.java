package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.common.service.BaseObjectService;

public interface CustomFieldTypeService extends BaseObjectService<CustomFieldType, String> {
    CustomFieldType createCustomFieldType(CustomFieldType customFieldType);

    CustomFieldType updateCustomFieldType(CustomFieldType customFieldType);

    boolean validateFieldValue(String value, CustomFieldType customFieldType);

    List<CustomFieldType> getAllActiveTypes();
}
