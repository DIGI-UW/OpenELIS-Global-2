package org.openelisglobal.dataexchange.service.aggregatereporting;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dataexchange.aggregatereporting.dao.ReportQueueTypeDAO;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportQueueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportQueueTypeServiceImpl extends AuditableBaseObjectServiceImpl<ReportQueueType, String>
        implements ReportQueueTypeService {
    @Autowired
    protected ReportQueueTypeDAO baseObjectDAO;

    ReportQueueTypeServiceImpl() {
        super(ReportQueueType.class);
    }

    @Override
    protected ReportQueueTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportQueueType getReportQueueTypeByName(String name) {
        return getBaseObjectDAO().getReportQueueTypeByName(name);
    }
}
