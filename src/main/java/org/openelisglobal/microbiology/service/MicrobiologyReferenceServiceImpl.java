package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroCultureSetupDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicrobiologyReferenceServiceImpl implements MicrobiologyReferenceService {

    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;
    private final MicroAstPanelDAO astPanelDAO;
    private final MicroCultureSetupDAO cultureSetupDAO;

    public MicrobiologyReferenceServiceImpl(MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO,
            MicroAstPanelDAO astPanelDAO, MicroCultureSetupDAO cultureSetupDAO) {
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
        this.astPanelDAO = astPanelDAO;
        this.cultureSetupDAO = cultureSetupDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroOrganism> getActiveOrganisms() {
        return organismDAO.getActiveOrganisms();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAntibiotic> getActiveAntibiotics() {
        return antibioticDAO.getActiveAntibiotics();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstPanel> getActiveAstPanels(MicroWorkflowType workflowType) {
        return astPanelDAO.getActivePanelsByWorkflowType(workflowType.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCultureSetup> getActiveCultureSetups(MicroWorkflowType workflowType) {
        return cultureSetupDAO.getActiveSetupsByWorkflowType(workflowType.name());
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCultureSetup getActiveCultureSetupForMethod(String methodId, MicroWorkflowType workflowType) {
        return cultureSetupDAO.getActiveSetupForMethod(methodId, workflowType.name());
    }
}
