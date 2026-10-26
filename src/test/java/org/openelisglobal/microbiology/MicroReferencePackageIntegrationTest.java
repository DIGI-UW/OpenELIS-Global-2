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
    private MicroAstPanelDAO panels;
    @Autowired
    private MicroAstPanelAntibioticDAO panelAntibiotics;

    @Autowired
    private MicroBreakpointImportService importer;
    @Autowired
    private MicrobiologyConfigurationService configuration;

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
                antibiotic.getId(), "MIC", null, "MIC", "NON_MENINGITIS").orElseThrow().getId());
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

    private MicroBreakpointStandard standard(ReferencePackageFixture fixture) {
        return standards.findByAuthorityAndVersion("EUCAST", fixture.edition).orElseThrow();
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
        Path target = configurationFiles.getRoot().toPath().resolve("microbiology-ast-panels").resolve("panels.csv");
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
}
