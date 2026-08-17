package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQACycleStateTransitionDAO;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQACycleStateTransitionDAOImpl extends BaseDAOImpl<EQACycleStateTransition, Long>
        implements EQACycleStateTransitionDAO {

    public EQACycleStateTransitionDAOImpl() {
        super(EQACycleStateTransition.class);
    }
}
