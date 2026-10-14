package org.openelisglobal.microbiology.service;

import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;
import org.openelisglobal.labelpreset.service.LabelPresetService;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroContainerLabelForm;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroContainerLabelServiceImpl implements MicroContainerLabelService {
    private static final Pattern CODE = Pattern.compile("^(PLT|ISO|AST)-([a-fA-F0-9-]{36})-([a-fA-F0-9-]{36})$");
    private final MicroCaseInoculationDAO cultures;
    private final MicroIsolateDAO isolates;
    private final MicroAstRunDAO runs;
    private final MicroCaseWorkspaceService workspace;
    private final LabelPresetService presets;

    public MicroContainerLabelServiceImpl(MicroCaseInoculationDAO cultures, MicroIsolateDAO isolates,
            MicroAstRunDAO runs, MicroCaseWorkspaceService workspace, LabelPresetService presets) {
        this.cultures = cultures;
        this.isolates = isolates;
        this.runs = runs;
        this.workspace = workspace;
        this.presets = presets;
    }

    public java.util.List<MicroContainerLabelForm.Preset> presets() {
        return presets.list(true, null).stream().filter(p -> Boolean.TRUE.equals(p.getPrintsPerContainer()))
                .map(p -> new MicroContainerLabelForm.Preset(p.getId(), p.getName(), p.getDefaultPerContainer(),
                        p.getMaxPerContainer()))
                .toList();
    }

    public MicroContainerLabelForm.Target resolve(String code, String actor) {
        if (actor == null || actor.isBlank())
            throw new AccessDeniedException("Authenticated user required");
        var match = CODE.matcher(code == null ? "" : code);
        if (!match.matches())
            throw new IllegalArgumentException("MICROBIOLOGY_LABEL_UNKNOWN");
        String kind = match.group(1), caseId = match.group(2), rowId = match.group(3), owner;
        switch (kind) {
        case "PLT" -> {
            var row = cultures.get(rowId).orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_LABEL_UNKNOWN"));
            if (row.getCancelledAt() != null)
                throw new IllegalArgumentException("MICROBIOLOGY_LABEL_UNKNOWN");
            owner = row.getCaseId();
        }
        case "ISO" -> owner = isolates.get(rowId)
                .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_LABEL_UNKNOWN")).getCaseId();
        case "AST" -> owner = isolates.get(runs.get(rowId)
                .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_LABEL_UNKNOWN")).getIsolateId())
                .orElseThrow().getCaseId();
        default -> throw new IllegalArgumentException("MICROBIOLOGY_LABEL_UNKNOWN");
        }
        if (!caseId.equals(owner))
            throw new IllegalArgumentException("MICROBIOLOGY_LABEL_UNKNOWN");
        return new MicroContainerLabelForm.Target(caseId, rowId,
                "PLT".equals(kind) ? "CULTURE" : "ISO".equals(kind) ? "ISOLATES" : "AST");
    }

    public byte[] print(MicroContainerLabelForm request, String actor) {
        String code = request.kind + "-" + request.caseId + "-" + request.rowId;
        resolve(code, actor);
        var c = workspace.get(request.caseId, actor);
        if (!c.canWrite)
            throw new AccessDeniedException("Case lab unit Results rights required");
        var preset = request.presetId == null ? null : presets.get(request.presetId);
        if (preset == null || !Boolean.TRUE.equals(preset.getIsActive())
                || !Boolean.TRUE.equals(preset.getPrintsPerContainer()) || request.quantity < 1
                || request.quantity > preset.getMaxPerContainer())
            throw new IllegalArgumentException("MICROBIOLOGY_LABEL_QUANTITY");
        try {
            var out = new ByteArrayOutputStream();
            float width = preset.getWidthMm() * 72f / 25.4f, height = preset.getHeightMm() * 72f / 25.4f;
            var doc = new Document(new Rectangle(width, height), 3, 3, 3, 3);
            var writer = PdfWriter.getInstance(doc, out);
            doc.open();
            for (int n = 0; n < request.quantity; n++) {
                if (n > 0)
                    doc.newPage();
                var format = switch (preset.getBarcodeType()) {
                case CODE_128 -> com.google.zxing.BarcodeFormat.CODE_128;
                case QR -> com.google.zxing.BarcodeFormat.QR_CODE;
                case DATAMATRIX -> com.google.zxing.BarcodeFormat.DATA_MATRIX;
                };
                boolean linear = format == com.google.zxing.BarcodeFormat.CODE_128;
                var matrix = new com.google.zxing.MultiFormatWriter().encode(code, format, 600, linear ? 200 : 600);
                var imageBytes = new ByteArrayOutputStream();
                com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(matrix, "PNG", imageBytes);
                var image = Image.getInstance(imageBytes.toByteArray());
                float symbolSize = Math.min(height - 6, width * .45f);
                image.scaleToFit(linear ? width - 6 : symbolSize, linear ? height / 2 : symbolSize);
                image.setAbsolutePosition(3, 3);
                writer.getDirectContent().addImage(image);
                var text = new StringBuilder(c.accessionNumber).append("\n").append(request.kind);
                for (var field : preset.getFields()) {
                    var key = org.openelisglobal.labelpreset.valueholder.LabelFieldKey.fromKey(field.getFieldKey());
                    if (key == null || key == org.openelisglobal.labelpreset.valueholder.LabelFieldKey.LAB_NUMBER)
                        continue;
                    String value = switch (key) {
                    case PATIENT_NAME -> c.patientName;
                    case PATIENT_ID -> c.patientId;
                    case PATIENT_DOB -> c.birthDate == null ? null
                            : org.openelisglobal.common.util.DateUtil.convertTimestampToStringDateAndTime(c.birthDate);
                    case PATIENT_SEX -> c.gender;
                    case CASE_NUMBER -> c.id;
                    case SPECIMEN_TYPE -> c.specimenType;
                    default -> null;
                    };
                    text.append("\n")
                            .append(org.openelisglobal.internationalization.MessageUtil.getMessage(key.getMessageKey()))
                            .append(": ").append(value == null || value.isBlank() ? "________" : value);
                }
                float left = linear ? 3 : symbolSize + 6, bottom = linear ? height / 2 + 6 : 3;
                boolean fitted = false;
                for (int fontSize = 8; fontSize >= 5; fontSize--) {
                    var column = new ColumnText(writer.getDirectContent());
                    column.setSimpleColumn(new Phrase(text.toString(), new Font(Font.HELVETICA, fontSize)), left,
                            bottom, width - 3, height - 3, fontSize + 1, Element.ALIGN_LEFT);
                    if (!ColumnText.hasMoreText(column.go(true))) {
                        column.setText(new Phrase(text.toString(), new Font(Font.HELVETICA, fontSize)));
                        column.setYLine(height - 3);
                        column.go();
                        fitted = true;
                        break;
                    }
                }
                if (!fitted)
                    throw new IllegalArgumentException("MICROBIOLOGY_LABEL_CONTENT_SIZE");

            }
            doc.close();
            return out.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("MICROBIOLOGY_LABEL_PRINT", error);
        }
    }
}
