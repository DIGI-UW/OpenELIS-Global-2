package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQACycleDAOImpl extends BaseDAOImpl<EQACycle, Long> implements EQACycleDAO {

    public EQACycleDAOImpl() {
        super(EQACycle.class);
    }
}
