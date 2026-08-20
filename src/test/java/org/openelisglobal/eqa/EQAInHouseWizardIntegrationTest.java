package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQAPanelService;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-612 [EQA V2.4] — the writes the in-house blinding wizard makes before it
 * seals: the cycle it blinds into (FR-V2.4-01), the panel and its samples
 * (FR-V2.4-02), and the analyst roster round-robin draws from (FR-V2.4-03).
 * Sealing itself is covered by the T-22 suites.
 */
public class EQAInHouseWizardIntegrationTest extends EQASpineTestBase {

    private static final long ANALYTE = 9801L;

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private EQAPanelService panelService;

    @Autowired
    private EQAProgramService programService;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    private EQAPanelSample sample(String target) {
        EQAPanelSample sample = new EQAPanelSample();
        sample.setAnalyteId(ANALYTE);
        sample.setTargetValue(target);
        return sample;
    }

    private EQAPanel panelFor(EQAProgram scheme, EQACycle cycle) {
        EQAPanel panel = new EQAPanel();
        panel.setScheme(scheme);
        panel.setCycle(cycle);
        panel.setPanelName("Wizard panel");
        panel.setUnblindDate(Date.valueOf("2026-12-01"));
        panel.setAliquotsProduced(4);
        panel.setHomogeneityQcPassed(true);
        return panel;
    }

    @Test
    public void createCycle_takesTheSchemesNextNumber() {
        EQAProgram scheme = insertScheme("Wizard cycles", EQASchemeType.IN_HOUSE, null);

        EQACycle first = cycleService.create(scheme.getId(), null, "Round one", Date.valueOf("2026-09-01"),
                Date.valueOf("2026-10-01"), USER);
        EQACycle second = cycleService.create(scheme.getId(), null, "Round two", null, null, USER);

        assertEquals(Integer.valueOf(1), first.getCycleNumber());
        assertEquals(Integer.valueOf(2), second.getCycleNumber());
        assertEquals(EQACycleStatus.PLANNED, first.getStatus());
        assertNotNull(readBack(first.getId()).getCreatedBy());
    }

    @Test
    public void createCycle_refusesADuplicateNumberWithAReadableMessage() {
        EQAProgram scheme = insertScheme("Wizard dupes", EQASchemeType.IN_HOUSE, null);
        cycleService.create(scheme.getId(), 7, "Seven", null, null, USER);

        try {
            cycleService.create(scheme.getId(), 7, "Seven again", null, null, USER);
            fail("uq_eqa_cycle_scheme_number must not be reachable through create");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("already has cycle 7"));
        }
    }

    @Test
    public void createPanel_startsPreparingAndGeneratesBlindCodes() {
        EQAProgram scheme = insertScheme("Wizard panel", EQASchemeType.IN_HOUSE, null);
        EQACycle cycle = cycleService.create(scheme.getId(), null, "Blind round", null, null, USER);

        EQAPanel panel = panelService.create(panelFor(scheme, cycle), List.of(sample("4.52"), sample("5.10")), USER);

        assertEquals(EQAPanelStatus.PREPARING, panel.getStatus());
        List<EQAPanelSample> samples = eqaPanelSampleDAO.getAllMatchingOrdered("panel.id", panel.getId(), "sampleCode",
                false);
        assertEquals(2, samples.size());
        assertEquals("S01", samples.get(0).getSampleCode());
        // The blind code becomes the order's accession number at distribution, so it
        // has to be unique lab-wide — hence the panel id in it.
        assertEquals("IH-" + panel.getId() + "-01", samples.get(0).getBlindCode());
        assertEquals("IH-" + panel.getId() + "-02", samples.get(1).getBlindCode());
    }

    @Test
    public void createPanel_refusesAPanelWithNoSamples() {
        EQAProgram scheme = insertScheme("Wizard empty", EQASchemeType.IN_HOUSE, null);
        EQACycle cycle = cycleService.create(scheme.getId(), null, "Empty round", null, null, USER);

        try {
            panelService.create(panelFor(scheme, cycle), List.of(), USER);
            fail("a panel with no samples has nothing to blind");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("at least one sample"));
        }
    }

    @Test
    public void setAnalysts_replacesTheRosterWithoutTrippingItsUniqueConstraint() {
        EQAProgram scheme = insertScheme("Wizard roster", EQASchemeType.IN_HOUSE, null);

        programService.setAnalysts(scheme.getId(), List.of(ADMIN_USER_ID), USER);
        // The same analyst again: a blind re-insert would violate
        // uq_eqa_scheme_analyst_scheme_user.
        List<EQASchemeAnalyst> roster = programService.setAnalysts(scheme.getId(), List.of(ADMIN_USER_ID), USER);

        assertEquals(1, roster.size());
        assertEquals(Long.valueOf(ADMIN_USER_ID), roster.get(0).getSystemUserId());

        assertTrue(programService.setAnalysts(scheme.getId(), List.of(), USER).isEmpty());
    }
}
