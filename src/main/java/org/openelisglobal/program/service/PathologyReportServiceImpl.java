package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologyReportForm;
import org.openelisglobal.program.dao.PathologyReportDAO;
import org.openelisglobal.program.util.PathologyCaseRules;
import org.openelisglobal.program.valueholder.pathology.PathologyReport;
import org.openelisglobal.program.valueholder.pathology.PathologyReport.PathologyReportType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.ReportView;
import org.openelisglobal.program.valueholder.pathology.StageDirection;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PathologyReportServiceImpl implements PathologyReportService {

    /** The unique index on (case, version), the last guard on the number. */
    private static final String VERSION_INDEX = "pathology_report_version_uk";

    /** The stored type a generated PDF carries, as an uploaded PDF is stored. */
    private static final String PDF_TYPE = "data:application/pdf";

    /** The stored types a file is served as, and the extension it is named with. */
    private static final Map<String, String> SERVED_TYPES = Map.of(PDF_TYPE, "pdf", "data:image/png", "png",
            "data:image/jpeg", "jpg");

    @Autowired
    private PathologyReportDAO pathologyReportDAO;

    // Field injection, as the case service uses it: the two services need each
    // other.
    @Autowired
    private PathologySampleService pathologySampleService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private PathologyReportRenderer pathologyReportRenderer;

    @Override
    @Transactional
    public void lockCase(Integer pathologySampleId) {
        if (pathologyReportDAO.lockCase(pathologySampleId) == null) {
            throw new ObjectNotFoundException(pathologySampleId, PathologySample.class.getName());
        }
    }

    @Override
    @Transactional
    public List<ReportView> listFor(Integer pathologySampleId) {
        List<PathologyReport> rows = pathologyReportDAO.getListedForCase(pathologySampleId);
        Integer newestGenerated = newestGeneratedVersion(rows);
        return rows.stream().map(row -> ReportView.of(row, stateOf(row, newestGenerated))).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean canGenerate(PathologySample pathologySample) {
        return PathologyCaseRules.hasDescriptions(pathologySample) && PathologyCaseRules.hasConclusion(pathologySample);
    }

    @Override
    @Transactional
    public ReportView generate(Integer pathologySampleId, String sysUserId, ReportPaths paths) {
        lockCase(pathologySampleId);
        PathologySample storedCase = pathologySampleService.get(pathologySampleId);
        if (!canGenerate(storedCase)) {
            throw new LocalizedValidationException("pathology.error.reportNeedsFindings", "pathology case "
                    + pathologySampleId + " needs both descriptions and a conclusion before a report is generated");
        }
        PathologyReportType type = pathologySampleService.isSignedOut(storedCase) ? PathologyReportType.FINAL
                : PathologyReportType.DRAFT;
        boolean amended = type == PathologyReportType.FINAL && wasReopened(pathologySampleId);
        int version = pathologyReportDAO.nextVersion(pathologySampleId);
        byte[] bytes = pathologyReportRenderer.render(pathologySampleId, sysUserId, paths, amended);
        PathologyReport row = newRow(pathologySampleId, bytes, PDF_TYPE, type, version, sysUserId);
        store(row);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_REPORT_GENERATED);
        payload.put("version", row.getVersionNumber());
        payload.put("type", type.name());
        recordEvent(row, IActionConstants.AUDIT_TRAIL_INSERT, payload, sysUserId);
        return viewOf(row);
    }

    @Override
    @Transactional
    public ReportView upload(Integer pathologySampleId, PathologyReportForm upload, String sysUserId) {
        lockCase(pathologySampleId);
        if (upload == null || (upload.getImage() == null && StringUtils.isBlank(upload.getBase64Image()))) {
            // The attachment rule lets a body with no bytes through, as a stored slide
            // is posted without its image; an upload must carry a file.
            throw new InvalidClinicalAttachmentException();
        }
        ClinicalAttachmentService.validate(upload);
        // The body is the entity itself, so it could name its own version, type,
        // voided flag or author: only the file and its type are taken from it.
        PathologyReport row = newRow(pathologySampleId, upload.getImage(), upload.getFileType(),
                PathologyReportType.EXTERNAL, pathologyReportDAO.nextVersion(pathologySampleId), sysUserId);
        store(row);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_REPORT_UPLOADED);
        payload.put("version", row.getVersionNumber());
        payload.put("fileType", row.getFileType());
        recordEvent(row, IActionConstants.AUDIT_TRAIL_INSERT, payload, sysUserId);
        return viewOf(row);
    }

    @Override
    @Transactional
    public ReportFile fileOf(Integer pathologySampleId, Integer reportId) {
        PathologyReport row = servedRow(pathologySampleId, reportId);
        String accessionNumber = pathologySampleService.get(pathologySampleId).getSample().getAccessionNumber();
        return new ReportFile(row.getImage(), StringUtils.removeStart(row.getFileType(), "data:"),
                accessionNumber + "-v" + row.getVersionNumber() + "." + SERVED_TYPES.get(row.getFileType()));
    }

    @Override
    @Transactional
    public ReportView withdraw(Integer pathologySampleId, Integer reportId, String sysUserId) {
        lockCase(pathologySampleId);
        PathologyReport row = servedRow(pathologySampleId, reportId);
        if (row.getReportType() != PathologyReportType.EXTERNAL) {
            throw new LocalizedValidationException("pathology.error.reportNotWithdrawable",
                    "report " + reportId + " was generated from the case, so it cannot be withdrawn");
        }
        if (row.isVoided()) {
            throw new PathologyReportConflictException("report " + reportId + " is already withdrawn");
        }
        row.setVoided(true);
        pathologyReportDAO.update(row);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PATHOLOGY_REPORT_WITHDRAWN);
        payload.put("version", row.getVersionNumber());
        recordEvent(row, IActionConstants.AUDIT_TRAIL_UPDATE, payload, sysUserId);
        return viewOf(row);
    }

    @Override
    @Transactional
    public Optional<Integer> voidFinalInForce(Integer pathologySampleId, String sysUserId) {
        List<PathologyReport> rows = pathologyReportDAO.getListedForCase(pathologySampleId);
        Integer newestGenerated = newestGeneratedVersion(rows);
        // The final in force is the newest generated row while it is an unvoided
        // final; an older final already reads superseded and is left as it is.
        Optional<Integer> inForce = rows.stream()
                .filter(row -> row.getReportType() == PathologyReportType.FINAL
                        && ReportView.CURRENT.equals(stateOf(row, newestGenerated)))
                .map(PathologyReport::getId).findFirst();
        inForce.flatMap(reportId -> pathologyReportDAO.getOnCase(pathologySampleId, reportId)).ifPresent(row -> {
            row.setVoided(true);
            pathologyReportDAO.update(row);
        });
        return inForce;
    }

    // helpers

    /**
     * A row of this case that holds a file of a served type; anything else is the
     * case's not found.
     */
    private PathologyReport servedRow(Integer pathologySampleId, Integer reportId) {
        return pathologyReportDAO.getOnCase(pathologySampleId, reportId)
                .filter(row -> row.getImage() != null && SERVED_TYPES.containsKey(row.getFileType()))
                .orElseThrow(() -> new ObjectNotFoundException(pathologySampleId, PathologySample.class.getName()));
    }

    private PathologyReport newRow(Integer pathologySampleId, byte[] bytes, String fileType, PathologyReportType type,
            int version, String sysUserId) {
        PathologyReport row = new PathologyReport();
        row.setPathologySampleId(pathologySampleId);
        row.setImage(bytes);
        row.setFileType(fileType);
        row.setReportType(type);
        row.setVersionNumber(version);
        row.setVoided(false);
        row.setGeneratedAt(new Timestamp(System.currentTimeMillis()));
        row.setGeneratedBy(systemUserService.get(sysUserId));
        row.setSysUserId(sysUserId);
        return row;
    }

    /**
     * Inserts the row and flushes it here, where a lost race on the number, which
     * the unique index answers only at the flush, becomes a conflict.
     */
    private void store(PathologyReport row) {
        try {
            pathologyReportDAO.insert(row);
        } catch (RuntimeException e) {
            if (namesTheVersionIndex(e)) {
                throw new PathologyReportConflictException(
                        "version " + row.getVersionNumber() + " of the case's report was stored at the same moment", e);
            }
            throw e;
        }
    }

    private static boolean namesTheVersionIndex(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && StringUtils.containsIgnoreCase(violation.getConstraintName(), VERSION_INDEX)) {
                return true;
            }
            if (StringUtils.containsIgnoreCase(cause.getMessage(), VERSION_INDEX)) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    private boolean wasReopened(Integer pathologySampleId) {
        return pathologySampleService.getStageEvents(pathologySampleId).stream()
                .anyMatch(event -> event.getDirection() == StageDirection.REOPEN);
    }

    private void recordEvent(PathologyReport row, String activity, Map<String, Object> payload, String sysUserId) {
        pathologySampleService.recordRowEvent(REPORT_REFERENCE_TABLE, row.getStringId(), activity, payload, sysUserId);
    }

    /** The row's view, its state worked out against the case's other rows. */
    private ReportView viewOf(PathologyReport row) {
        List<PathologyReport> rows = pathologyReportDAO.getListedForCase(row.getPathologySampleId());
        return ReportView.of(row, stateOf(row, newestGeneratedVersion(rows)));
    }

    private static Integer newestGeneratedVersion(List<PathologyReport> rows) {
        return rows.stream().filter(PathologyReportServiceImpl::isGenerated).map(PathologyReport::getVersionNumber)
                .filter(version -> version != null).max(Integer::compare).orElse(null);
    }

    /**
     * Voided when the flag is set; a generated row older than the newest generated
     * row, a voided one included, is superseded; anything else is current. An
     * upload is its own document and is never superseded.
     */
    private static String stateOf(PathologyReport row, Integer newestGenerated) {
        if (row.isVoided()) {
            return ReportView.VOIDED;
        }
        if (isGenerated(row) && newestGenerated != null && row.getVersionNumber() != null
                && row.getVersionNumber() < newestGenerated) {
            return ReportView.SUPERSEDED;
        }
        return ReportView.CURRENT;
    }

    private static boolean isGenerated(PathologyReport row) {
        return row.getReportType() == PathologyReportType.DRAFT || row.getReportType() == PathologyReportType.FINAL;
    }
}
