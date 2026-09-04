package org.openelisglobal.dataexchange.service.aggregatereporting;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportQueueType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
public interface ReportQueueTypeService extends BaseObjectService<ReportQueueType, String> {
    ReportQueueType getReportQueueTypeByName(String name);
}
