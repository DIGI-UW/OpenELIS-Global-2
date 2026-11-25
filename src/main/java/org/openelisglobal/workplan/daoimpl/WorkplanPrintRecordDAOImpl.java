package org.openelisglobal.workplan.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.workplan.dao.WorkplanPrintRecordDAO;
import org.openelisglobal.workplan.valueholder.WorkplanPrintRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class WorkplanPrintRecordDAOImpl extends BaseDAOImpl<WorkplanPrintRecord, String>
        implements WorkplanPrintRecordDAO {
    public WorkplanPrintRecordDAOImpl() {
        super(WorkplanPrintRecord.class);
    }
}
