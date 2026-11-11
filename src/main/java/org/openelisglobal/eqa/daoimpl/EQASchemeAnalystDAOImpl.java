package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQASchemeAnalystDAO;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQASchemeAnalystDAOImpl extends BaseDAOImpl<EQASchemeAnalyst, Long> implements EQASchemeAnalystDAO {

    public EQASchemeAnalystDAOImpl() {
        super(EQASchemeAnalyst.class);
    }
}
