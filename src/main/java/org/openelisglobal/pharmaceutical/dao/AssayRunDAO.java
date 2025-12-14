package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pharmaceutical.valueholder.AssayRun;

public interface AssayRunDAO extends BaseDAO<AssayRun, Integer> {

    List<AssayRun> findBySampleId(Integer sampleId);

    List<AssayRun> findByStatus(AssayRun.AssayStatus status);

    List<AssayRun> findByOosFlag(boolean oosFlag);

    List<AssayRun> findPendingReview();

    AssayRun findByNotebookPageId(String notebookPageId);
}
