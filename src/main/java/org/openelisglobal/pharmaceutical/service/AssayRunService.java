package org.openelisglobal.pharmaceutical.service;

import java.util.List;
import org.openelisglobal.pharmaceutical.valueholder.AssayRun;

public interface AssayRunService {

    AssayRun get(Integer id);

    List<AssayRun> getAll();

    AssayRun save(AssayRun assayRun);

    AssayRun update(AssayRun assayRun);

    void delete(Integer id);

    List<AssayRun> findBySampleId(Integer sampleId);

    List<AssayRun> findByStatus(AssayRun.AssayStatus status);

    List<AssayRun> findByOosFlag(Boolean oosFlag);

    List<AssayRun> findPendingReview();

    List<AssayRun> findByNotebookPageId(Integer notebookPageId);

    AssayRun initiateAssayRun(Integer sampleId, AssayRun assayRun, String userId);

    AssayRun recordAssayResults(Integer assayRunId, String rawResults, String calculatedResults,
            Boolean oosFlag, String userId);

    AssayRun submitForReview(Integer assayRunId, String userId);

    AssayRun approveAssayRun(Integer assayRunId, String approverId);

    AssayRun rejectAssayRun(Integer assayRunId, String rejectionReason, String approverId);

    AssayRun linkToNotebook(Integer assayRunId, Integer notebookPageId, String userId);

    boolean canApprove(Integer assayRunId);
}
