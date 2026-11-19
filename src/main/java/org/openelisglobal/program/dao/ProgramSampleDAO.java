package org.openelisglobal.program.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.AnatomicPathologyBench;
import org.openelisglobal.program.valueholder.PriorCase;
import org.openelisglobal.program.valueholder.ProgramSample;

public interface ProgramSampleDAO extends BaseDAO<ProgramSample, Integer> {
    ProgramSample getProgrammeSampleBySample(Integer sampleId, String programName);

    List<ProgramSample> getProgramSamplesByAccessionNumberOrProgramName(String filter);

    // Programs V2: read-time historical order count for the deactivate modal.
    long countByProgramId(String programId);

    /** The case of that bench with that id; empty when that bench has none. */
    Optional<ProgramSample> getCase(AnatomicPathologyBench bench, Integer id);

    /**
     * Every pathology, immunohistochemistry and cytology case whose order belongs
     * to the patient of the given order, that order's own cases included.
     */
    List<PriorCase> getCasesSharingPatientWith(String sampleId);

    /**
     * The conclusion rows of the pathology cases as {case id, type, value}, by case
     * and then in stored order.
     */
    List<Object[]> getPathologyConclusions(Collection<Integer> caseIds);

    /**
     * Each cytology case's diagnosis and specimen adequacy satisfaction, as {case
     * id, diagnosis, satisfaction}; either is null when the case has none.
     */
    List<Object[]> getCytologyDiagnoses(Collection<Integer> caseIds);
}
