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
        for (String edition : java.util.List.of("2025-2026.1", "2026-2026.1")) {
            var standard = standards.findByAuthorityAndVersion("EUCAST", edition).orElseThrow();
            assertEquals("LOADED", standard.getLifecycleStatus());
            assertEquals("N", standard.getIsActive());
            assertTrue(rules.countByStandardId(standard.getId()) > 0);
            assertTrue(rules.getByStandardId(standard.getId()).stream()
                    .noneMatch(MicroBreakpointRule::isResistantInclusive));
        }
        int compared = 0;
        try (var zip = new java.util.zip.ZipFile(path);
                var input = new java.io.InputStreamReader(
                        zip.getInputStream(zip.getEntry("evidence/interpretation-vectors.csv")),
                        java.nio.charset.StandardCharsets.UTF_8);
                var vectors = org.apache.commons.csv.CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                        .build().parse(input)) {
            for (var vector : vectors) {
                var org = organisms.findByReference("AMR", vector.get("organism_code")).orElseThrow();
                var agent = antibiotics.findByReference("AMR", vector.get("antibiotic_code")).orElseThrow();
                var edition = standards.findByAuthorityAndVersion(vector.get("publisher"), vector.get("version"))
                        .orElseThrow();
                var rule = rules.findBestRule(edition.getId(), org.getId(), org.getOrganismGroup(), agent.getId(),
                        "MIC", null, "MIC");
                var expected = switch (vector.get("expected")) {
                case "S" -> MicroAstInterpretation.SUSCEPTIBLE;
                case "I" -> MicroAstInterpretation.INTERMEDIATE;
                case "R" -> MicroAstInterpretation.RESISTANT;
                default -> throw new AssertionError("Unexpected interpretation");
                };
                assertEquals(vector.toString(), expected,
                        interpreter.interpret(rule, MicroAstMethod.MIC, new BigDecimal(vector.get("value"))));
                compared++;
            }
        }
        assertEquals(1746, compared);
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

    private org.openelisglobal.configuration.service.ConfigurationReloadResult reload(boolean force) {
        return initialization.reload(new org.openelisglobal.configuration.service.ConfigurationReloadOptions(
                java.util.Set.of("microbiology-organisms", "microbiology-antimicrobials", "microbiology-breakpoints"),
                force));
    }
}
