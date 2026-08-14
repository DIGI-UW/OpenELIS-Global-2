package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQARoundDAOImpl extends BaseDAOImpl<EQARound, Long> implements EQARoundDAO {

    public EQARoundDAOImpl() {
        super(EQARound.class);
    }
}
