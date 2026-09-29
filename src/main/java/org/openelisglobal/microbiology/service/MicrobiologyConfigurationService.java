package org.openelisglobal.microbiology.service;

import java.sql.Date;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Application boundary for maintaining microbiology reference configuration.
 */
public interface MicrobiologyConfigurationService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroOrganism createOrganism(MicroOrganism organism);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroOrganism getOrCreateOrganism(String displayName, String whonetCode, String defaultAstPanelId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAntibiotic createAntibiotic(MicroAntibiotic antibiotic);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAntibiotic getOrCreateAntibiotic(String displayName, String whonetCode, String antibioticClass);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAstPanel createAstPanel(MicroAstPanel panel);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAstPanel getOrCreateAstPanel(String name, String workflowType, String organismGroup);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAstPanelAntibiotic addAntibioticToPanel(MicroAstPanelAntibiotic panelAntibiotic);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAstPanelAntibiotic getOrCreatePanelAntibiotic(String panelId, String antibioticId, int displayOrder);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroBreakpointStandard getOrCreateBreakpointStandard(String authority, String version, Date effectiveDate);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroBreakpointRule createBreakpointRule(MicroBreakpointRule rule);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroBreakpointRule getOrCreateBreakpointRule(MicroBreakpointRule rule);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroCultureSetup createCultureSetup(MicroCultureSetup setup);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroCultureSetup getOrCreateCultureSetup(MicroCultureSetup setup);
}
