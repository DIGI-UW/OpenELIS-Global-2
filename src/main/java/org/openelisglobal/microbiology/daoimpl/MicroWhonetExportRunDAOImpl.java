package org.openelisglobal.microbiology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroWhonetExportRunDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetExportRun;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroWhonetExportRunDAOImpl extends BaseDAOImpl<MicroWhonetExportRun, String>
        implements MicroWhonetExportRunDAO {

    public MicroWhonetExportRunDAOImpl() {
        super(MicroWhonetExportRun.class);
    }
}
