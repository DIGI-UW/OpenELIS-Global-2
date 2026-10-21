package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroReferencePackageIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicroReferencePackageConfigurationHandler handler;
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
        executeDataSetWithStateManagement("testdata/microbiology/reference-package.xml");
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
        assertTrue(administration.getStandard(standardId).packageManaged);
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
    public void failedSourceVectorRollsBackEntireServiceTransaction() throws Exception {
        // Leave the test transaction so the service owns commit/rollback, as at
        // startup.
        TestTransaction.flagForRollback();
        TestTransaction.end();
        var fixture = new ReferencePackageFixture("2099.3", "ROLLBACK");
        fixture.files.compute("interpretation-vectors.csv", (key, value) -> value.replace("4,I", "4,R"));
        var error = assertThrows(IllegalArgumentException.class, () -> load(fixture));
        assertTrue(error.getMessage(), error.getMessage().contains("Source interpretation differs"));
        assertFalse(organisms.findByReference("AMR", fixture.organism).isPresent());
        assertFalse(antibiotics.findByReference("AMR", fixture.antibiotic).isPresent());
        assertFalse(standards.findByAuthorityAndVersion("EUCAST", fixture.edition).isPresent());
    }

    @Test
    public void changedBytesCannotReplaceAnExistingEdition() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "IMMUTABLE");
        load(fixture);
        fixture.files.put("NOTICE.txt", "Different content under an existing identity");
        var error = assertThrows(IllegalArgumentException.class, () -> load(fixture));
        assertTrue(error.getMessage(), error.getMessage().contains("different content"));
    }

    @Test
    public void realPublishedCandidatePassesReceivingApplicationAndSourceVectors() throws Exception {
        String path = System.getProperty("amr.reference.artifact");
        org.junit.Assume.assumeTrue("Supply -Damr.reference.artifact for the generated-package contract test",
                path != null);
        try (var input = Files.newInputStream(Path.of(path))) {
            handler.processConfiguration(input, Path.of(path).getFileName().toString());
        }
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
    }

    private MicroBreakpointStandard standard(ReferencePackageFixture fixture) {
        return standards.findByAuthorityAndVersion("EUCAST", fixture.edition).orElseThrow();
    }

    private void load(ReferencePackageFixture fixture) throws Exception {
        handler.processConfiguration(new ByteArrayInputStream(fixture.archive()), "reference.zip");
    }
}
