package org.openelisglobal.qachecklist.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qachecklist.valueholder.SampleQaChecklist;

public interface SampleQaChecklistDAO extends BaseDAO<SampleQaChecklist, Integer> {

    /**
     * Find QA checklist by sample ID.
     *
     * @param sampleId the sample ID
     * @return the QA checklist or null if not found
     */
    SampleQaChecklist findBySampleId(Integer sampleId);

    /**
     * Find QA checklist by sample ID (String version for compatibility).
     *
     * @param sampleId the sample ID as string
     * @return the QA checklist or null if not found
     */
    SampleQaChecklist findBySampleId(String sampleId);
}
