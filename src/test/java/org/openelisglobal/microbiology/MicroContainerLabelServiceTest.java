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
    @Test
    public void labelsUsePresetDimensionsAndQuantityAndResolveOnlyTheirOwner() throws Exception {
        String caseId = "11111111-1111-4111-8111-111111111111", rowId = "22222222-2222-4222-8222-222222222222";
        var cultures = mock(MicroCaseInoculationDAO.class);
        var workspace = mock(MicroCaseWorkspaceService.class);
        var presets = mock(LabelPresetService.class);
        var service = new MicroContainerLabelServiceImpl(cultures, mock(MicroIsolateDAO.class),
                mock(MicroAstRunDAO.class), workspace, presets);
        var row = new MicroCaseInoculation();
        row.setId(rowId);
        row.setCaseId(caseId);
        when(cultures.get(rowId)).thenReturn(Optional.of(row));
        var shell = new MicroCaseShellForm();
        shell.canWrite = true;
        shell.accessionNumber = "LABEL-001";
        shell.id = caseId;
        when(workspace.get(caseId, "actor")).thenReturn(shell);
        var preset = new LabelPreset();
        preset.setId(1);
        preset.setIsActive(true);
        preset.setPrintsPerContainer(true);
        preset.setMaxPerContainer(3);
        preset.setWidthMm(50);
        preset.setHeightMm(25);
        preset.setBarcodeType(BarcodeType.QR);
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
        java.nio.file.Files.write(java.nio.file.Path.of("target/m7-container-label.pdf"), pdf);
    }
}
