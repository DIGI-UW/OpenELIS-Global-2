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

    /**
     * The configured culture setup for a test method, if any. This is catalogue
     * metadata, it describes how a method is set up, not any case's data, and the
     * order-entry test picker reads it to decorate each selectable method. Gating
     * it on PRIV_MICRO_VIEW alone made the whole picker 403 for Reception, so it
     * also accepts PRIV_CATALOGUE_VIEW. Microbiology CASE data stays on
     * PRIV_MICRO_VIEW throughout the rest of this interface.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_MICRO_VIEW','PRIV_CATALOGUE_VIEW')")
    MicroCultureSetup getActiveCultureSetupForMethod(String methodId, MicroWorkflowType workflowType);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroPatientOriginOptions getPatientOrigins(String organizationId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    boolean isActivePatientOriginCode(String code);
}
