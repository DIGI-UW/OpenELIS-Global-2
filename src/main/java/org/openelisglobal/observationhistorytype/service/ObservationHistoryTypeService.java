package org.openelisglobal.observationhistorytype.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface ObservationHistoryTypeService extends BaseObjectService<ObservationHistoryType, String> {
    ObservationHistoryType getByName(String name);
}
