package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceCategory;

public interface NceCategoryService extends BaseObjectService<NceCategory, Integer> {
    List<NceCategory> getAllNceCategories();
}
