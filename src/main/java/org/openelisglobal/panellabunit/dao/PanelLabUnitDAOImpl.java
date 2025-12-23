package org.openelisglobal.panellabunit.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.panellabunit.valueholder.PanelLabUnit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PanelLabUnitDAOImpl extends BaseDAOImpl<PanelLabUnit, String> implements PanelLabUnitDAO {
    PanelLabUnitDAOImpl() {
        super(PanelLabUnit.class);
    }
}
