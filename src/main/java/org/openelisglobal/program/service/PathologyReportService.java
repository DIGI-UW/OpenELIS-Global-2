package org.openelisglobal.program.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologyReportForm;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.ReportView;

/**
 * The report versions of a pathology case: generated from the stored case,
 * uploaded from outside, withdrawn, and read. Each write first locks the case's
 * row, so writes on one case, a reopen among them, run one after another.
 */
public interface PathologyReportService {

    /** Audit verb recorded on the report row when a version is generated. */
    String PATHOLOGY_REPORT_GENERATED = "PATHOLOGY_REPORT_GENERATED";

    /** Audit verb recorded on the report row when a file is uploaded. */
    String PATHOLOGY_REPORT_UPLOADED = "PATHOLOGY_REPORT_UPLOADED";

    /** Audit verb recorded on the report row when an upload is withdrawn. */
    String PATHOLOGY_REPORT_WITHDRAWN = "PATHOLOGY_REPORT_WITHDRAWN";

    /** reference_tables row the report rows' audit entries point at. */
    String REPORT_REFERENCE_TABLE = "PATHOLOGY_REPORT";

    /** A stored file as it is served. */
    record ReportFile(byte[] bytes, String mediaType, String fileName) {
    }

    /**
     * Takes a write lock on the case's row until the transaction ends, without
     * raising the case's version.
     *
     * @throws org.hibernate.ObjectNotFoundException when no case carries the id
     */
    void lockCase(Integer pathologySampleId);

    /** The rows that hold a file, newest first, each with its state. */
    List<ReportView> listFor(Integer pathologySampleId);

    /**
     * Whether the stored case can have a report: both descriptions written and a
     * conclusion, coded or a narrative.
     */
    boolean canGenerate(PathologySample pathologySample);

    /**
     * Renders the stored case and stores it as the next version: a final on a
     * signed-out case, a draft otherwise.
     */
    ReportView generate(Integer pathologySampleId, String sysUserId, ReportPaths paths);

    /**
     * Stores an outside file as the next version. Only the file and its type are
     * taken from the body.
     */
    ReportView upload(Integer pathologySampleId, PathologyReportForm upload, String sysUserId);

    /**
     * The stored file of a row of this case, a voided one included.
     *
     * @throws org.hibernate.ObjectNotFoundException when the row is not this
     *                                               case's, holds no file, or holds
     *                                               a type that is not served
     */
    ReportFile fileOf(Integer pathologySampleId, Integer reportId);

    /** Voids a current uploaded row; the row is kept. */
    ReportView withdraw(Integer pathologySampleId, Integer reportId, String sysUserId);

    /**
     * Voids the newest final that is not voided yet, in the caller's transaction,
     * and answers its id. Writes no audit entry: the caller's own names it.
     */
    Optional<Integer> voidFinalInForce(Integer pathologySampleId, String sysUserId);
}
