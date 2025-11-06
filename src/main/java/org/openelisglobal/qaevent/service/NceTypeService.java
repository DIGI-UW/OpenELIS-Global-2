package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceType;

public interface NceTypeService extends BaseObjectService<NceType, String> {

    List<NceType> getAllNceTypes();
}
