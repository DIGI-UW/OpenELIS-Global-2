package org.openelisglobal.microbiology.service;

import java.sql.Date;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

/**
 * Application boundary for maintaining microbiology reference configuration.
 */
public interface MicrobiologyConfigurationService {

    MicroOrganism createOrganism(MicroOrganism organism);

    MicroOrganism getOrCreateOrganism(String displayName, String whonetCode, String organismGroup);

    MicroAntibiotic createAntibiotic(MicroAntibiotic antibiotic);

    MicroAntibiotic getOrCreateAntibiotic(String displayName, String whonetCode, String antibioticClass);

    MicroAstPanel createAstPanel(MicroAstPanel panel);

    MicroAstPanel getOrCreateAstPanel(String name, String workflowType, String organismGroup);

    MicroAstPanelAntibiotic addAntibioticToPanel(MicroAstPanelAntibiotic panelAntibiotic);

    MicroAstPanelAntibiotic getOrCreatePanelAntibiotic(String panelId, String antibioticId, int displayOrder);

    MicroBreakpointStandard getOrCreateBreakpointStandard(String authority, String version, Date effectiveDate);

    MicroBreakpointRule createBreakpointRule(MicroBreakpointRule rule);

    MicroBreakpointRule getOrCreateBreakpointRule(MicroBreakpointRule rule);

    MicroCultureSetup createCultureSetup(MicroCultureSetup setup);

    MicroCultureSetup getOrCreateCultureSetup(MicroCultureSetup setup);
}
