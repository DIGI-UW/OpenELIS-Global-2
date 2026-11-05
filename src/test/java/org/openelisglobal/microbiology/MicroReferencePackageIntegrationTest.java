package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroBreakpointRuleAdminForm;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;

public class MicroReferencePackageIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicroOrganismConfigurationHandler organismsHandler;
    @Autowired
    private MicroAntimicrobialConfigurationHandler antimicrobialsHandler;
    @Autowired
    private MicroBreakpointConfigurationHandler breakpointsHandler;
    @Autowired
    private MicroOrganismGroupConfigurationHandler groupsHandler;
    @Autowired
    private MicroAstPanelConfigurationHandler panelsHandler;
    @Autowired
    private org.openelisglobal.configuration.service.ConfigurationInitializationService initialization;
    private org.openelisglobal.configuration.service.CatalogImportService catalog;
    @Autowired
    private org.springframework.context.ApplicationContext context;
    @org.junit.Rule
    public org.junit.rules.TemporaryFolder configurationFiles = new org.junit.rules.TemporaryFolder();
    private Object oldDirectory;
    private Object oldCatalogDirectory;
    @Autowired
    private MicroOrganismDAO organisms;
    @Autowired
    private MicroAntibioticDAO antibiotics;
    @Autowired
    private MicroBreakpointStandardDAO standards;
    @Autowired
    private MicroBreakpointRuleDAO rules;
    @Autowired
    private MicroBreakpointAdminService administration;
    @Autowired
    private MicroAstInterpretationService interpreter;
    @Autowired
    private org.openelisglobal.microbiology.service.MicroReportingRuleService reportingRules;
    @Autowired
    private org.openelisglobal.microbiology.service.MicroInterpretationRuleService interpretationRules;
    @Autowired
    private MicroReportingRuleDAO reportingRuleDAO;
    @Autowired
    private org.openelisglobal.configuration.service.UnresolvedReferenceService unresolvedReferences;
    @Autowired
    private MicroAstPanelDAO panels;
    @Autowired
    private MicroAstPanelAntibioticDAO panelAntibiotics;

    @Autowired
    private MicroQcRangeService qcRanges;

    @Autowired
    private MicroBreakpointImportService importer;
    @Autowired
    private MicrobiologyConfigurationService configuration;
    @Autowired
    private MicrobiologyReferenceAdminService referenceAdmin;

    @Before
    public void seedAdoptableConcept() throws Exception {
        catalog = new org.openelisglobal.configuration.service.CatalogImportServiceImpl();
        context.getAutowireCapableBeanFactory().autowireBean(catalog);
        oldDirectory = org.springframework.test.util.ReflectionTestUtils.getField(initialization,
                "configurationBaseDir");
        oldCatalogDirectory = org.springframework.test.util.ReflectionTestUtils.getField(catalog,
                "configurationBaseDir");
        org.springframework.test.util.ReflectionTestUtils.setField(initialization, "configurationBaseDir",
                configurationFiles.getRoot().toString());
        org.springframework.test.util.ReflectionTestUtils.setField(catalog, "configurationBaseDir",
                configurationFiles.getRoot().toString());
        executeDataSetWithStateManagement("testdata/microbiology/reference-package.xml");
    }

    @org.junit.After
    public void restoreConfigurationDirectories() {
        org.springframework.test.util.ReflectionTestUtils.setField(initialization, "configurationBaseDir",
                oldDirectory);
        org.springframework.test.util.ReflectionTestUtils.setField(catalog, "configurationBaseDir",
                oldCatalogDirectory);
    }

    @Test
    public void indicationSpecificRulesRequireExplicitContextAndReloadIndependently() throws Exception {
        var fixture = new ReferencePackageFixture("2099.4", "CONTEXT");
        var lines = fixture.files.get("breakpoints.csv").lines().toList();
        String generic = lines.get(1).replace(",MIC,1,,,4,", ",MIC,,,,,");
        fixture.files.put("breakpoints.csv",
                lines.get(0) + ",clinical_context\n" + generic + ",\n" + lines.get(1) + ",NON_MENINGITIS\n");
        load(fixture);
        var standard = standard(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibiotic = antibiotics.findByReference("AMR", fixture.antibiotic).orElseThrow();
        var unspecified = rules.findBestRule(standard.getId(), organism.getId(), null, antibiotic.getId(), "MIC", null,
                "MIC");
        assertNull(unspecified.getClinicalContext());
        assertEquals(MicroAstInterpretation.NO_BREAKPOINT,
                interpreter.interpret(unspecified, MicroAstMethod.MIC, new BigDecimal("4")));
        var indicated = rules.findContextualRule(standard.getId(), organism.getId(), null, antibiotic.getId(), "MIC",
                null, "MIC", "NON_MENINGITIS", null);
        assertEquals("NON_MENINGITIS", indicated.getClinicalContext());
        assertEquals(MicroAstInterpretation.INTERMEDIATE,
                interpreter.interpret(indicated, MicroAstMethod.MIC, new BigDecimal("4")));
        assertNotEquals(unspecified.getId(), indicated.getId());
        load(fixture);
        assertEquals(2, rules.countByStandardId(standard.getId()));
        assertEquals(indicated.getId(), rules.findByContextualNaturalKey(standard.getId(), organism.getId(), null,
                antibiotic.getId(), "MIC", null, "MIC", "NON_MENINGITIS", null).orElseThrow().getId());
    }

    @Test
    public void zoneRuleMatchesOnlyTheDiskContentItWasPublishedFor() throws Exception {
        var fixture = new ReferencePackageFixture("2099.5", "DISK");
        var lines = fixture.files.get("breakpoints.csv").lines().toList();
        String zone = lines.get(1).replace(",MIC,,MIC,1,,,4,mg/L,", ",ZONE,,ZONE,20,,,17,mm,");
        fixture.files.put("breakpoints.csv",
                lines.get(0) + ",disk_content\n" + lines.get(1) + ",\n" + zone + ",30ug\n");
        load(fixture);
        var standard = standard(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibiotic = antibiotics.findByReference("AMR", fixture.antibiotic).orElseThrow();

        var matching = rules.findContextualRule(standard.getId(), organism.getId(), null, antibiotic.getId(), "ZONE",
                null, "ZONE", null, "30ug");
        assertEquals("30ug", matching.getDiskContent());
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE,
                interpreter.interpret(matching, MicroAstMethod.ZONE, new BigDecimal("22")));
        assertNull("Another disk must not borrow the 30ug breakpoint", rules.findContextualRule(standard.getId(),
                organism.getId(), null, antibiotic.getId(), "ZONE", null, "ZONE", null, "10ug"));
        assertNull("An unrecorded disk must not borrow the 30ug breakpoint",
                rules.findBestRule(standard.getId(), organism.getId(), null, antibiotic.getId(), "ZONE", null, "ZONE"));
        assertNull("MIC rules ignore disk content",
                rules.findBestRule(standard.getId(), organism.getId(), null, antibiotic.getId(), "MIC", null, "MIC")
                        .getDiskContent());
    }

    @Test
    public void twoDiskStrengthsInOneEditionLoadAsSeparateRules() throws Exception {
        var fixture = new ReferencePackageFixture("2099.51", "DISKS");
        var lines = fixture.files.get("breakpoints.csv").lines().toList();
        String zone = lines.get(1).replace(",MIC,,MIC,1,,,4,mg/L,", ",ZONE,,ZONE,20,,,17,mm,");
        fixture.files.put("breakpoints.csv", lines.get(0) + ",disk_content\n" + zone + ",30ug\n"
                + zone.replace(",20,,,17,", ",15,,,12,") + ",10ug\n");
        load(fixture);
        load(fixture);
        var standard = standard(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibiotic = antibiotics.findByReference("AMR", fixture.antibiotic).orElseThrow();

        assertEquals(2, rules.countByStandardId(standard.getId()));
        assertEquals(MicroAstInterpretation.RESISTANT,
                interpreter
                        .interpret(
                                rules.findContextualRule(standard.getId(), organism.getId(), null, antibiotic.getId(),
                                        "ZONE", null, "ZONE", null, "30ug"),
                                MicroAstMethod.ZONE, new BigDecimal("16")));
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE,
                interpreter
                        .interpret(
                                rules.findContextualRule(standard.getId(), organism.getId(), null, antibiotic.getId(),
                                        "ZONE", null, "ZONE", null, "10ug"),
                                MicroAstMethod.ZONE, new BigDecimal("16")));
    }

    @Test
    public void organismAndAntimicrobialFilesListEveryBadRowAndSaveNothing() throws Exception {
        var fixture = new ReferencePackageFixture("2099.56", "BADROWS");
        fixture.files.put("organisms.csv", "source,code,displayName,whonetCode,gramStain\nAMR," + fixture.organism
                + ",Synthetic organism BADROWS,,GRAM_NEGATIVE\nAMR,BADROWS_A,,,\nAMR,BADROWS_B,,,\n");
        fixture.files.put("antimicrobials.csv",
                "source,code,displayName,whonetCode,antibioticClass\nAMR," + fixture.antibiotic
                        + ",Synthetic agent BADROWS," + fixture.antibiotic
                        + ",Test class\nAMR,BADROWS_C,,BADROWS_C,\nAMR,BADROWS_D,,BADROWS_D,\n");
        writeFiles(fixture);

        var files = reload(true).files();

        for (String domain : java.util.List.of("microbiology-organisms", "microbiology-antimicrobials")) {
            String error = files.stream().filter(file -> domain.equals(file.domain())).findFirst().orElseThrow()
                    .errorMessage();
            assertTrue(domain + ": " + error, error != null && error.contains("Line 3:") && error.contains("Line 4:"));
        }
        assertTrue(organisms.findByReference("AMR", fixture.organism).isEmpty());
        assertTrue(antibiotics.findByReference("AMR", fixture.antibiotic).isEmpty());
    }

    @Test
    public void zoneReadingNeverMatchesARuleWithoutDiskContent() throws Exception {
        var fixture = new ReferencePackageFixture("2099.60", "NODISK");
        load(fixture);
        var standard = standard(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibiotic = antibiotics.findByReference("AMR", fixture.antibiotic).orElseThrow();
        var zone = new MicroBreakpointRule();
        zone.setStandardId(standard.getId());
        zone.setOrganismId(organism.getId());
        zone.setAntibioticId(antibiotic.getId());
        zone.setMethod("ZONE");
        zone.setBreakpointType("ZONE");
        zone.setSusceptibleValue(new BigDecimal("20"));
        zone.setResistantValue(new BigDecimal("17"));
        zone.setUnits("mm");
        zone.setIsActive("Y");
        zone.setLastUpdatedBy("1");
        rules.insert(zone);

        assertNull(rules.findContextualRule(standard.getId(), organism.getId(), null, antibiotic.getId(), "ZONE", null,
                "ZONE", null, "30ug"));
        assertNull(
                rules.findBestRule(standard.getId(), organism.getId(), null, antibiotic.getId(), "ZONE", null, "ZONE"));
        assertNotNull("MIC readings are unaffected",
                rules.findBestRule(standard.getId(), organism.getId(), null, antibiotic.getId(), "MIC", null, "MIC"));
    }

    @Test
    public void zoneRowWithoutDiskContentIsRejected() throws Exception {
        var fixture = new ReferencePackageFixture("2099.61", "ZONENODISK");
        var lines = fixture.files.get("breakpoints.csv").lines().toList();
        fixture.files.put("breakpoints.csv",
                lines.get(0) + "\n" + lines.get(1).replace(",MIC,,MIC,1,,,4,mg/L,", ",ZONE,,ZONE,20,,,17,mm,") + "\n");
        writeFiles(fixture);

        String error = reload(true).files().stream().filter(file -> "microbiology-breakpoints".equals(file.domain()))
                .findFirst().orElseThrow().errorMessage();

        assertTrue(error, error != null && error.contains("Disk content is required for zone breakpoints"));
    }

    @Test
    public void zoneCorrectionNeedsDiskContentAndMatchesOnlyThatDisk() throws Exception {
        var fixture = new ReferencePackageFixture("2099.62", "ZONEDISK");
        load(fixture);
        String local = fixture.edition + "-LOCAL";
        importer.apply(
                importer.preview(fixture.files.get("breakpoints.csv").replace(fixture.edition, local)).previewToken,
                "1");
        String standardId = standards.findByAuthorityAndVersion("EUCAST", local).orElseThrow().getId();
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibiotic = antibiotics.findByReference("AMR", fixture.antibiotic).orElseThrow();
        var form = new MicroBreakpointRuleAdminForm();
        form.organismId = organism.getId();
        form.antibioticId = antibiotic.getId();
        form.method = "DISK_DIFFUSION";
        form.breakpointType = "ZONE";
        form.susceptibleValue = new BigDecimal("20");
        form.resistantValue = new BigDecimal("17");
        form.units = "mm";

        assertEquals("Disk content is required for zone breakpoints",
                assertThrows(IllegalArgumentException.class, () -> administration.saveRule(standardId, null, form, "1"))
                        .getMessage());
        form.diskContent = "30ug";
        assertEquals("30ug", administration.saveRule(standardId, null, form, "1").diskContent);

        assertNotNull(rules.findContextualRule(standardId, organism.getId(), null, antibiotic.getId(), "DISK_DIFFUSION",
                null, "ZONE", null, "30ug"));
        assertNull(rules.findContextualRule(standardId, organism.getId(), null, antibiotic.getId(), "DISK_DIFFUSION",
                null, "ZONE", null, "10ug"));
    }

    @Test
    public void micRuleWithDiskContentIsRejected() throws Exception {
        var fixture = new ReferencePackageFixture("2099.7", "MICDISK");
        var lines = fixture.files.get("breakpoints.csv").lines().toList();
        fixture.files.put("breakpoints.csv", lines.get(0) + ",disk_content\n" + lines.get(1) + ",30ug\n");
        writeFiles(fixture);
        assertTrue(reload(true).hasErrors());
        assertTrue(standards.findByAuthorityAndVersion("EUCAST", fixture.edition).isEmpty());
    }

    @Test
    public void packageOrganismGroupFillsABlankButNeverReplacesTheLaboratorysGroup() throws Exception {
        var fixture = new ReferencePackageFixture("2099.6", "GROUP");
        String header = "source,code,displayName,whonetCode,gramStain,organism_group\n";
        String row = "AMR," + fixture.organism + ",Synthetic organism GROUP,,GRAM_NEGATIVE,";
        fixture.files.put("organisms.csv", header + row + "Enterobacterales\n");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        assertEquals("Enterobacterales", organism.getOrganismGroup());

        organism.setOrganismGroup("Laboratory group");
        organisms.update(organism);
        fixture.files.put("organisms.csv", header + row + "Pseudomonas\n");
        load(fixture);
        assertEquals("Laboratory group",
                organisms.findByReference("AMR", fixture.organism).orElseThrow().getOrganismGroup());
    }

    @Test
    public void reportingRuleFileLoadsRulesNotesAndAlwaysResistantAndReloadsWithoutChange() throws Exception {
        var fixture = new ReferencePackageFixture("2099.9", "RULES");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,,Synthetic rule group\n");
        assertFalse(reloadOrganismGroups(false).hasErrors());
        writeReportingRules(RULES_HEADER + "SPECIES," + organism.getDisplayName() + ",ANY," + fixture.antibiotic
                + ",Synthetic,DONT_REPORT,,,,YES,Consider meropenem,WHEN_R,,\n" + "GROUP,Synthetic rule group,ANY,"
                + fixture.antibiotic + ",Synthetic,CASCADE,ANY_R,CRO;GEN,,,,,,\n");
        assertFalse(reloadReportingRules(false).hasErrors());

        var loaded = rulesFor(antibioticId);
        assertEquals(2, loaded.size());
        var species = loaded.stream().filter(r -> organism.getId().equals(r.organismId)).findFirst().orElseThrow();
        assertEquals("DONT_REPORT", species.reportBehavior);
        assertEquals("Consider meropenem", species.note);
        assertEquals("WHEN_R", species.noteWhen);
        assertTrue(species.sampleTypeIds.isEmpty());
        var group = loaded.stream().filter(r -> "Synthetic rule group".equals(r.organismGroup)).findFirst()
                .orElseThrow();
        assertEquals("CASCADE", group.reportBehavior);
        assertEquals("ANY_R CRO;GEN", group.cascadeCondition);
        assertEquals(1,
                interpretationRules
                        .getRules().stream().filter(r -> antibioticId.equals(r.antibioticId)
                                && organism.getId().equals(r.organismId) && "RESISTANT".equals(r.interpretation))
                        .count());

        assertFalse(reloadReportingRules(true).hasErrors());
        assertEquals(2, rulesFor(antibioticId).size());
        assertEquals(1, reportingRules.getHistory(species.id).size());

        writeReportingRules(reportingRules.exportCsv());
        assertFalse(reloadReportingRules(true).hasErrors());
        assertEquals(2, rulesFor(antibioticId).size());
        assertEquals(1, reportingRules.getHistory(species.id).size());
        assertEquals(1, reportingRules.getHistory(group.id).size());

        group.addAutomatically = true;
        reportingRules.save(group, "1");
        writeReportingRules(reportingRules.exportCsv());
        assertFalse(reloadReportingRules(true).hasErrors());
        assertTrue(rulesFor(antibioticId).stream().filter(r -> group.id.equals(r.id)).findFirst()
                .orElseThrow().addAutomatically);
        assertEquals(2, reportingRules.getHistory(group.id).size());
    }

    @Test
    public void reportingRuleFileIsAppliedWholeOrNotAtAll() throws Exception {
        var fixture = new ReferencePackageFixture("2099.11", "ATOMIC");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        writeReportingRules(RULES_HEADER + "SPECIES," + organism.getDisplayName() + ",ANY," + fixture.antibiotic
                + ",Synthetic,DONT_REPORT,,,,,,,,\n" + "SPECIES," + organism.getDisplayName()
                + ",ANY,NO_SUCH_AGENT,Missing,REPORT,,,,,,,,\n");
        assertTrue(reloadReportingRules(false).hasErrors());
        assertTrue(rulesFor(antibioticId).isEmpty());
    }

    @Test
    public void exportKeepsInactiveRulesAndCustomSourcesThroughAReimport() throws Exception {
        var fixture = new ReferencePackageFixture("2099.12", "ROUNDTRIP");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        writeReportingRules(RULES_HEADER + "SPECIES," + organism.getDisplayName() + ",ANY," + fixture.antibiotic
                + ",Synthetic,REPORT,,,,YES,,,,\n");
        assertFalse(reloadReportingRules(false).hasErrors());
        var rule = rulesFor(antibioticId).get(0);
        rule.active = false;
        reportingRules.save(rule, "1");
        var resistant = interpretationRules.getRules().stream().filter(r -> antibioticId.equals(r.antibioticId))
                .findFirst().orElseThrow();
        resistant.source = "EUCAST expert rules";
        interpretationRules.save(resistant, "1");

        writeReportingRules(reportingRules.exportCsv());
        assertFalse(reloadReportingRules(true).hasErrors());
        assertFalse(rulesFor(antibioticId).get(0).active);
        assertEquals(2, reportingRules.getHistory(rule.id).size());
        assertEquals("EUCAST expert rules", interpretationRules.getRules().stream()
                .filter(r -> resistant.id.equals(r.id)).findFirst().orElseThrow().source);
    }

    @Test
    public void isolateLevelNoteRowsAreSkippedNotFatal() throws Exception {
        var fixture = new ReferencePackageFixture("2099.13", "ISOLATE");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        writeReportingRules(RULES_HEADER + "SPECIES," + organism.getDisplayName()
                + ",No such sample type,(isolate),Isolate note,,,,,,Therapy is not usually indicated,ALWAYS,,\n"
                + "SPECIES," + organism.getDisplayName() + ",ANY," + fixture.antibiotic
                + ",Synthetic,REPORT,,,,,,,,\n");
        assertFalse(reloadReportingRules(false).hasErrors());
        assertEquals(1, rulesFor(antibioticId).size());
    }

    @Test
    public void reportingRuleFileWithAnUnknownSampleTypeFails() throws Exception {
        var fixture = new ReferencePackageFixture("2099.10", "BADRULE");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        writeReportingRules(RULES_HEADER + "SPECIES," + organism.getDisplayName() + ",No such sample type,"
                + fixture.antibiotic + ",Synthetic,REPORT,,,,,,,,\n");
        assertTrue(reloadReportingRules(false).hasErrors());
        assertTrue(rulesFor(antibioticId).isEmpty());
    }

    @Test
    public void reportingRuleFileListsEveryUnknownNameWithItsLineAndSavesNothing() throws Exception {
        var fixture = new ReferencePackageFixture("2099.20", "UNKNOWNS");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        String sampleType = "CSF " + java.util.UUID.randomUUID().toString().substring(0, 8);
        String csv = RULES_HEADER + "SPECIES," + organism.getDisplayName() + ",ANY," + fixture.antibiotic
                + ",Synthetic,DONT_REPORT,,,,,,,,\n" + "SPECIES," + organism.getDisplayName() + "," + sampleType + ","
                + fixture.antibiotic + ",Synthetic,REPORT,,,,,,,,\n" + "SPECIES," + organism.getDisplayName()
                + ",ANY,NO_SUCH_AGENT,Missing,REPORT,,,,,,,,\n" + "SPECIES,No such organism,ANY," + fixture.antibiotic
                + ",Synthetic,REPORT,,,,,,,,\n";
        writeReportingRules(csv);
        var file = new org.springframework.mock.web.MockMultipartFile("files", "reporting-rules.csv", "text/csv",
                csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String previewed = catalog
                .preview(java.util.List.of(file), java.util.List.of("microbiology-reporting-rules"), "1").files().get(0)
                .error();
        String reloaded = reloadReportingRules(false).files().get(0).errorMessage();

        for (String error : java.util.List.of(previewed, reloaded)) {
            assertTrue(error, error.contains("Line 3: unknown sample type " + sampleType));
            assertTrue(error, error.contains("Line 4: unknown antibiotic NO_SUCH_AGENT"));
            assertTrue(error, error.contains("Line 5: unknown organism No such organism"));
            assertFalse(error, error.contains("Line 2:"));
        }
        assertTrue(rulesFor(antibioticId).isEmpty());
        assertEquals(Integer.valueOf(3), unresolvedReferences.getOpen().stream()
                .filter(r -> sampleType.equals(r.getReferenceValue())).findFirst().orElseThrow().getLineNumber());
    }

    @Test
    public void panelFileListsEveryUnknownAntibioticWithItsLineAndSavesNothing() throws Exception {
        var fixture = new ReferencePackageFixture("2099.21", "PANELUNK");
        load(fixture);
        writePanels("panelName,antibioticWhonetCode\nSynthetic good panel," + fixture.antibiotic
                + "\nSynthetic unknown panel,NO_SUCH_AGENT_A\nSynthetic unknown panel," + fixture.antibiotic
                + "\nSynthetic unknown panel,NO_SUCH_AGENT_B\nSecond unknown panel,NO_SUCH_AGENT_C\n");

        String error = reloadPanels(false).files().get(0).errorMessage();

        assertTrue(error, error.contains("Line 3: unknown antibiotic NO_SUCH_AGENT_A"));
        assertTrue(error, error.contains("Line 5: unknown antibiotic NO_SUCH_AGENT_B"));
        assertTrue(error, error.contains("Line 6: unknown antibiotic NO_SUCH_AGENT_C"));
        assertNull(panels.findCurrentByNameIgnoreCase("Synthetic good panel"));
        assertNull(panels.findCurrentByNameIgnoreCase("Synthetic unknown panel"));
    }

    @Test
    public void panelFileCreatesVersionsAndAssignsTheMostSpecificDefault() throws Exception {
        var fixture = new ReferencePackageFixture("2099.8", "PANEL");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        String header = "panelName,antibioticWhonetCode,diskContent,defaultFor\n";
        writePanels(header + "Synthetic species panel," + fixture.antibiotic + ",30ug,Synthetic organism PANEL\n"
                + "Synthetic genus panel," + fixture.antibiotic + ",,Synthetic\n");
        assertFalse(reloadPanels(false).hasErrors());
        var species = panels.findCurrentByNameIgnoreCase("Synthetic species panel");
        assertEquals(1, species.getVersionNumber().intValue());
        assertEquals("30ug", panelAntibiotics.getByPanelId(species.getId()).get(0).getDiskContent());
        assertEquals("The species claim beats the genus claim", species.getId(),
                organisms.get(organism.getId()).orElseThrow().getDefaultAstPanelId());

        assertFalse(reloadPanels(true).hasErrors());
        assertEquals("An unchanged panel publishes nothing", species.getId(),
                panels.findCurrentByNameIgnoreCase("Synthetic species panel").getId());

        writePanels(header + "Synthetic species panel," + fixture.antibiotic + ",10ug,Synthetic organism PANEL\n"
                + "Synthetic genus panel," + fixture.antibiotic + ",,Synthetic\n");
        assertFalse(reloadPanels(false).hasErrors());
        var republished = panels.findCurrentByNameIgnoreCase("Synthetic species panel");
        assertEquals(2, republished.getVersionNumber().intValue());
        assertEquals("10ug", panelAntibiotics.getByPanelId(republished.getId()).get(0).getDiskContent());
        assertEquals("The default follows the current version", republished.getId(),
                organisms.get(organism.getId()).orElseThrow().getDefaultAstPanelId());
    }

    @Test
    public void mostSpecificDefaultWinsAcrossPanelFilesWhateverTheirOrder() throws Exception {
        var fixture = new ReferencePackageFixture("2099.52", "ACROSS");
        load(fixture);
        String genusName = "Across" + suffix();
        var organism = organism(genusName + " species");
        String header = "panelName,antibioticWhonetCode,defaultFor\n";
        String species = header + "Synthetic species panel ACROSS," + fixture.antibiotic + ","
                + organism.getDisplayName() + "\n";
        String genus = header + "Synthetic genus panel ACROSS," + fixture.antibiotic + "," + genusName + "\n";
        for (var files : java.util.List.of(java.util.List.of(species, genus), java.util.List.of(genus, species))) {
            writePanels("a-panels.csv", files.get(0));
            writePanels("b-panels.csv", files.get(1));
            var result = reloadPanels(true);
            assertFalse(result.toString(), result.hasErrors());
            assertEquals(panels.findCurrentByNameIgnoreCase("Synthetic species panel ACROSS").getId(),
                    organisms.get(organism.getId()).orElseThrow().getDefaultAstPanelId());
        }
    }

    @Test
    public void panelsThatClaimAnOrganismEquallySpecificallyFailTheLoad() throws Exception {
        var fixture = new ReferencePackageFixture("2099.58", "TIE");
        load(fixture);
        var organism = organism("Tie organism " + suffix());
        String first = "Tie group A " + suffix();
        String second = "Tie group B " + suffix();
        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,," + first + "\n" + organism.getDisplayName()
                + ",,," + second + "\n");
        assertFalse(reloadOrganismGroups(false).hasErrors());
        writePanels("panelName,antibioticWhonetCode,defaultFor\nSynthetic first panel TIE," + fixture.antibiotic + ","
                + first + "\nSynthetic second panel TIE," + fixture.antibiotic + "," + second + "\n");

        String error = reloadPanels(true).files().get(0).errorMessage();

        assertTrue(error, error.contains("Lines 2 and 3:"));
        assertTrue(error, error.contains("Synthetic first panel TIE") && error.contains("Synthetic second panel TIE"));
        assertTrue(error, error.contains(first) && error.contains(second) && error.contains(organism.getDisplayName()));
        assertNull(panels.findCurrentByNameIgnoreCase("Synthetic first panel TIE"));
    }

    @Test
    public void panelFileRefusesADefaultWhoseProtocolDiffersFromTheOrganisms() throws Exception {
        var fixture = new ReferencePackageFixture("2099.53", "PROTOCOL");
        load(fixture);
        var organism = organism("Protocol organism " + suffix());
        organism.setSusceptibilityProtocol("TB");
        organisms.update(organism);
        writePanels("panelName,antibioticWhonetCode,defaultFor\nSynthetic general panel PROTOCOL," + fixture.antibiotic
                + "," + organism.getDisplayName() + "\n");

        String error = reloadPanels(true).files().get(0).errorMessage();

        assertTrue(error, error.contains("Line 2:"));
        assertTrue(error, error.contains(organism.getDisplayName()) && error.contains("TB"));
        assertNull(organisms.get(organism.getId()).orElseThrow().getDefaultAstPanelId());
    }

    @Test
    public void packageUpdateLeavesALocallyEditedPanelAlone() throws Exception {
        var fixture = new ReferencePackageFixture("2099.63", "LOCALEDIT");
        load(fixture);
        String header = "panelName,antibioticWhonetCode,diskContent\n";
        writePanels(header + "Synthetic edited panel," + fixture.antibiotic + ",30ug\n" + "Synthetic untouched panel,"
                + fixture.antibiotic + ",30ug\n");
        assertFalse(reloadPanels(false).hasErrors());
        var edited = panels.findCurrentByNameIgnoreCase("Synthetic edited panel");
        var form = referenceAdmin.getAstPanel(edited.getId());
        form.antibiotics.get(0).diskContent = "10ug";
        String local = referenceAdmin.publishPanelVersion(edited.getId(), form, "1").id;
        var legacy = panels.findCurrentByNameIgnoreCase("Synthetic untouched panel");
        jdbcTemplate.update("UPDATE clinlims.micro_ast_panel SET configuration_content = NULL WHERE id = ?",
                legacy.getId());

        writePanels(header + "Synthetic edited panel," + fixture.antibiotic + ",5ug\n" + "Synthetic untouched panel,"
                + fixture.antibiotic + ",5ug\n");
        assertFalse(reloadPanels(false).hasErrors());

        assertEquals(local, panels.findCurrentByNameIgnoreCase("Synthetic edited panel").getId());
        assertEquals("10ug", panelAntibiotics.getByPanelId(local).get(0).getDiskContent());
        assertTrue(panelsHandler.getLastSummary().getRows().stream()
                .anyMatch(row -> "Panel edited locally; package version not applied".equals(row.reason())));
        var updated = panels.findCurrentByNameIgnoreCase("Synthetic untouched panel");
        assertNotEquals("A panel loaded before this rule still takes the package version", legacy.getId(),
                updated.getId());
        assertEquals("5ug", panelAntibiotics.getByPanelId(updated.getId()).get(0).getDiskContent());
    }

    @Test
    public void panelFileWithTierAndReportBehaviorColumnsStillLoads() throws Exception {
        var fixture = new ReferencePackageFixture("2099.19", "PANELCOL");
        load(fixture);
        writePanels("panelName,antibioticWhonetCode,tier,reportBehavior\nSynthetic column panel," + fixture.antibiotic
                + ",4,CASCADE\n");
        var result = reloadPanels(false);
        assertFalse(result.toString(), result.hasErrors());
        var panel = panels.findCurrentByNameIgnoreCase("Synthetic column panel");
        assertEquals(1, panelAntibiotics.getByPanelId(panel.getId()).size());
    }

    @Test
    public void freshLoadAndReloadKeepIdentifiersLocalEditsAndInactiveEdition() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "RELOAD");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibiotic = antibiotics.findByReference("AMR", fixture.antibiotic).orElseThrow();
        var standard = standard(fixture);
        var rule = rules.getByStandardId(standard.getId()).get(0);
        assertEquals("N", standard.getIsActive());
        assertEquals("LOADED", standard.getLifecycleStatus());
        assertFalse(rule.isResistantInclusive());
        assertEquals(MicroAstInterpretation.INTERMEDIATE,
                interpreter.interpret(rule, MicroAstMethod.MIC, new BigDecimal("4")));
        organism.setDisplayName("Local organism name");
        organism.setIsActive("N");
        organism.setSysUserId("1");
        organisms.update(organism);
        antibiotic.setDisplayName("Local antimicrobial name");
        antibiotic.setSysUserId("1");
        antibiotics.update(antibiotic);
        load(fixture);
        var reloaded = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        assertEquals(organism.getId(), reloaded.getId());
        assertEquals("Local organism name", reloaded.getDisplayName());
        assertEquals("N", reloaded.getIsActive());
        assertEquals("Local antimicrobial name",
                antibiotics.findByReference("AMR", fixture.antibiotic).orElseThrow().getDisplayName());
        assertEquals(standard.getId(), standard(fixture).getId());
        assertEquals(1, rules.countByStandardId(standard.getId()));
        assertEquals(rule.getId(), rules.getByStandardId(standard.getId()).get(0).getId());
    }

    @Test
    public void adoptsExistingConceptWithoutChangingItsClinicalSettings() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "ADOPT");
        load(fixture);
        var adopted = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        assertEquals("reference-package-adoption", adopted.getId());
        assertEquals("GRAM_POSITIVE", adopted.getGramStain());
        assertEquals("N", adopted.getIsActive());
        assertFalse(adopted.isSeeded());
    }

    @Test
    public void newPackageCreatesInactiveEditionAndPreservesPreviousRulesAndActivation() throws Exception {
        var first = new ReferencePackageFixture("2099.1", "UPGRADE");
        load(first);
        var old = standard(first);
        administration.activate(old.getId(), java.sql.Date.valueOf("2026-01-01"), "1");
        var oldRule = rules.getByStandardId(old.getId()).get(0);
        var next = new ReferencePackageFixture("2099.2", "UPGRADE");
        load(next);
        assertNotEquals(old.getId(), standard(next).getId());
        assertEquals("N", standard(next).getIsActive());
        assertEquals("LOADED", standard(next).getLifecycleStatus());
        assertEquals("Y", standard(first).getIsActive());
        assertEquals(oldRule.getId(), rules.getByStandardId(old.getId()).get(0).getId());
        assertEquals(1, rules.countByStandardId(old.getId()));
        assertEquals(1, rules.countByStandardId(standard(next).getId()));
    }

    @Test
    public void anyOrganismCorrectionCanBeCreatedAndEdited() throws Exception {
        var fixture = new ReferencePackageFixture("2099.57", "ANYORG");
        load(fixture);
        String local = fixture.edition + "-LOCAL";
        var applied = importer.apply(
                importer.preview(fixture.files.get("breakpoints.csv").replace(fixture.edition, local)).previewToken,
                "1");
        assertTrue(applied.errors.toString(), applied.errors.isEmpty());
        String standardId = standards.findByAuthorityAndVersion("EUCAST", local).orElseThrow().getId();
        var form = new MicroBreakpointRuleAdminForm();
        form.antibioticId = antibiotics.findByReference("AMR", fixture.antibiotic).orElseThrow().getId();
        form.method = "MIC";
        form.breakpointType = "MIC";
        form.susceptibleValue = new BigDecimal("2");
        form.resistantValue = new BigDecimal("8");
        form.units = "mg/L";

        var created = administration.saveRule(standardId, null, form, "1");
        form.susceptibleValue = new BigDecimal("1");
        var edited = administration.saveRule(standardId, created.id, form, "1");

        assertNull(edited.organismId);
        assertNull(edited.organismGroup);
        assertEquals(0, new BigDecimal("1").compareTo(edited.susceptibleValue));
    }

    @Test
    public void activatingAnActiveEditionAgainRecordsNoSecondEvent() throws Exception {
        var fixture = new ReferencePackageFixture("2099.66", "REACTIVATE");
        load(fixture);
        String standardId = standard(fixture).getId();

        administration.activate(standardId, java.sql.Date.valueOf("2099-01-01"), "1");
        administration.activate(standardId, java.sql.Date.valueOf("2099-01-01"), "1");

        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT count(*)::int FROM clinlims.micro_breakpoint_activation_event WHERE standard_id = ? AND action = 'ACTIVATED'",
                Integer.class, standardId));
    }

    @Test
    public void managedEditionCannotBeEditedThroughAdministration() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "EDIT");
        load(fixture);
        String standardId = standard(fixture).getId();
        var rule = rules.getByStandardId(standardId).get(0);
        assertTrue(administration.getStandard(standardId).configurationManaged);
        assertThrows(MicroReferenceConflictException.class,
                () -> administration.saveRule(standardId, rule.getId(), new MicroBreakpointRuleAdminForm(), "1"));
    }

    @Test
    public void otherWritersCannotReplaceRulesOrActivateManagedEditions() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "WRITERS");
        load(fixture);
        var preview = importer.preview(fixture.files.get("breakpoints.csv"));
        assertThrows(IllegalArgumentException.class, () -> importer.apply(preview.previewToken, "1"));
        assertThrows(MicroReferenceConflictException.class, () -> configuration.getOrCreateBreakpointStandard("EUCAST",
                fixture.edition, java.sql.Date.valueOf("2026-01-01")));
        var rule = rules.getByStandardId(standard(fixture).getId()).get(0);
        assertThrows(MicroReferenceConflictException.class, () -> configuration.getOrCreateBreakpointRule(rule));
        assertThrows(MicroReferenceConflictException.class, () -> configuration.createBreakpointRule(rule));
        assertEquals("N", standard(fixture).getIsActive());
    }

    @Test
    public void failedEditionIsReportedAndRetryableWithoutPartialRules() throws Exception {
        var fixture = new ReferencePackageFixture("2099.3", "ROLLBACK");
        String valid = fixture.files.get("breakpoints.csv");
        fixture.files.put("breakpoints.csv",
                valid + valid.lines().skip(1).findFirst().orElseThrow().replace(",1,,,4,", ",1,,,0.00001,") + "\n");
        writeFiles(fixture);
        var failure = reload(false);
        assertTrue(failure.hasErrors());
        assertTrue(organisms.findByReference("AMR", fixture.organism).isPresent());
        assertFalse(standards.findByAuthorityAndVersion("EUCAST", fixture.edition).isPresent());
        fixture.files.put("breakpoints.csv", valid);
        writeFiles(fixture);
        assertFalse(reload(false).hasErrors());
        assertEquals(1, rules.countByStandardId(standard(fixture).getId()));
    }

    @Test
    public void invalidConceptFileRollsBackAndDuplicateIdentitiesAreRejected() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "INVALID_CONCEPT");
        String valid = fixture.files.get("organisms.csv");
        String second = "AMR,SECOND_INVALID_CONCEPT," + "x".repeat(256) + ",,\n";
        fixture.files.put("organisms.csv", valid + second);
        writeFiles(fixture);
        var failure = reload(false);
        assertTrue(failure.hasErrors());
        assertFalse(organisms.findByReference("AMR", fixture.organism).isPresent());
        fixture.files.put("organisms.csv", valid + valid.lines().skip(1).findFirst().orElseThrow() + "\n");
        writeFiles(fixture);
        var duplicate = reload(false);
        assertTrue(duplicate.files().stream().anyMatch(
                file -> file.errorMessage() != null && file.errorMessage().contains("Duplicate source identity")));
        assertFalse(organisms.findByReference("AMR", fixture.organism).isPresent());
    }

    @Test
    public void catalogPreviewRollsBackAndApplyReportsRows() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "PREVIEW");
        var file = new org.springframework.mock.web.MockMultipartFile("files", "organisms.csv", "text/csv",
                fixture.files.get("organisms.csv").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(catalog.getImportableDomains().containsAll(java.util.List.of("microbiology-organisms",
                "microbiology-antimicrobials", "microbiology-breakpoints")));
        var preview = catalog.preview(java.util.List.of(file), java.util.List.of("microbiology-organisms"), "1");
        assertEquals(1, preview.files().get(0).created());
        assertFalse(organisms.findByReference("AMR", fixture.organism).isPresent());
        var applied = catalog.apply(java.util.List.of(file), java.util.List.of("microbiology-organisms"), "1");
        assertEquals(1, applied.files().get(0).created());
        assertTrue(organisms.findByReference("AMR", fixture.organism).isPresent());
    }

    @Test
    public void breakpointPreviewRollsBackAndCombinedApplyLoadsDependenciesFirst() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "DEPENDENCIES");
        java.util.List<org.springframework.web.multipart.MultipartFile> files = new java.util.ArrayList<>();
        for (String name : java.util.List.of("breakpoints.csv", "antimicrobials.csv", "organisms.csv")) {
            files.add(new org.springframework.mock.web.MockMultipartFile("files", name, "text/csv",
                    fixture.files.get(name).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        var domains = java.util.List.of("microbiology-breakpoints", "microbiology-antimicrobials",
                "microbiology-organisms");
        var concepts = catalog.apply(files.subList(1, 3), domains.subList(1, 3), "1");
        assertTrue(concepts.toString(), concepts.files().stream().allMatch(file -> file.error() == null));
        var preview = catalog.preview(files.subList(0, 1), domains.subList(0, 1), "1");
        assertEquals(1, preview.files().get(0).created());
        assertFalse(standards.findByAuthorityAndVersion("EUCAST", fixture.edition).isPresent());
        var apply = catalog.apply(files, domains, "1");
        assertTrue(apply.toString(), apply.files().stream().allMatch(file -> file.error() == null));
        assertEquals(
                java.util.List.of("microbiology-organisms", "microbiology-antimicrobials", "microbiology-breakpoints"),
                apply.files().stream()
                        .map(org.openelisglobal.configuration.service.CatalogImportService.FilePlan::domain).toList());
        assertEquals(1, rules.countByStandardId(standard(fixture).getId()));
    }

    @Test
    public void unchangedFilesAreSkippedByNormalReload() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "CHECKSUM");
        load(fixture);
        var result = reload(false);
        assertEquals(3, result.files().size());
        assertTrue(result.files().stream().allMatch(file -> file
                .status() == org.openelisglobal.configuration.service.ConfigurationReloadFileResult.Status.SKIPPED));
    }

    @Test
    public void changedBytesCannotReplaceAnExistingEdition() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "IMMUTABLE");
        load(fixture);
        fixture.files.compute("breakpoints.csv", (key, value) -> value.replace(",1,,,4,", ",1,,,5,"));
        writeFiles(fixture);
        var failure = reload(true);
        assertTrue(failure.hasErrors());
        assertTrue(failure.files().stream()
                .anyMatch(file -> file.errorMessage() != null && file.errorMessage().contains("different content")));
    }

    @Test
    public void addingEditionToConfigurationPreservesExistingEditionAndRejectsItsMutation() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "MULTI_EDITION");
        load(fixture);
        var first = standard(fixture);
        var originalRule = rules.getByStandardId(first.getId()).get(0);
        String csv = fixture.files.get("breakpoints.csv");
        String next = csv.lines().skip(1).findFirst().orElseThrow().replace(fixture.edition, fixture.edition + "-next");
        fixture.files.put("breakpoints.csv", csv.lines().findFirst().orElseThrow() + "\n" + next + "\n"
                + csv.lines().skip(1).findFirst().orElseThrow() + "\n");
        writeFiles(fixture);
        assertFalse(reload(false).hasErrors());
        assertEquals(first.getConfigurationHash(), standard(fixture).getConfigurationHash());
        assertEquals(originalRule.getId(), rules.getByStandardId(first.getId()).get(0).getId());
        var added = standards.findByAuthorityAndVersion("EUCAST", fixture.edition + "-next").orElseThrow();
        assertEquals("N", added.getIsActive());
        assertNotEquals(first.getConfigurationHash(), added.getConfigurationHash());
        fixture.files.compute("breakpoints.csv",
                (key, content) -> content.replace("Synthetic evidence", "Changed note"));
        writeFiles(fixture);
        assertTrue(reload(false).hasErrors());
        assertEquals("Synthetic evidence", rules.getByStandardId(first.getId()).get(0).getNotes());
    }

    @Test
    public void administratorImportAppliesNoteOnlyCorrectionsWithoutReplacingRuleId() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "NOTE_UPDATE");
        load(fixture);
        String edition = fixture.edition + "-manual";
        String csv = fixture.files.get("breakpoints.csv").replace(fixture.edition, edition);
        var preview = importer.preview(csv);
        assertEquals(1, importer.apply(preview.previewToken, "1").importedRows);
        String standardId = standards.findByAuthorityAndVersion("EUCAST", edition).orElseThrow().getId();
        var before = rules.getByStandardId(standardId).get(0);
        var corrected = importer.preview(csv.replace("Synthetic evidence", "Corrected evidence"));
        assertEquals(1, importer.apply(corrected.previewToken, "1").importedRows);
        var after = rules.getByStandardId(standardId).get(0);
        assertEquals(before.getId(), after.getId());
        assertEquals("Corrected evidence", after.getNotes());
        assertNotEquals(before.getSourceRowHash(), after.getSourceRowHash());
    }

    @Test
    public void explicitNoBreakpointOverridesNumericGroupFallback() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "BLOCK_FALLBACK");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        organism.setOrganismGroup("Synthetic blocking group");
        organism.setSysUserId("1");
        organisms.update(organism);
        String edition = fixture.edition + "-blocking";
        String csv = "publisher,version,organism_or_group,antibiotic_whonet_code,method,specimen_type_id,"
                + "breakpoint_type,susceptible_value,intermediate_lower_value,intermediate_upper_value,resistant_value,units\n"
                + "EUCAST," + edition + ",group:Synthetic blocking group," + fixture.antibiotic
                + ",MIC,,MIC,1,,,4,mg/L\n" + "EUCAST," + edition + "," + organism.getDisplayName() + ","
                + fixture.antibiotic + ",MIC,,MIC,,,,,mg/L\n";
        assertEquals(2, importer.applyConfiguration(csv, "1").importedRows);
        var standard = standards.findByAuthorityAndVersion("EUCAST", edition).orElseThrow();
        var antibiotic = antibiotics.findByReference("AMR", fixture.antibiotic).orElseThrow();
        var selected = rules.findBestRule(standard.getId(), organism.getId(), organism.getOrganismGroup(),
                antibiotic.getId(), "MIC", null, "MIC");
        assertEquals(organism.getId(), selected.getOrganismId());
        assertEquals(MicroAstInterpretation.NO_BREAKPOINT,
                interpreter.interpret(selected, MicroAstMethod.MIC, BigDecimal.ONE));
    }

    @Test
    public void realPublishedCandidatePassesReceivingApplicationAndSourceVectors() throws Exception {
        String path = System.getProperty("amr.reference.artifact");
        org.junit.Assume.assumeTrue("Supply -Damr.reference.artifact for the generated-package contract test",
                path != null);
        // Archives are inspected only by this compatibility test, never by application
        // startup.
        try (var zip = new java.util.zip.ZipFile(path)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().startsWith("configuration/backend/")) {
                    Path target = configurationFiles.getRoot().toPath()
                            .resolve(entry.getName().substring("configuration/backend/".length()));
                    Files.createDirectories(target.getParent());
                    try (var in = zip.getInputStream(entry)) {
                        Files.copy(in, target);
                    }
                }
            }
        }
        assertFalse(reload(false).hasErrors());
        var organism = organisms.findByReference("AMR", "B_ESCHR_COLI").orElseThrow();
        assertEquals("Escherichia coli", organism.getDisplayName());
        com.fasterxml.jackson.databind.JsonNode manifest;
        try (var zip = new java.util.zip.ZipFile(path)) {
            manifest = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(zip.getInputStream(zip.getEntry("manifest.json")));
        }
        var editions = new java.util.HashMap<String, MicroBreakpointStandard>();
        for (var edition : manifest.get("standards")) {
            String authority = edition.get("publisher").asText();
            String version = edition.get("version").asText();
            var standard = standards.findByAuthorityAndVersion(authority, version).orElseThrow();
            editions.put(authority + "|" + version, standard);
            assertEquals("LOADED", standard.getLifecycleStatus());
            assertEquals("N", standard.getIsActive());
            assertTrue(rules.countByStandardId(standard.getId()) > 0);
        }
        var loadedOrganisms = new java.util.HashMap<String, MicroOrganism>();
        var loadedAgents = new java.util.HashMap<String, MicroAntibiotic>();
        var selectedRules = new java.util.HashMap<String, MicroBreakpointRule>();
        // A zone vector is interpreted with the disk content its rule was published
        // for.
        var diskContents = new java.util.HashMap<String, String>();
        try (var zip = new java.util.zip.ZipFile(path);
                var input = new java.io.InputStreamReader(
                        zip.getInputStream(
                                zip.getEntry("configuration/backend/microbiology-breakpoints/breakpoints.csv")),
                        java.nio.charset.StandardCharsets.UTF_8);
                var published = org.apache.commons.csv.CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                        .build().parse(input)) {
            for (var row : published) {
                if (row.isMapped("disk_content") && !row.get("disk_content").isBlank()) {
                    diskContents.put(String.join("|", row.get("version"), row.get("organism_code"),
                            row.get("antibiotic_code"), row.get("method"), row.get("clinical_context")),
                            row.get("disk_content"));
                }
            }
        }
        int compared = 0;
        try (var zip = new java.util.zip.ZipFile(path);
                var input = new java.io.InputStreamReader(
                        zip.getInputStream(zip.getEntry("evidence/interpretation-vectors.csv")),
                        java.nio.charset.StandardCharsets.UTF_8);
                var vectors = org.apache.commons.csv.CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                        .build().parse(input)) {
            for (var vector : vectors) {
                var org = loadedOrganisms.computeIfAbsent(vector.get("organism_code"),
                        code -> organisms.findByReference("AMR", code).orElseThrow());
                var agent = loadedAgents.computeIfAbsent(vector.get("antibiotic_code"),
                        code -> antibiotics.findByReference("AMR", code).orElseThrow());
                var edition = editions.get(vector.get("publisher") + "|" + vector.get("version"));
                String method = vector.get("method");
                String clinicalContext = vector.isMapped("clinical_context")
                        && !vector.get("clinical_context").isBlank() ? vector.get("clinical_context") : null;
                String key = edition.getId() + "|" + org.getId() + "|" + agent.getId() + "|" + method + "|"
                        + clinicalContext;
                String diskContent = diskContents
                        .get(String.join("|", vector.get("version"), vector.get("organism_code"),
                                vector.get("antibiotic_code"), method, clinicalContext == null ? "" : clinicalContext));
                var rule = selectedRules.computeIfAbsent(key,
                        ignored -> rules.findContextualRule(edition.getId(), org.getId(), org.getOrganismGroup(),
                                agent.getId(), method, null, method, clinicalContext, diskContent));
                assertNotNull("Every source context must resolve to an explicit rule: " + key, rule);
                var expected = switch (vector.get("expected")) {
                case "S" -> MicroAstInterpretation.SUSCEPTIBLE;
                case "I" -> MicroAstInterpretation.INTERMEDIATE;
                case "R" -> MicroAstInterpretation.RESISTANT;
                case "NO_BREAKPOINT" -> MicroAstInterpretation.NO_BREAKPOINT;
                default -> throw new AssertionError("Unexpected interpretation");
                };
                assertEquals(vector.toString(), expected, interpreter.interpret(rule, MicroAstMethod.valueOf(method),
                        new BigDecimal(vector.get("value")), edition.getAuthority()));
                compared++;
            }
        }
        assertEquals(manifest.get("files").get("evidence/interpretation-vectors.csv").get("rows").asInt(), compared);
        assertTrue(compared > 0);
    }

    private static final java.util.Map<String, String> PNG_DEFAULT_PANELS = java.util.Map.of("Escherichia coli",
            "AST-GN rods", "Klebsiella pneumoniae", "AST-GN rods", "Enterobacter cloacae", "AST-GN rods",
            "Serratia marcescens", "AST-GN rods", "Proteus mirabilis", "AST-GN rods", "Shigella sonnei",
            "AST-Salm+Shig", "Acinetobacter baumannii", "AST - Acinetobacter", "Pseudomonas aeruginosa",
            "AST - Pseudomonas", "Streptococcus anginosus", "AST - Viridans Strep", "Staphylococcus epidermidis",
            "AST-Staph other");
    private static final String CLSI = "pkg--amr-reference-clsi--2026.2--";
    private static final String EUCAST = "pkg--amr-reference-eucast--2026.2--";

    @Test
    public void pngDefaultPanelsWithBothEditionsInTheConfiguredLoadOrder() throws Exception {
        copyPngFiles("microbiology-organisms", CLSI + "organisms.csv", EUCAST + "organisms.csv");
        copyPngFiles("microbiology-antimicrobials", CLSI + "antimicrobials.csv", EUCAST + "antimicrobials.csv");
        var packages = reload(true);
        assertFalse(packages.toString(), packages.hasErrors());

        assertEquals(java.util.List.of(), pngDefaultPanelMismatches());
        assertNull("A package group alone claims no default panel",
                organisms.findByDisplayNameIgnoreCase("Citrobacter freundii").orElseThrow().getDefaultAstPanelId());
    }

    @Test
    public void pngDefaultPanelsWhenEucastLoadsFirst() throws Exception {
        copyPngFiles("microbiology-organisms", EUCAST + "organisms.csv");
        copyPngFiles("microbiology-antimicrobials", CLSI + "antimicrobials.csv", EUCAST + "antimicrobials.csv");
        var eucast = reload(true);
        assertFalse(eucast.toString(), eucast.hasErrors());
        copyPngFiles("microbiology-organisms", CLSI + "organisms.csv");
        var clsi = reload(true);
        assertFalse(clsi.toString(), clsi.hasErrors());

        assertEquals(java.util.List.of(), pngDefaultPanelMismatches());
    }

    @Test
    public void panelFileRejectsADefaultThatMatchesNoOrganism() throws Exception {
        var fixture = new ReferencePackageFixture("2099.35", "NODEFAULT");
        load(fixture);
        writePanels("panelName,antibioticWhonetCode,defaultFor\nUnclaimed panel," + fixture.antibiotic
                + ",Synthetic organism NODEFAULT;No such organism group\n");

        var result = reloadPanels(true);

        assertTrue(result.hasErrors());
        assertTrue(result.toString(), result.toString().contains("No such organism group"));
        assertFalse(result.toString(), result.toString().contains("Synthetic organism NODEFAULT"));
        assertNull(panels.findCurrentByNameIgnoreCase("Unclaimed panel"));
    }

    private java.util.List<String> pngDefaultPanelMismatches() throws Exception {
        copyPngFiles("microbiology-organism-groups", "png-organism-groups.csv");
        var groups = reloadOrganismGroups(true);
        assertFalse(groups.toString(), groups.hasErrors());
        copyPngFiles("microbiology-ast-panels", "png-ast-panels.csv");
        var load = reloadPanels(true);
        var mismatches = new java.util.ArrayList<String>();
        if (load.hasErrors()) {
            mismatches.add("panel file errors: " + load);
        }
        PNG_DEFAULT_PANELS.forEach((name, panelName) -> {
            var organism = organisms.findByDisplayNameIgnoreCase(name).orElseThrow();
            var expected = panels.findCurrentByNameIgnoreCase(panelName);
            String actual = organism.getDefaultAstPanelId();
            if (expected == null || !expected.getId().equals(actual)) {
                mismatches.add(name + " (group " + organism.getOrganismGroup() + ") gets "
                        + (actual == null ? "no default panel"
                                : panels.get(actual).map(MicroAstPanel::getName).orElse(actual))
                        + ", PNG expects " + panelName);
            }
        });
        java.util.Collections.sort(mismatches);
        return mismatches;
    }

    private void copyPngFiles(String domain, String... names) throws Exception {
        Path directory = configurationFiles.getRoot().toPath().resolve(domain);
        Files.createDirectories(directory);
        for (String name : names) {
            Files.copy(Path.of(getClass().getResource("/testdata/microbiology/png-panel-defaults/" + name).toURI()),
                    directory.resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private MicroBreakpointStandard standard(ReferencePackageFixture fixture) {
        return standards.findByAuthorityAndVersion("EUCAST", fixture.edition).orElseThrow();
    }

    @Test
    public void organismGroupFileGivesAnOrganismSeveralGroupsAndAReloadDropsARemovedMembership() throws Exception {
        var fixture = new ReferencePackageFixture("2099.30", "GROUPS");
        load(fixture);
        var klebsiella = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var coli = organism("E. coli " + suffix());
        String species = "Klebsiella spp. " + suffix();
        String nonEscppm = "Enterobacterales non-ESCPPM " + suffix();
        writeOrganismGroups(GROUPS_HEADER + klebsiella.getDisplayName() + ",,," + species + "\n" + ",AMR,"
                + fixture.organism + "," + nonEscppm + "\n" + coli.getDisplayName() + ",,," + nonEscppm + "\n");
        var loaded = reloadOrganismGroups(false);
        assertFalse(loaded.toString(), loaded.hasErrors());
        assertEquals(java.util.List.of(nonEscppm, species), labGroups(klebsiella));
        assertEquals(java.util.List.of(nonEscppm), labGroups(coli));
        assertEquals(nonEscppm + "; " + species, referenceAdmin.getOrganism(klebsiella.getId()).labGroups);
        var offered = referenceAdmin.getOptions("lab-organism-groups").stream().map(option -> option.label).toList();
        assertTrue(offered.toString(), offered.containsAll(java.util.List.of(species, nonEscppm)));

        writeOrganismGroups(GROUPS_HEADER + klebsiella.getDisplayName() + ",,," + species + "\n" + coli.getDisplayName()
                + ",,," + nonEscppm + "\n");
        assertFalse(reloadOrganismGroups(false).hasErrors());
        assertEquals(java.util.List.of(species), labGroups(klebsiella));
        assertEquals(java.util.List.of(nonEscppm), labGroups(coli));
        assertEquals(java.util.List.of(klebsiella.getDisplayName() + " removed from group " + nonEscppm),
                groupsHandler.getLastSummary().getRows().stream().filter(
                        row -> row.outcome() == org.openelisglobal.configuration.service.CsvLoadSummary.Outcome.REMOVED)
                        .map(row -> row.reason()).toList());
    }

    @Test
    public void organismGroupReloadMovesTheDefaultPanelWithTheGroups() throws Exception {
        var fixture = new ReferencePackageFixture("2099.54", "REGROUP");
        load(fixture);
        var organism = organism("Regroup organism " + suffix());
        var other = organism("Regroup organism " + suffix());
        String first = "Regroup first " + suffix();
        String second = "Regroup second " + suffix();
        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,," + first + "\n" + other.getDisplayName()
                + ",,," + second + "\n");
        assertFalse(reloadOrganismGroups(false).hasErrors());
        writePanels("panelName,antibioticWhonetCode,defaultFor\nSynthetic first panel REGROUP," + fixture.antibiotic
                + "," + first + "\nSynthetic second panel REGROUP," + fixture.antibiotic + "," + second + "\n");
        assertFalse(reloadPanels(false).hasErrors());

        writeOrganismGroups(GROUPS_HEADER + other.getDisplayName() + ",,," + first + "\n" + organism.getDisplayName()
                + ",,," + second + "\n");
        var result = reloadOrganismGroups(false);

        assertFalse(result.toString(), result.hasErrors());
        assertEquals(panels.findCurrentByNameIgnoreCase("Synthetic second panel REGROUP").getId(),
                organisms.get(organism.getId()).orElseThrow().getDefaultAstPanelId());
    }

    @Test
    public void organismGroupFileListsEveryUnknownOrganismWithItsLineAndSavesNothing() throws Exception {
        var fixture = new ReferencePackageFixture("2099.31", "GROUPBAD");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        String group = "Group " + suffix();
        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,," + group + "\n" + "No such organism A,,,"
                + group + "\n" + "No such organism B,AMR,NO-SUCH-CODE," + group + "\n");

        String error = reloadOrganismGroups(false).files().get(0).errorMessage();

        assertTrue(error, error.contains("Line 3: unknown organism No such organism A"));
        assertTrue(error, error.contains("Line 4: unknown organism No such organism B"));
        assertTrue(labGroups(organism).isEmpty());
    }

    @Test
    public void reportingRuleFileNamingAnUnknownGroupListsItWithTheOtherBadRowsAndSavesNothing() throws Exception {
        var fixture = new ReferencePackageFixture("2099.32", "GROUPRULE");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        String group = "Group " + suffix();
        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,," + group + "\n");
        assertFalse(reloadOrganismGroups(false).hasErrors());
        writeReportingRules(
                RULES_HEADER + "GROUP," + group + ",ANY," + fixture.antibiotic + ",Synthetic,DONT_REPORT,,,,,,,,\n"
                        + "GROUP,No such group,ANY," + fixture.antibiotic + ",Synthetic,REPORT,,,,,,,,\n"
                        + "SPECIES,No such organism,ANY," + fixture.antibiotic + ",Synthetic,REPORT,,,,,,,,\n");

        String error = reloadReportingRules(false).files().get(0).errorMessage();

        assertTrue(error, error.contains("Line 3: unknown organism group No such group"));
        assertTrue(error, error.contains("Line 4: unknown organism No such organism"));
        assertTrue(rulesFor(antibioticId).isEmpty());
    }

    @Test
    public void groupRulesThatDisagreeForAnOrganismInBothGroupsFailTheRulesFileAndNameBothRows() throws Exception {
        var fixture = new ReferencePackageFixture("2099.33", "GROUPTIE");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        String first = "Group A " + suffix();
        String second = "Group B " + suffix();
        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,," + first + "\n" + organism.getDisplayName()
                + ",,," + second + "\n");
        assertFalse(reloadOrganismGroups(false).hasErrors());
        writeReportingRules(
                RULES_HEADER + "GROUP," + first + ",ANY," + fixture.antibiotic + ",Synthetic,DONT_REPORT,,,,,,,,\n"
                        + "GROUP," + second + ",ANY," + fixture.antibiotic + ",Synthetic,REPORT,,,,,,,,\n");

        String error = reloadReportingRules(false).files().get(0).errorMessage();

        assertTrue(error, error.contains("Lines 2 and 3:"));
        assertTrue(error, error.contains(first) && error.contains(second));
        assertTrue(rulesFor(antibioticId).isEmpty());

        writeReportingRules(
                RULES_HEADER + "GROUP," + first + ",ANY," + fixture.antibiotic + ",Synthetic,DONT_REPORT,,,,,,,,\n"
                        + "GROUP," + second + ",ANY," + fixture.antibiotic + ",Synthetic,DONT_REPORT,,,,,,,,\n");
        assertFalse(reloadReportingRules(false).hasErrors());
        assertEquals(2, rulesFor(antibioticId).size());
    }

    @Test
    public void aSpeciesRuleForTheSharedOrganismSettlesGroupRulesThatDisagree() throws Exception {
        var fixture = new ReferencePackageFixture("2099.35", "GROUPSPEC");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        String first = "Group A " + suffix();
        String second = "Group B " + suffix();
        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,," + first + "\n" + organism.getDisplayName()
                + ",,," + second + "\n");
        assertFalse(reloadOrganismGroups(false).hasErrors());
        writeReportingRules(
                RULES_HEADER + "GROUP," + first + ",ANY," + fixture.antibiotic + ",Synthetic,DONT_REPORT,,,,,,,,\n"
                        + "GROUP," + second + ",ANY," + fixture.antibiotic + ",Synthetic,REPORT,,,,,,,,\n" + "SPECIES,"
                        + organism.getDisplayName() + ",ANY," + fixture.antibiotic + ",Synthetic,REPORT,,,,,,,,\n");

        var reloaded = reloadReportingRules(false);

        assertFalse(reloaded.toString(), reloaded.hasErrors());
        assertEquals(java.util.Set.of(first, second), rulesFor(antibioticId).stream().map(rule -> rule.organismGroup)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet()));
        assertEquals(1,
                rulesFor(antibioticId).stream().filter(rule -> organism.getId().equals(rule.organismId)).count());
    }

    @Test
    public void groupFileThatPutsAnOrganismUnderTwoDisagreeingRulesFailsAndNamesBothGroups() throws Exception {
        var fixture = new ReferencePackageFixture("2099.34", "GROUPTIE2");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var other = organism("Other organism " + suffix());
        String first = "Group A " + suffix();
        String second = "Group B " + suffix();
        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,," + first + "\n" + other.getDisplayName()
                + ",,," + second + "\n");
        assertFalse(reloadOrganismGroups(false).hasErrors());
        writeReportingRules(
                RULES_HEADER + "GROUP," + first + ",ANY," + fixture.antibiotic + ",Synthetic,DONT_REPORT,,,,,,,,\n"
                        + "GROUP," + second + ",ANY," + fixture.antibiotic + ",Synthetic,REPORT,,,,,,,,\n");
        assertFalse(reloadReportingRules(false).hasErrors());

        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,," + first + "\n" + organism.getDisplayName()
                + ",,," + second + "\n" + other.getDisplayName() + ",,," + second + "\n");
        String error = reloadOrganismGroups(false).files().get(0).errorMessage();

        assertTrue(error, error.contains("Line 3:"));
        assertTrue(error, error.contains(organism.getDisplayName()));
        assertTrue(error, error.contains(first) && error.contains(second) && error.contains(fixture.antibiotic));
        assertEquals(java.util.List.of(first), labGroups(organism));
    }

    private void load(ReferencePackageFixture fixture) throws Exception {
        writeFiles(fixture);
        var result = reload(true);
        assertFalse(result.toString(), result.hasErrors());
    }

    private void writeFiles(ReferencePackageFixture fixture) throws Exception {
        for (var pair : java.util.Map.of("organisms.csv", "microbiology-organisms", "antimicrobials.csv",
                "microbiology-antimicrobials", "breakpoints.csv", "microbiology-breakpoints").entrySet()) {
            Path target = configurationFiles.getRoot().toPath().resolve(pair.getValue()).resolve(pair.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, fixture.files.get(pair.getKey()));
        }
    }

    private void writePanels(String csv) throws Exception {
        writePanels("panels.csv", csv);
    }

    private void writePanels(String fileName, String csv) throws Exception {
        Path target = configurationFiles.getRoot().toPath().resolve("microbiology-ast-panels").resolve(fileName);
        Files.createDirectories(target.getParent());
        Files.writeString(target, csv);
    }

    private org.openelisglobal.configuration.service.ConfigurationReloadResult reloadPanels(boolean force) {
        return initialization.reload(new org.openelisglobal.configuration.service.ConfigurationReloadOptions(
                java.util.Set.of("microbiology-ast-panels"), force));
    }

    private org.openelisglobal.configuration.service.ConfigurationReloadResult reload(boolean force) {
        return initialization.reload(new org.openelisglobal.configuration.service.ConfigurationReloadOptions(
                java.util.Set.of("microbiology-organisms", "microbiology-antimicrobials", "microbiology-breakpoints"),
                force));
    }

    @Test
    public void forcedReloadDoesNotRewriteUnchangedRules() throws Exception {
        var fixture = new ReferencePackageFixture("2099.14", "UNCHANGED");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        writeReportingRules(RULES_HEADER + "SPECIES," + organism.getDisplayName() + ",ANY," + fixture.antibiotic
                + ",Synthetic,REPORT,,,,YES,,,,\n");
        assertFalse(reloadReportingRules(false).hasErrors());
        String ruleId = rulesFor(antibioticId).get(0).id;
        var written = reportingRuleDAO.get(ruleId).orElseThrow().getLastupdated();
        assertFalse(reloadReportingRules(true).hasErrors());
        assertEquals(written, reportingRuleDAO.get(ruleId).orElseThrow().getLastupdated());
    }

    @Test
    public void unknownOrganismAndAntibioticInARuleFileAreListedForADecisionAndAnAliasLoadsThem() throws Exception {
        var fixture = new ReferencePackageFixture("2099.55", "UNKNOWNOA");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        var antibiotic = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow();
        String organismName = "Organism alias " + suffix();
        String agentCode = "AGENT_" + suffix().toUpperCase(java.util.Locale.ROOT);
        writeReportingRules(RULES_HEADER + "SPECIES," + organismName + ",ANY," + fixture.antibiotic
                + ",Synthetic,DONT_REPORT,,,,,,,,\n" + "ANY,,ANY," + agentCode + ",Synthetic,REPORT,,,,,,,,\n");
        assertTrue(reloadReportingRules(false).hasErrors());

        var open = unresolvedReferences.getOpen();
        var organismItem = open.stream().filter(r -> organismName.equals(r.getReferenceValue())).findFirst()
                .orElseThrow();
        var agentItem = open.stream().filter(r -> agentCode.equals(r.getReferenceValue())).findFirst().orElseThrow();
        assertEquals("ORGANISM", organismItem.getReferenceType());
        assertEquals("ANTIBIOTIC", agentItem.getReferenceType());

        unresolvedReferences.resolve(organismItem.getId(), "ALIAS", organism.getId(), "1");
        unresolvedReferences.resolve(agentItem.getId(), "ALIAS", antibiotic.getId(), "1");
        var reloaded = reloadReportingRules(true);
        assertFalse(reloaded.toString(), reloaded.hasErrors());
    }

    @Test
    public void serviceErrorsCarryTheirLineAndADatabaseErrorFailsTheFileAtOnce() throws Exception {
        var fixture = new ReferencePackageFixture("2099.64", "ROWERR");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        writeReportingRules(RULES_HEADER + "SPECIES," + organism.getDisplayName() + ",ANY," + fixture.antibiotic
                + ",Synthetic,REPORT,,,,," + "x".repeat(1001) + ",ALWAYS,,\n");
        String rulesError = reloadReportingRules(false).files().get(0).errorMessage();
        assertTrue(rulesError, rulesError.contains("Line 2: REPORTING_RULE_NOTE_TOO_LONG"));

        writePanels("panelName,antibioticWhonetCode\nSynthetic duplicate panel," + fixture.antibiotic
                + "\nSynthetic duplicate panel," + fixture.antibiotic + "\n");
        String duplicate = reloadPanels(true).files().get(0).errorMessage();
        assertTrue(duplicate, duplicate.contains("Line 2: Duplicate antibiotic"));

        writePanels("panelName,antibioticWhonetCode\n" + "P".repeat(300) + "," + fixture.antibiotic
                + "\nSynthetic panel after a database error," + fixture.antibiotic + "\n");
        String database = reloadPanels(true).files().get(0).errorMessage();
        assertTrue(database, database.contains("too long"));
        assertFalse(database, database.contains("current transaction is aborted"));

        writeOrganismGroups(GROUPS_HEADER + organism.getDisplayName() + ",,," + "G".repeat(101) + "\n");
        String group = reloadOrganismGroups(false).files().get(0).errorMessage();
        assertTrue(group, group.contains("Line 2: group is longer than 100 characters"));
    }

    @Test
    public void unknownSampleTypeInARuleFileIsListedForADecision() throws Exception {
        var fixture = new ReferencePackageFixture("2099.15", "UNKNOWNST");
        load(fixture);
        var organism = organisms.findByReference("AMR", fixture.organism).orElseThrow();
        String sampleType = "CSF " + java.util.UUID.randomUUID().toString().substring(0, 8);
        writeReportingRules(RULES_HEADER + "SPECIES," + organism.getDisplayName() + "," + sampleType + ","
                + fixture.antibiotic + ",Synthetic,REPORT,,,,,,,,\n");
        assertTrue(reloadReportingRules(false).hasErrors());
        assertTrue(unresolvedReferences.getOpen().stream().anyMatch(r -> sampleType.equals(r.getReferenceValue())));
    }

    @Test
    public void qcRangeFileLoadsRangesPerMethodAndDiskContentAndUpdatesInPlace() throws Exception {
        var fixture = new ReferencePackageFixture("2099.16", "QCRANGE");
        load(fixture);
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        String standardId = standard(fixture).getId();
        writeQcRanges(QC_RANGES_HEADER + "E. coli ATCC 25922," + fixture.antibiotic + ",ZONE,10,EUCAST,"
                + fixture.edition + ",19,25\n" + "E. coli ATCC 25922," + fixture.antibiotic + ",MIC,,EUCAST,"
                + fixture.edition + ",0.25,1\n");
        assertFalse(reloadQcRanges(false).hasErrors());

        var zone = qcRanges.find("e. coli atcc 25922", antibioticId, MicroAstMethod.ZONE, "10", standardId)
                .orElseThrow();
        assertEquals(0, new BigDecimal("19").compareTo(zone.getLow()));
        assertEquals(0, new BigDecimal("25").compareTo(zone.getHigh()));
        assertTrue(qcRanges.find("E. coli ATCC 25922", antibioticId, MicroAstMethod.ZONE, "30", standardId).isEmpty());
        var mic = qcRanges.find("E. coli ATCC 25922", antibioticId, MicroAstMethod.MIC, null, standardId).orElseThrow();
        assertEquals(0, new BigDecimal("0.25").compareTo(mic.getLow()));

        var written = zone.getLastupdated();
        assertFalse(reloadQcRanges(true).hasErrors());
        assertEquals(written, qcRanges.find("E. coli ATCC 25922", antibioticId, MicroAstMethod.ZONE, "10", standardId)
                .orElseThrow().getLastupdated());

        writeQcRanges(QC_RANGES_HEADER + "E. coli ATCC 25922," + fixture.antibiotic + ",ZONE,10,EUCAST,"
                + fixture.edition + ",19,26\n");
        assertFalse(reloadQcRanges(true).hasErrors());
        var updated = qcRanges.find("E. coli ATCC 25922", antibioticId, MicroAstMethod.ZONE, "10", standardId)
                .orElseThrow();
        assertEquals(zone.getId(), updated.getId());
        assertEquals(0, new BigDecimal("26").compareTo(updated.getHigh()));
        assertTrue(qcRanges.find("E. coli ATCC 25922", antibioticId, MicroAstMethod.MIC, null, standardId).isPresent());
    }

    @Test
    public void qcRangeFileIsAppliedWholeOrNotAtAll() throws Exception {
        var fixture = new ReferencePackageFixture("2099.17", "QCATOMIC");
        load(fixture);
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        writeQcRanges(QC_RANGES_HEADER + "E. coli ATCC 25922," + fixture.antibiotic + ",ZONE,10,EUCAST,"
                + fixture.edition + ",19,25\n" + "E. coli ATCC 25922,NO_SUCH_AGENT,ZONE,10,EUCAST," + fixture.edition
                + ",19,25\n");
        assertTrue(reloadQcRanges(false).hasErrors());
        assertTrue(
                qcRanges.find("E. coli ATCC 25922", antibioticId, MicroAstMethod.ZONE, "10", standard(fixture).getId())
                        .isEmpty());
    }

    @Test
    public void qcRangeFileRejectsAnUnknownGuidelineAnInvertedRangeAndAMicWithDiskContent() throws Exception {
        var fixture = new ReferencePackageFixture("2099.18", "QCBAD");
        load(fixture);
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        String standardId = standard(fixture).getId();
        for (String row : new String[] { "ZONE,10,EUCAST,1900.1,19,25", "ZONE,10,EUCAST," + fixture.edition + ",25,19",
                "MIC,10,EUCAST," + fixture.edition + ",0.25,1", "DISK,10,EUCAST," + fixture.edition + ",19,25" }) {
            writeQcRanges(QC_RANGES_HEADER + "E. coli ATCC 25922," + fixture.antibiotic + "," + row + "\n");
            assertTrue(row, reloadQcRanges(true).hasErrors());
        }
        assertTrue(qcRanges.find("E. coli ATCC 25922", antibioticId, MicroAstMethod.ZONE, "10", standardId).isEmpty());
        assertTrue(qcRanges.find("E. coli ATCC 25922", antibioticId, MicroAstMethod.MIC, "10", standardId).isEmpty());
    }

    @Test
    public void qcRangeFileRejectsALimitTheColumnCannotKeepExactlyOrThatIsNotPositive() throws Exception {
        var fixture = new ReferencePackageFixture("2099.19", "QCLIMIT");
        load(fixture);
        var antibioticId = antibiotics.findByWhonetCodeIgnoreCase(fixture.antibiotic).orElseThrow().getId();
        for (String limits : new String[] { "0.03125,1", "0,1", "-1,1", "0.25,123456789" }) {
            writeQcRanges(QC_RANGES_HEADER + "E. coli ATCC 25922," + fixture.antibiotic + ",MIC,,EUCAST,"
                    + fixture.edition + "," + limits + "\n");
            var result = reloadQcRanges(true);
            assertTrue(limits, result.hasErrors());
            assertTrue(result.files().get(0).errorMessage(), result.files().get(0).errorMessage().contains("Line 2"));
        }
        assertTrue(
                qcRanges.find("E. coli ATCC 25922", antibioticId, MicroAstMethod.MIC, null, standard(fixture).getId())
                        .isEmpty());
    }

    private static final String QC_RANGES_HEADER = "qc_strain,agent,method,disk_content,guideline,version,low,high\n";

    private void writeQcRanges(String csv) throws Exception {
        Path target = configurationFiles.getRoot().toPath().resolve("microbiology-qc-ranges").resolve("qc-ranges.csv");
        Files.createDirectories(target.getParent());
        Files.writeString(target, csv);
    }

    private org.openelisglobal.configuration.service.ConfigurationReloadResult reloadQcRanges(boolean force) {
        return initialization.reload(new org.openelisglobal.configuration.service.ConfigurationReloadOptions(
                java.util.Set.of("microbiology-qc-ranges"), force));
    }

    private static final String RULES_HEADER = "organism_level,organism,specimen_group,agent,agent_name,report_default,"
            + "cascade_when,trigger_agents,report_as,always_report_R,comment,comment_when,sop_section,note\n";

    private java.util.List<org.openelisglobal.microbiology.form.MicroReportingRuleForm> rulesFor(String antibioticId) {
        return reportingRules.getRules().stream().filter(r -> antibioticId.equals(r.antibioticId)).toList();
    }

    private void writeReportingRules(String csv) throws Exception {
        Path target = configurationFiles.getRoot().toPath().resolve("microbiology-reporting-rules")
                .resolve("reporting-rules.csv");
        Files.createDirectories(target.getParent());
        Files.writeString(target, csv);
    }

    private org.openelisglobal.configuration.service.ConfigurationReloadResult reloadReportingRules(boolean force) {
        return initialization.reload(new org.openelisglobal.configuration.service.ConfigurationReloadOptions(
                java.util.Set.of("microbiology-reporting-rules"), force));
    }

    private static final String GROUPS_HEADER = "organism,organism_source,organism_code,group\n";

    private void writeOrganismGroups(String csv) throws Exception {
        Path target = configurationFiles.getRoot().toPath().resolve("microbiology-organism-groups")
                .resolve("organism-groups.csv");
        Files.createDirectories(target.getParent());
        Files.writeString(target, csv);
    }

    private org.openelisglobal.configuration.service.ConfigurationReloadResult reloadOrganismGroups(boolean force) {
        return initialization.reload(new org.openelisglobal.configuration.service.ConfigurationReloadOptions(
                java.util.Set.of("microbiology-organism-groups"), force));
    }

    private java.util.List<String> labGroups(MicroOrganism organism) {
        return jdbcTemplate.queryForList(
                "SELECT group_name FROM clinlims.micro_organism_group WHERE organism_id = ? ORDER BY group_name",
                String.class, organism.getId());
    }

    private MicroOrganism organism(String displayName) {
        var organism = new MicroOrganism();
        organism.setDisplayName(displayName);
        organism.setWhonetCode("X" + suffix());
        organisms.insert(organism);
        return organism;
    }

    private static String suffix() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
