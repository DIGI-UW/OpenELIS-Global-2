package org.openelisglobal.qaevent.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.QaObservation;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
public interface QaObservationService extends BaseObjectService<QaObservation, String> {
    QaObservation getQaObservationByTypeAndObserved(String typeName, String observedType, String observedId);
}
