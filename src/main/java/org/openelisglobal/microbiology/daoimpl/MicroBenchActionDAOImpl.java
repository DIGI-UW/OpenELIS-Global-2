package org.openelisglobal.microbiology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroBenchActionDAO;
import org.openelisglobal.microbiology.valueholder.MicroBenchAction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroBenchActionDAOImpl extends BaseDAOImpl<MicroBenchAction, String> implements MicroBenchActionDAO {
    public MicroBenchActionDAOImpl() {
        super(MicroBenchAction.class);
    }
}
