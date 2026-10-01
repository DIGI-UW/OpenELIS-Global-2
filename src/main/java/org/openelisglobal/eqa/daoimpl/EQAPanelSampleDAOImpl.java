package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAPanelSampleDAOImpl extends BaseDAOImpl<EQAPanelSample, Long> implements EQAPanelSampleDAO {

    public EQAPanelSampleDAOImpl() {
        super(EQAPanelSample.class);
    }
}
