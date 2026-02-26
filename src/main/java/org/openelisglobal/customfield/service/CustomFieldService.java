package org.openelisglobal.customfield.service;

import java.util.List;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.customfield.valueholder.CustomField;

public interface CustomFieldService extends BaseObjectService<CustomField, String> {

    List<CustomField> getActiveCustomFields();
}
