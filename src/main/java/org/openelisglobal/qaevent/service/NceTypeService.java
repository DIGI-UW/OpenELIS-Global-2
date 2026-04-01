package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceType;

public interface NceTypeService extends BaseObjectService<NceType, Integer> {

    List<NceType> getAllNceTypes();

    /**
     * Get all NCE types for a specific category.
     *
     * @param categoryId the category ID
     * @return list of NCE types in that category
     */
    List<NceType> getNceTypesByCategoryId(Integer categoryId);
}
