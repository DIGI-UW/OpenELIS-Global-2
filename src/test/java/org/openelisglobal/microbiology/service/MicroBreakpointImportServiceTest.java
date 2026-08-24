package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroBreakpointImportPreviewForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;

@RunWith(MockitoJUnitRunner.class)
public class MicroBreakpointImportServiceTest {

    private static final String CSV = "publisher,version,organism_or_group,antibiotic_whonet_code,method,"
            + "specimen_type_id,breakpoint_type,susceptible_value,intermediate_lower_value,"
            + "intermediate_upper_value,resistant_value,units\n"
            + "CLSI,SYNTH-2026,Escherichia coli,CIP,MIC,,MIC,1,2,3,4,ug/mL\n"
            + "CLSI,SYNTH-2026,Missing organism,CIP,MIC,,MIC,1,2,3,4,ug/mL\n"
            + "CLSI,SYNTH-2026,Escherichia coli,CIP,INVALID,,MIC,1,2,3,4,ug/mL\n";

    @Mock
    private MicroOrganismDAO organismDAO;
    @Mock
    private MicroAntibioticDAO antibioticDAO;
    @Mock
    private MicroBreakpointStandardDAO standardDAO;
    @Mock
    private MicroBreakpointRuleDAO ruleDAO;
    @Mock
    private TypeOfSampleService typeOfSampleService;

    private MicroBreakpointImportService service;

    @Before
    public void setUp() {
        service = new MicroBreakpointImportServiceImpl(organismDAO, antibioticDAO, standardDAO, ruleDAO,
                typeOfSampleService);
        MicroOrganism organism = new MicroOrganism();
        organism.setId("eco");
        organism.setDisplayName("Escherichia coli");
        organism.setOrganismGroup("Enterobacterales");
        when(organismDAO.findByDisplayNameIgnoreCase("Escherichia coli")).thenReturn(Optional.of(organism));
        when(organismDAO.findByDisplayNameIgnoreCase("Missing organism")).thenReturn(Optional.empty());
        when(organismDAO.getActiveOrganisms()).thenReturn(List.of(organism));
        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setId("cip");
        antibiotic.setWhonetCode("CIP");
        when(antibioticDAO.findByWhonetCodeIgnoreCase("CIP")).thenReturn(Optional.of(antibiotic));
        when(standardDAO.findByAuthorityAndVersion("CLSI", "SYNTH-2026")).thenReturn(Optional.empty());
    }

    @Test
    public void previewReturnsValidRowsAndActionableRowErrorsWithoutWriting() {
        MicroBreakpointImportPreviewForm preview = service.preview(CSV);

        assertEquals(3, preview.totalRows);
        assertEquals(1, preview.validRows);
        assertEquals(2, preview.skippedRows);
        assertEquals(List.of(3, 4), preview.errors.stream().map(error -> error.rowNumber).toList());
        assertTrue(preview.errors.get(0).message.contains("Missing organism"));
        assertTrue(preview.errors.get(1).message.contains("INVALID"));
        assertFalse(preview.previewToken.isBlank());
        verify(standardDAO, org.mockito.Mockito.never()).insert(org.mockito.ArgumentMatchers.any());
        verify(ruleDAO, org.mockito.Mockito.never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void applyCreatesLoadedStandardAndIsIdempotentForUnchangedRows() {
        MicroBreakpointImportPreviewForm preview = service.preview(CSV);

        MicroBreakpointImportPreviewForm applied = service.apply(preview.previewToken, "42");

        ArgumentCaptor<MicroBreakpointStandard> standardCaptor = ArgumentCaptor.forClass(MicroBreakpointStandard.class);
        verify(standardDAO).insert(standardCaptor.capture());
        assertEquals("LOADED", standardCaptor.getValue().getLifecycleStatus());
        assertEquals("42", standardCaptor.getValue().getLastUpdatedBy());
        ArgumentCaptor<MicroBreakpointRule> ruleCaptor = ArgumentCaptor.forClass(MicroBreakpointRule.class);
        verify(ruleDAO).insert(ruleCaptor.capture());
        assertTrue(ruleCaptor.getValue().isSeeded());
        assertFalse(ruleCaptor.getValue().isLocallyCustomized());
        assertEquals(1, applied.validRows);
        assertEquals(1, applied.importedRows);
        assertEquals(2, applied.skippedRows);

        when(standardDAO.findByAuthorityAndVersion("CLSI", "SYNTH-2026"))
                .thenReturn(Optional.of(standardCaptor.getValue()));
        when(ruleDAO.findBySourceRowHash(ruleCaptor.getValue().getSourceRowHash()))
                .thenReturn(Optional.of(ruleCaptor.getValue()));
        MicroBreakpointImportPreviewForm retry = service.preview(CSV);
        MicroBreakpointImportPreviewForm retried = service.apply(retry.previewToken, "42");
        assertEquals(1, retried.validRows);
        assertEquals(0, retried.importedRows);
        assertEquals(1, retried.unchangedRows);
    }

    @Test
    public void applyPreservesLocalCorrectionForTheSameNaturalKey() {
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setId("standard-1");
        standard.setAuthority("CLSI");
        standard.setVersion("SYNTH-2026");
        when(standardDAO.findByAuthorityAndVersion("CLSI", "SYNTH-2026")).thenReturn(Optional.of(standard));
        MicroBreakpointRule correction = new MicroBreakpointRule();
        correction.setId("local-rule");
        correction.setLocallyCustomized(true);
        when(ruleDAO.findByNaturalKey("standard-1", "eco", null, "cip", "MIC", null, "MIC"))
                .thenReturn(Optional.of(correction));

        MicroBreakpointImportPreviewForm preview = service.preview(CSV);
        MicroBreakpointImportPreviewForm applied = service.apply(preview.previewToken, "42");

        assertEquals(0, applied.importedRows);
        assertEquals(0, applied.validRows);
        assertEquals(3, applied.skippedRows);
        assertTrue(applied.errors.stream().anyMatch(error -> error.message.contains("locally customized")));
        verify(ruleDAO, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(ruleDAO, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void previewRejectsEveryRowWithADuplicateNaturalKey() {
        String duplicateCsv = csv("CLSI,SYNTH-2026,Escherichia coli,CIP,MIC,,MIC,1,2,3,4,ug/mL\n"
                + "CLSI,SYNTH-2026,Escherichia coli,CIP,MIC,,MIC,2,3,4,5,ug/mL\n");

        MicroBreakpointImportPreviewForm preview = service.preview(duplicateCsv);

        assertEquals(0, preview.validRows);
        assertEquals(2, preview.skippedRows);
        assertTrue(preview.errors.stream().allMatch(error -> error.message.contains("Duplicate breakpoint key")));
    }

    @Test
    public void previewRejectsUnknownOrganismGroupsAndSpecimenTypes() {
        String invalidContextCsv = csv("CLSI,SYNTH-2026,group:Enterobactrales,CIP,MIC,,MIC,1,2,3,4,ug/mL\n"
                + "CLSI,SYNTH-2026,group:Enterobacterales,CIP,MIC,999,MIC,1,2,3,4,ug/mL\n");

        MicroBreakpointImportPreviewForm preview = service.preview(invalidContextCsv);

        assertEquals(0, preview.validRows);
        assertTrue(preview.errors.stream().anyMatch(error -> error.message.contains("Unknown organism group")));
        assertTrue(preview.errors.stream().anyMatch(error -> error.message.contains("Unknown specimen type")));
    }

    @Test
    public void applyRejectsArchivedStandardsWithoutWritingRules() {
        MicroBreakpointStandard archived = new MicroBreakpointStandard();
        archived.setId("standard-1");
        archived.setAuthority("CLSI");
        archived.setVersion("SYNTH-2026");
        archived.setLifecycleStatus("ARCHIVED");
        when(standardDAO.findByAuthorityAndVersion("CLSI", "SYNTH-2026")).thenReturn(Optional.of(archived));
        MicroBreakpointImportPreviewForm preview = service.preview(CSV);

        try {
            service.apply(preview.previewToken, "42");
            org.junit.Assert.fail("Expected archived standard import to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("archived"));
        }

        verify(ruleDAO, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(ruleDAO, never()).update(org.mockito.ArgumentMatchers.any());
    }

    private String csv(String rows) {
        return "publisher,version,organism_or_group,antibiotic_whonet_code,method,specimen_type_id,"
                + "breakpoint_type,susceptible_value,intermediate_lower_value,intermediate_upper_value,"
                + "resistant_value,units\n" + rows;
    }
}
