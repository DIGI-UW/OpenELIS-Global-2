package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

public interface MicrobiologyReferenceService {
    List<MicroOrganism> getActiveOrganisms();

    List<MicroAntibiotic> getActiveAntibiotics();

    List<MicroAstPanel> getActiveAstPanels(MicroWorkflowType workflowType);

    List<MicroCultureSetup> getActiveCultureSetups(MicroWorkflowType workflowType);

    MicroCultureSetup getActiveCultureSetupForMethod(String methodId, MicroWorkflowType workflowType);
}
