package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAPanelDAOImpl extends BaseDAOImpl<EQAPanel, Long> implements EQAPanelDAO {

    public EQAPanelDAOImpl() {
        super(EQAPanel.class);
    }
}
