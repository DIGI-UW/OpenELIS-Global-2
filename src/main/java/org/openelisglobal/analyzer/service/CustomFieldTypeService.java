package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CustomFieldTypeService extends BaseObjectService<CustomFieldType, String> {
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    CustomFieldType createCustomFieldType(CustomFieldType customFieldType);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    CustomFieldType updateCustomFieldType(CustomFieldType customFieldType);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean validateFieldValue(String value, CustomFieldType customFieldType);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<CustomFieldType> getAllActiveTypes();
}
