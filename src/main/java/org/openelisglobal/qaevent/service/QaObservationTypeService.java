package org.openelisglobal.qaevent.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.QaObservationType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
public interface QaObservationTypeService extends BaseObjectService<QaObservationType, String> {
    QaObservationType getQaObservationTypeByName(String typeName);
}
