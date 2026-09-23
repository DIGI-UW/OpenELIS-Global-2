package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicrobiologyReferenceService {
    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroOrganism> getActiveOrganisms();

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroAntibiotic> getActiveAntibiotics();

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroAstPanel> getActiveAstPanels(MicroWorkflowType workflowType);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroCultureSetup> getActiveCultureSetups(MicroWorkflowType workflowType);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroCultureSetup getActiveCultureSetupForMethod(String methodId, MicroWorkflowType workflowType);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroPatientOriginOptions getPatientOrigins(String organizationId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    boolean isActivePatientOriginCode(String code);
}
