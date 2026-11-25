package org.openelisglobal.microbiology;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.Test;
import org.openelisglobal.labelpreset.service.LabelPresetService;
import org.openelisglobal.labelpreset.valueholder.*;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.openpdf.text.pdf.PdfReader;

public class MicroContainerLabelServiceTest {
    private Object previousMessages;

    @org.junit.Before
    public void messages() {
        previousMessages = org.springframework.test.util.ReflectionTestUtils
                .getField(org.openelisglobal.internationalization.MessageUtil.class, "instance");
        var source = new org.springframework.context.support.ResourceBundleMessageSource();
        source.setBasename("languages/message");
        source.setDefaultEncoding("UTF-8");
        org.openelisglobal.internationalization.MessageUtil.setMessageSource(source);
    }

    @org.junit.After
    public void restoreMessages() {
        org.springframework.test.util.ReflectionTestUtils
                .setField(org.openelisglobal.internationalization.MessageUtil.class, "instance", previousMessages);
    }

    @Test
    public void labelsUsePresetDimensionsAndQuantityAndResolveOnlyTheirOwner() throws Exception {
        String caseId = "11111111-1111-4111-8111-111111111111", rowId = "22222222-2222-4222-8222-222222222222";
        var cultures = mock(MicroCaseInoculationDAO.class);
        var workspace = mock(MicroCaseWorkspaceService.class);
        var presets = mock(LabelPresetService.class);
        var isolates = mock(MicroIsolateDAO.class);
        var runs = mock(MicroAstRunDAO.class);
        var service = new MicroContainerLabelServiceImpl(cultures, isolates, runs, workspace, presets);
        var row = new MicroCaseInoculation();
        row.setId(rowId);
        row.setCaseId(caseId);
        row.setSourceSampleItemId("second-specimen");
        var isolate = new org.openelisglobal.microbiology.valueholder.MicroIsolate();
        isolate.setCaseId(caseId);
        isolate.setSourceSampleItemId("second-specimen");
        when(isolates.get(rowId)).thenReturn(Optional.of(isolate));
        var run = new org.openelisglobal.microbiology.valueholder.MicroAstRun();
        run.setIsolateId(rowId);
        when(runs.get(rowId)).thenReturn(Optional.of(run));
        var source = new org.openelisglobal.sampleitem.valueholder.SampleItem();
        source.setId("second-specimen");
        var type = new org.openelisglobal.typeofsample.valueholder.TypeOfSample();
        type.setDescription("Urine");
        source.setTypeOfSample(type);
        when(cultures.getSources(caseId)).thenReturn(List.of(source));
        when(cultures.get(rowId)).thenReturn(Optional.of(row));
        var shell = new MicroCaseShellForm();
        shell.canWrite = true;
        shell.accessionNumber = "LABEL-001";
        shell.id = caseId;
        shell.specimenType = "Blood";
        when(workspace.get(caseId, "actor")).thenReturn(shell);
        var preset = new LabelPreset();
        preset.setId(1);
        preset.setIsActive(true);
        preset.setPrintsPerContainer(true);
        preset.setMaxPerContainer(3);
        preset.setWidthMm(50);
        preset.setHeightMm(25);
        preset.setBarcodeType(BarcodeType.QR);
        var field = new LabelPresetField();
        field.setFieldKey("SPECIMEN_TYPE");
        preset.getFields().add(field);
        when(presets.get(1)).thenReturn(preset);
        var request = new MicroContainerLabelForm();
        request.kind = "PLT";
        request.caseId = caseId;
        request.rowId = rowId;
        request.quantity = 2;
        request.presetId = 1;
        byte[] pdf = service.print(request, "actor");
        try (var reader = new PdfReader(pdf)) {
            assertEquals(2, reader.getNumberOfPages());
            assertEquals(50 * 72f / 25.4f, reader.getPageSize(1).getWidth(), .1f);
            assertEquals(25 * 72f / 25.4f, reader.getPageSize(1).getHeight(), .1f);
        }
        for (String kind : List.of("PLT", "ISO", "AST")) {
            request.kind = kind;
            try (var reader = new PdfReader(service.print(request, "actor"))) {
                String text = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
                assertTrue(kind + " label must identify its source specimen", text.contains("Urine"));
                assertFalse(text.contains("Blood"));
            }
        }
        run.setStatus("CANCELLED");
        request.kind = "AST";
        assertThrows(IllegalArgumentException.class, () -> service.resolve("AST-" + caseId + "-" + rowId, "actor"));
        assertThrows(IllegalArgumentException.class, () -> service.print(request, "actor"));
        run.setStatus("IN_PROGRESS");
        isolate.setCancelledAt(new java.sql.Timestamp(System.currentTimeMillis()));
        for (String kind : List.of("ISO", "AST")) {
            request.kind = kind;
            assertThrows(IllegalArgumentException.class,
                    () -> service.resolve(kind + "-" + caseId + "-" + rowId, "actor"));
            assertThrows(IllegalArgumentException.class, () -> service.print(request, "actor"));
        }
        request.kind = "PLT";
        assertEquals("CULTURE", service.resolve("PLT-" + caseId + "-" + rowId, "actor").scope());
        try {
            service.resolve("PLT-33333333-3333-4333-8333-333333333333-" + rowId, "actor");
            fail("Wrong owner must be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals("MICROBIOLOGY_LABEL_UNKNOWN", expected.getMessage());
        }
        request.quantity = 4;
        try {
            service.print(request, "actor");
            fail("Preset quantity limit must be enforced");
        } catch (IllegalArgumentException expected) {
            assertEquals("MICROBIOLOGY_LABEL_QUANTITY", expected.getMessage());
        }
        row.setPositiveSource("RECEIVED_ISOLATE");
        assertEquals("MICROBIOLOGY_LABEL_UNKNOWN", assertThrows(IllegalArgumentException.class,
                () -> service.resolve("PLT-" + caseId + "-" + rowId, "actor")).getMessage());
        java.nio.file.Files.write(java.nio.file.Path.of("target/m7-container-label.pdf"), pdf);
    }
}
