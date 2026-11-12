package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicroWhonetOrganismConfigurationHandler;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.beans.factory.annotation.Autowired;

public class MicroWhonetBreakpointMatchIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicroWhonetOrganismConfigurationHandler organismTable;
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

    private String standardId;
    private String antibioticId;

    @Before
    public void whonetOrganismsAndAnEdition() throws Exception {
        executeDataSetWithStateManagement("testdata/microbiology/whonet-breakpoint-match.xml");
        try (InputStream file = getClass().getClassLoader()
                .getResourceAsStream("testdata/microbiology/whonet-organisms.txt")) {
            organismTable.processConfiguration(file, "Organisms.txt");
        }
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setAuthority("CLSI");
        standard.setVersion("2026-test");
        standard.setLifecycleStatus("LOADED");
        standard.setIsActive("N");
        standardId = standards.insert(standard);
        antibioticId = antibiotics.findByWhonetCodeIgnoreCase("CIP").map(MicroAntibiotic::getId).orElseGet(() -> {
            MicroAntibiotic antibiotic = new MicroAntibiotic();
            antibiotic.setDisplayName("Ciprofloxacin");
            antibiotic.setWhonetCode("CIP");
            return antibiotics.insert(antibiotic);
        });
    }

    private String rule(String level, String code, String site, String susceptible) {
        MicroBreakpointRule rule = new MicroBreakpointRule();
        rule.setStandardId(standardId);
        rule.setAntibioticId(antibioticId);
        rule.setBreakpointType("MIC");
        rule.setOrganismCodeType(level);
        rule.setOrganismCode(code);
        rule.setSiteOfInfection(site);
        rule.setSusceptibleValue(new BigDecimal(susceptible));
        rule.setResistantValue(new BigDecimal("64"));
        rule.setIsActive("Y");
        return rules.insert(rule);
    }

    private MicroBreakpointRule match(String whonetCode) {
        return match(whonetCode, false);
    }

    private MicroBreakpointRule match(String whonetCode, boolean urineSpecimen) {
        MicroOrganism organism = organisms.findByWhonetCodeIgnoreCase(whonetCode).orElseThrow();
        return breakpoints.findWhonetBreakpointRule(standardId, organism.getId(), antibioticId, "MIC", null,
                urineSpecimen);
    }

    @Test
    public void theMostSpecificOrganismLevelWins() {
        rule("ALL", "Gen", null, "1");
        String family = rule("FAMILY_CODE", "EBC", null, "2");
        String genus = rule("GENUS_CODE", "KL-", null, "3");
        String species = rule("WHONET_ORG_CODE", "kpn", null, "4");
        String serovarGroup = rule("SEROVAR_GROUP", "ECO", null, "5");
        rule("WHONET_ORG_CODE", "eco", null, "6");

        assertEquals(species, match("kpn").getId());
        assertEquals(genus, match("kl-").getId());
        assertEquals(serovarGroup, match("eco").getId());
        assertEquals(serovarGroup, match("157").getId());
        MicroBreakpointRule retired = rules.get(serovarGroup).orElseThrow();
        retired.setIsActive("N");
        rules.update(retired);
        assertEquals(family, match("157").getId());
    }

    @Test
    public void aRuleForAllOrganismsAppliesWhenNothingCloserDoes() {
        String all = rule("ALL", "Gen", null, "1");
        rule("FAMILY_CODE", "EBC", null, "2");

        assertEquals(all, match("kma").getId());
    }

    @Test
    public void amongRulesOfOneLevelWhonetsSiteOrderDecides() {
        rule("FAMILY_CODE", "EBC", "Meningitis", "1");
        String nonMeningitis = rule("FAMILY_CODE", "EBC", "Non-meningitis", "2");
        assertEquals(nonMeningitis, match("kpn").getId());

        rule("GENUS_CODE", "KL-", "Uncomplicated urinary tract infection", "3");
        String general = rule("GENUS_CODE", "KL-", null, "4");
        assertEquals(general, match("kpn").getId());
    }

    @Test
    public void onlyAUrineSpecimenUsesAUrinaryRule() {
        String intravenous = rule("FAMILY_CODE", "EBC", "Intravenous", "8");
        String urinary = rule("FAMILY_CODE", "EBC", "Oral, Uncomplicated urinary tract infection", "32");
        rule("FAMILY_CODE", "EBC", "Oral, Infections originating from the urinary tract", "1");

        assertEquals(intravenous, match("kpn", false).getId());
        assertEquals(urinary, match("kpn", true).getId());
    }

    @Test
    public void anOrganismWithOnlyAUrinaryRuleHasNoBreakpointOutsideUrine() {
        rule("FAMILY_CODE", "EBC", null, "2");
        String urinary = rule("GENUS_CODE", "KL-", "Uncomplicated urinary tract infection", "3");

        assertNull(match("kpn", false));
        assertEquals(urinary, match("kpn", true).getId());
    }

    @Test
    public void aUrineSpecimenWithoutAUrinaryRuleUsesTheGeneralOne() {
        String general = rule("FAMILY_CODE", "EBC", null, "2");

        assertEquals(general, match("kpn", true).getId());
    }

    @Test
    public void noRuleMatchesAnOrganismOutsideEveryLevel() {
        rule("FAMILY_CODE", "EBC", null, "2");

        assertNull(match("kma"));
    }

    @Test
    public void anOrganismWithoutAWhonetCodeHasNoWhonetRule() {
        rule("ALL", "Gen", null, "1");

        assertNull(breakpoints.findWhonetBreakpointRule(standardId, "lab-uncoded", antibioticId, "MIC", null, false));
    }

    @Test
    public void theEarlierLookupNeverReturnsAWhonetRule() {
        rule("ALL", "Gen", null, "1");
        String organismId = organisms.findByWhonetCodeIgnoreCase("kpn").orElseThrow().getId();

        assertNull(breakpoints.findBreakpointRule(standardId, organismId, null, antibioticId, "MIC", null, "MIC"));
    }

    @Test
    public void aZoneRuleAppliesOnlyToItsDiskContent() {
        MicroBreakpointRule zone = rules.get(rule("FAMILY_CODE", "EBC", null, "21")).orElseThrow();
        zone.setBreakpointType("ZONE");
        zone.setDiskContent("5 mcg");
        rules.update(zone);
        String organismId = organisms.findByWhonetCodeIgnoreCase("kpn").orElseThrow().getId();

        assertEquals(zone.getId(), breakpoints
                .findWhonetBreakpointRule(standardId, organismId, antibioticId, "ZONE", "5 mcg", false).getId());
        assertNull(breakpoints.findWhonetBreakpointRule(standardId, organismId, antibioticId, "ZONE", "10 mcg", false));
        assertNull(breakpoints.findWhonetBreakpointRule(standardId, organismId, antibioticId, "ZONE", null, false));
    }

    @Test
    public void aDiskContentMatchesHoweverItIsSpelled() {
        MicroBreakpointRule zone = rules.get(rule("FAMILY_CODE", "EBC", null, "21")).orElseThrow();
        zone.setBreakpointType("ZONE");
        zone.setDiskContent("5 mcg");
        rules.update(zone);
        String organismId = organisms.findByWhonetCodeIgnoreCase("kpn").orElseThrow().getId();

        for (String spelling : new String[] { "5ug", "5 µg", "5mcg", "5 MCG" }) {
            assertEquals(spelling, zone.getId(), breakpoints
                    .findWhonetBreakpointRule(standardId, organismId, antibioticId, "ZONE", spelling, false).getId());
        }
        assertNull(breakpoints.findWhonetBreakpointRule(standardId, organismId, antibioticId, "ZONE", "50 mcg", false));
    }
}
