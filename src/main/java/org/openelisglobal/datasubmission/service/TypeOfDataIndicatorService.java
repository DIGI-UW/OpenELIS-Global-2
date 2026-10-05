package org.openelisglobal.datasubmission.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
public interface TypeOfDataIndicatorService extends BaseObjectService<TypeOfDataIndicator, String> {
    void getData(TypeOfDataIndicator typeOfIndicator);

    TypeOfDataIndicator getTypeOfDataIndicator(String id);

    List<TypeOfDataIndicator> getAllTypeOfDataIndicator();
}
