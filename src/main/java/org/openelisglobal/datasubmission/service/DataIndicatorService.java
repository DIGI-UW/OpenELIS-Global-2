package org.openelisglobal.datasubmission.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.datasubmission.valueholder.DataIndicator;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
public interface DataIndicatorService extends BaseObjectService<DataIndicator, String> {

    DataIndicator getIndicatorByTypeYearMonth(TypeOfDataIndicator type, int year, int month);

    List<DataIndicator> getIndicatorsByStatus(String status);
}
