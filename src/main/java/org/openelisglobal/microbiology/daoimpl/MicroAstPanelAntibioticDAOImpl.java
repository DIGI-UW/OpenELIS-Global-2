package org.openelisglobal.microbiology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstPanelAntibioticDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstPanelAntibioticDAOImpl extends BaseDAOImpl<MicroAstPanelAntibiotic, String>
        implements MicroAstPanelAntibioticDAO {

    public MicroAstPanelAntibioticDAOImpl() {
        super(MicroAstPanelAntibiotic.class);
    }
}
