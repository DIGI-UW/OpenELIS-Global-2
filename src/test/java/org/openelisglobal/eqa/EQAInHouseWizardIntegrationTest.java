package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.eqa.controller.rest.EQAPanelRestController;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQABlindingService;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQALabelPDFService;
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
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * OGC-612 [EQA V2.4] — the writes the in-house blinding wizard makes before it
 * seals: the cycle it blinds into, the panel and its samples, and the analyst
 * roster round-robin draws from. Sealing itself is covered by the blinding
 * suites.
 */
public class EQAInHouseWizardIntegrationTest extends EQASpineTestBase {

    private static final long ANALYTE = 9801L;
    private static final long TEST_IN_SCHEME = 9731L;
    private static final long ANALYTE_IN_SCHEME = 9831L;

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private EQABlindingService blindingService;

    @Autowired
    private EQALabelPDFService labelPDFService;

    // The shared test context leaves EQA controllers out of its scan.
    private EQAPanelRestController panelController;

    @Autowired
    private EQAPanelService panelService;

    @Autowired
    private EQAProgramService programService;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        panelController = new EQAPanelRestController(panelService, blindingService, labelPDFService, programService,
                cycleService);
    }

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

    private EQAProgram schemeCarrying(String name, long testId) {
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated) VALUES (?, ?, 'Y', now())"
                + " ON CONFLICT (id) DO NOTHING", ANALYTE_IN_SCHEME, "Wizard scheme analyte");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, ?, ?, 'Y', ?, now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                testId, "Wizard scheme test", "Wizard scheme test", UUID.randomUUID().toString(), testId);
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = ?", 99831);
        jdbc.update("INSERT INTO clinlims.test_analyte (id, test_id, analyte_id, lastupdated) VALUES (?, ?, ?, now())",
                99831, testId, ANALYTE_IN_SCHEME);
        EQAProgram scheme = insertScheme(name, EQASchemeType.IN_HOUSE, null);
        programService.assignTest(scheme.getId(), testId);
        return scheme;
    }

    private Map<String, Object> panelBody(EQAProgram scheme, String testId) {
        return Map.of("schemeId", scheme.getId(), "panelName", "Scheme-bound panel", "samples",
                List.of(Map.of("testId", testId, "targetValue", "4.52")));
    }

    private HttpServletRequest request() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(USER));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }

    @Test
    public void createPanel_refusesATestTheSchemeDoesNotCarry() {
        EQAProgram scheme = schemeCarrying("Wizard off-scheme test", TEST_IN_SCHEME);

        try {
            panelController.createPanel(request(), panelBody(scheme, "424242"));
            fail("a test outside the scheme is not panel material");
        } catch (IllegalArgumentException expected) {
            assertEquals("Test 424242 is not assigned to this scheme", expected.getMessage());
        }
        assertTrue(eqaPanelDAO.getAllMatching("scheme.id", scheme.getId()).isEmpty());
    }

    @Test
    public void createPanel_acceptsATestTheSchemeCarries() {
        EQAProgram scheme = schemeCarrying("Wizard on-scheme test", TEST_IN_SCHEME);

        panelController.createPanel(request(), panelBody(scheme, String.valueOf(TEST_IN_SCHEME)));

        List<EQAPanel> panels = eqaPanelDAO.getAllMatching("scheme.id", scheme.getId());
        assertEquals(1, panels.size());
        List<EQAPanelSample> samples = eqaPanelSampleDAO.getAllMatching("panel.id", panels.get(0).getId());
        assertEquals(1, samples.size());
        assertEquals(Long.valueOf(ANALYTE_IN_SCHEME), samples.get(0).getAnalyteId());
    }
}
