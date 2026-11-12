package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.service.MicroAstInterpretationService;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicroWhonetAntibioticConfigurationHandler;
import org.openelisglobal.microbiology.service.MicroWhonetBreakpointConfigurationHandler;
import org.openelisglobal.microbiology.service.MicroWhonetOrganismConfigurationHandler;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs on rows cut unchanged from WHONET's Breakpoints.txt, release v26.3.26.
 */
public class MicroWhonetBreakpointEditionIntegrationTest extends BaseWebContextSensitiveTest {
    private static final String FILE = "testdata/microbiology/whonet-breakpoints-editions.txt";
    private static final String PACKAGE_FILE = "pkg--amr-reference-clsi--2026.4--Breakpoints.txt";

    @Autowired
    private MicroWhonetOrganismConfigurationHandler organismTable;
    @Autowired
    private MicroWhonetAntibioticConfigurationHandler antibioticTable;
    @Autowired
    private MicroWhonetBreakpointConfigurationHandler breakpointTable;
    @Autowired
    private MicroOrganismDAO organisms;
    @Autowired
    private MicroAntibioticDAO antibiotics;
    @Autowired
    private MicroBreakpointStandardDAO standards;
    @Autowired
    private MicroBreakpointRuleDAO rules;
    @Autowired
    private MicroBreakpointService breakpoints;
    @Autowired
    private MicroAstInterpretationService interpreter;

    @Before
    public void whonetOrganismsAndAntibiotics() throws Exception {
        executeDataSetWithStateManagement("testdata/microbiology/whonet-breakpoint-match.xml");
        load(organismTable, "testdata/microbiology/whonet-organisms.txt", "Organisms.txt");
        load(antibioticTable, "testdata/microbiology/whonet-antibiotics.txt", "Antibiotics.txt");
    }

    private static void load(org.openelisglobal.configuration.service.DomainConfigurationHandler handler,
            String resource, String fileName) throws Exception {
        try (InputStream file = MicroWhonetBreakpointEditionIntegrationTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            handler.processConfiguration(file, fileName);
        }
    }

    private String fileText() throws Exception {
        try (InputStream file = getClass().getClassLoader().getResourceAsStream(FILE)) {
            return new String(file.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void load(String text, String fileName) throws Exception {
        breakpointTable.processConfiguration(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), fileName);
    }

    private MicroBreakpointStandard edition(String authority, String version) {
        return standards.findByAuthorityAndVersion(authority, version).orElseThrow();
    }

    private List<MicroBreakpointRule> rulesOf(String authority, String version) {
        return rules.getByStandardId(edition(authority, version).getId());
    }

    private MicroBreakpointRule rule(String authority, String version, String antibiotic, String type, String site) {
        String antibioticId = antibiotics.findByWhonetCodeIgnoreCase(antibiotic).orElseThrow().getId();
        return rulesOf(authority, version).stream()
                .filter(rule -> antibioticId.equals(rule.getAntibioticId()) && type.equals(rule.getBreakpointType())
                        && "EBC".equals(rule.getOrganismCode())
                        && java.util.Objects.equals(site, rule.getSiteOfInfection()))
                .findFirst().orElseThrow();
    }

    @Test
    public void theThreeLatestYearsOfClsiAndEucastBecomeInactiveEditions() throws Exception {
        load(fileText(), PACKAGE_FILE);

        assertEquals(6, standards.getAll().size());
        for (String authority : List.of("CLSI", "EUCAST")) {
            for (String year : List.of("2024", "2025", "2026")) {
                MicroBreakpointStandard edition = edition(authority, year + "-2026.4");
                assertEquals("N", edition.getIsActive());
                assertEquals("LOADED", edition.getLifecycleStatus());
                assertNotNull(edition.getConfigurationHash());
            }
            assertFalse(standards.findByAuthorityAndVersion(authority, "2023-2026.4").isPresent());
        }
    }

    @Test
    public void aRuleKeepsWhonetsOrganismLevelSiteAndValues() throws Exception {
        load(fileText(), PACKAGE_FILE);

        MicroBreakpointRule disk = rule("CLSI", "2026-2026.4", "AMK", "ZONE", null);
        assertEquals("FAMILY_CODE", disk.getOrganismCodeType());
        assertEquals("30 mcg", disk.getDiskContent());
        assertEquals(0, new BigDecimal("20").compareTo(disk.getSusceptibleValue()));
        assertEquals(0, new BigDecimal("17").compareTo(disk.getIntermediateLowerValue()));
        assertEquals(0, new BigDecimal("19").compareTo(disk.getIntermediateUpperValue()));
        assertEquals(0, new BigDecimal("16").compareTo(disk.getResistantValue()));
        assertTrue(disk.isResistantInclusive());
        assertNull(disk.getOrganismId());

        MicroBreakpointRule urinary = rule("EUCAST", "2026-2026.4", "AMC", "ZONE",
                "Oral, Infections originating from the urinary tract");
        assertEquals("20/10 mcg", urinary.getDiskContent());
        assertEquals(0, new BigDecimal("49").compareTo(urinary.getIntermediateUpperValue()));
    }

    @Test
    public void onlyHumanClinicalBreakpointsBecomeRules() throws Exception {
        load(fileText(), PACKAGE_FILE);

        // The file's CLSI 2026 rows also hold one animal and one ECOFF row.
        assertEquals(8, rulesOf("CLSI", "2026-2026.4").size());
    }

    @Test
    public void aRuleWithoutValuesIsKeptSoItStillBlocksABroaderOne() throws Exception {
        load(fileText(), PACKAGE_FILE);

        MicroBreakpointRule salmonella = rulesOf("CLSI", "2026-2026.4").stream()
                .filter(rule -> "SAL".equals(rule.getOrganismCode()) && "MIC".equals(rule.getBreakpointType()))
                .findFirst().orElseThrow();
        assertEquals("GENUS_CODE", salmonella.getOrganismCodeType());
        assertNull(salmonella.getSusceptibleValue());
        assertNull(salmonella.getResistantValue());
    }

    @Test
    public void aRuleForAnAntibioticOpenElisDoesNotHaveIsLeftOut() throws Exception {
        load(fileText(), PACKAGE_FILE);

        // 13 human EUCAST 2026 rows, one of them for FIX, which Antibiotics.txt lacks.
        assertEquals(12, rulesOf("EUCAST", "2026-2026.4").size());
    }

    @Test
    public void aReadingIsInterpretedAgainstTheEdition() throws Exception {
        load(fileText(), PACKAGE_FILE);
        String klebsiella = organisms.findByWhonetCodeIgnoreCase("kpn").orElseThrow().getId();
        String amikacin = antibiotics.findByWhonetCodeIgnoreCase("AMK").orElseThrow().getId();

        MicroBreakpointRule mic = breakpoints.findWhonetBreakpointRule(edition("CLSI", "2026-2026.4").getId(),
                klebsiella, amikacin, "MIC", null, false);
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE, interpreter.interpret(mic, MicroAstMethod.MIC, value("4")));
        assertEquals(MicroAstInterpretation.INTERMEDIATE, interpreter.interpret(mic, MicroAstMethod.MIC, value("8")));
        assertEquals(MicroAstInterpretation.RESISTANT, interpreter.interpret(mic, MicroAstMethod.MIC, value("16")));

        MicroBreakpointRule zone = breakpoints.findWhonetBreakpointRule(edition("CLSI", "2026-2026.4").getId(),
                klebsiella, amikacin, "ZONE", "30 mcg", false);
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE, interpreter.interpret(zone, MicroAstMethod.ZONE, value("20")));
        assertEquals(MicroAstInterpretation.INTERMEDIATE,
                interpreter.interpret(zone, MicroAstMethod.ZONE, value("17")));
        assertEquals(MicroAstInterpretation.RESISTANT, interpreter.interpret(zone, MicroAstMethod.ZONE, value("16")));

        MicroBreakpointRule eucast = breakpoints.findWhonetBreakpointRule(edition("EUCAST", "2026-2026.4").getId(),
                klebsiella, amikacin, "MIC", null, false);
        assertNull(eucast.getSiteOfInfection());
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE,
                interpreter.interpret(eucast, MicroAstMethod.MIC, value("8"), "EUCAST"));
        assertEquals(MicroAstInterpretation.RESISTANT,
                interpreter.interpret(eucast, MicroAstMethod.MIC, value("16"), "EUCAST"));
    }

    @Test
    public void aUrineSpecimenTakesTheUrinaryBreakpointAndOthersNeverDo() throws Exception {
        load(fileText(), PACKAGE_FILE);
        String eucast = edition("EUCAST", "2026-2026.4").getId();
        String klebsiella = organisms.findByWhonetCodeIgnoreCase("kpn").orElseThrow().getId();
        String amoxicillinClavulanate = antibiotics.findByWhonetCodeIgnoreCase("AMC").orElseThrow().getId();

        MicroBreakpointRule systemic = breakpoints.findWhonetBreakpointRule(eucast, klebsiella, amoxicillinClavulanate,
                "MIC", null, false);
        assertEquals("Intravenous", systemic.getSiteOfInfection());
        assertEquals(0, new BigDecimal("8").compareTo(systemic.getSusceptibleValue()));

        MicroBreakpointRule urinary = breakpoints.findWhonetBreakpointRule(eucast, klebsiella, amoxicillinClavulanate,
                "MIC", null, true);
        assertEquals("Oral, Uncomplicated urinary tract infection", urinary.getSiteOfInfection());
        assertEquals(0, new BigDecimal("32").compareTo(urinary.getSusceptibleValue()));
    }

    @Test
    public void aValueBetweenDilutionsIsReadAtTheNextDilution() throws Exception {
        load(fileText(), PACKAGE_FILE);
        String klebsiella = organisms.findByWhonetCodeIgnoreCase("kpn").orElseThrow().getId();
        String amikacin = antibiotics.findByWhonetCodeIgnoreCase("AMK").orElseThrow().getId();
        MicroBreakpointRule eucast = breakpoints.findWhonetBreakpointRule(edition("EUCAST", "2026-2026.4").getId(),
                klebsiella, amikacin, "MIC", null, false);

        // EUCAST's "R > 8" is held as WHONET writes it, R at 16.
        assertEquals(MicroAstInterpretation.RESISTANT,
                interpreter.interpret(eucast, MicroAstMethod.MIC, value("12"), "EUCAST"));
    }

    @Test
    public void loadingTheSameTablesAgainAddsNothing() throws Exception {
        load(fileText(), PACKAGE_FILE);
        String clsi = edition("CLSI", "2026-2026.4").getId();
        int ruleCount = rules.getAll().size();

        load(fileText(), PACKAGE_FILE);
        load(fileText(), "pkg--amr-reference-eucast--2026.4--Breakpoints.txt");
        load(fileText(), "pkg--amr-reference-clsi--2026.5--Breakpoints.txt");

        assertEquals(6, standards.getAll().size());
        assertEquals(clsi, edition("CLSI", "2026-2026.4").getId());
        assertEquals(ruleCount, rules.getAll().size());
    }

    @Test
    public void aCorrectedYearInALaterPackageIsANewEditionAndTheOthersStay() throws Exception {
        load(fileText(), PACKAGE_FILE);

        load(corrected(), "pkg--amr-reference-clsi--2026.5--Breakpoints.txt");

        assertEquals(7, standards.getAll().size());
        assertEquals(0,
                new BigDecimal("2").compareTo(rule("CLSI", "2026-2026.5", "AMK", "MIC", null).getSusceptibleValue()));
        assertEquals(0,
                new BigDecimal("4").compareTo(rule("CLSI", "2026-2026.4", "AMK", "MIC", null).getSusceptibleValue()));
        assertEquals("N", edition("CLSI", "2026-2026.5").getIsActive());
    }

    @Test
    public void changedContentUnderALoadedVersionFailsAndChangesNothing() throws Exception {
        load(fileText(), PACKAGE_FILE);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> load(corrected(), PACKAGE_FILE));

        assertTrue(failure.getMessage(), failure.getMessage().contains("CLSI 2026-2026.4"));
        assertEquals(6, standards.getAll().size());
        assertEquals(0,
                new BigDecimal("4").compareTo(rule("CLSI", "2026-2026.4", "AMK", "MIC", null).getSusceptibleValue()));
    }

    // The CLSI 2026 amikacin MIC susceptible value changed from 4 to 2.
    private String corrected() throws Exception {
        String row = "CLSI\t2026\tMIC\t\tEBC\tFAMILY_CODE\tHuman\tHuman\t\tTable 2A-1\t\tAMK\tAMK_NM\t16\t8\t\t";
        String text = fileText();
        assertTrue(text.contains(row + "4\t"));
        return text.replace(row + "4\t", row + "2\t");
    }

    private static BigDecimal value(String text) {
        return new BigDecimal(text);
    }
}
