package org.openelisglobal.panelterminology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.panelterminology.dao.PanelTerminologyMappingDAO;
import org.openelisglobal.panelterminology.valueholder.PanelTerminologyMapping;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PanelTerminologyMappingDAOImpl extends BaseDAOImpl<PanelTerminologyMapping, String>
        implements PanelTerminologyMappingDAO {

    public PanelTerminologyMappingDAOImpl() {
        super(PanelTerminologyMapping.class);
    }
}
