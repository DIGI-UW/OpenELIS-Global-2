package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;

import org.junit.Test;

public class MicroReferencePackageValidationTest {
    @Test
    public void acceptsCompleteSyntheticPackage() throws Exception {
        assertEquals(9, MicroReferencePackageServiceImpl
                .validateArchive(new ReferencePackageFixture("2099.1", "VALID").archive()).size());
    }

    @Test
    public void rejectsDuplicateConceptEvenWithValidChecksums() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "DUP");
        fixture.files.compute("organisms.csv",
                (key, value) -> value + value.lines().skip(1).findFirst().orElseThrow() + "\n");
        rejected(fixture, "duplicate reference identity");
    }

    @Test
    public void rejectsRuleUsingUnlistedConceptOrAnotherNamespace() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "REF");
        fixture.files.compute("breakpoints.csv",
                (key, value) -> value.replace("AMR,TEST_ORG_REF", "LOCAL,TEST_ORG_REF"));
        rejected(fixture, "supported package contract");
    }

    @Test
    public void rejectsRuleWhenCoverageExplicitlyExcludesIt() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "COVER");
        fixture.files.compute("coverage.csv",
                (key, value) -> value.replace("SUPPORTED,,", "UNSUPPORTED,context_required,"));
        rejected(fixture, "Unsupported coverage cannot have a rule");
    }

    @Test
    public void rejectsMissingSourceAccounting() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "SOURCE");
        fixture.files.compute("source-rows.csv", (key, value) -> value + "2,EUCAST 2026," + fixture.organism + ","
                + fixture.antibiotic + ",MIC,SEE_COVERAGE\n");
        rejected(fixture, "Incomplete rule coverage or source accounting");
    }

    @Test
    public void rejectsCoveragePointingToDifferentSourceContext() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "CONTEXT");
        fixture.files.compute("source-rows.csv", (key, value) -> value.replace(",MIC,", ",DISK,"));
        rejected(fixture, "Coverage does not match its source row");
    }

    @Test
    public void rejectsDuplicateCsvColumns() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "HEADER");
        fixture.files.compute("organisms.csv", (key, value) -> value.replace("gramStain", "code"));
        rejected(fixture, "duplicate CSV columns");
    }

    @Test
    public void rejectsEditionVersionUnrelatedToPackageVersion() throws Exception {
        var fixture = new ReferencePackageFixture("2099.1", "EDITION");
        fixture.files.compute("breakpoints.csv", (key, value) -> value.replace(fixture.edition, "2026-2099.2"));
        rejected(fixture, "supported package contract");
    }

    private void rejected(ReferencePackageFixture fixture, String message) throws Exception {
        byte[] bytes = fixture.archive();
        var exception = assertThrows(IllegalArgumentException.class,
                () -> MicroReferencePackageServiceImpl.validateArchive(bytes));
        assertTrue(exception.getMessage(), exception.getMessage().contains(message));
    }
}
