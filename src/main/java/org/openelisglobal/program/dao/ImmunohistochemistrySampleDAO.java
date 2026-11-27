package org.openelisglobal.program.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;

public interface ImmunohistochemistrySampleDAO extends BaseDAO<ImmunohistochemistrySample, Integer> {

    List<ImmunohistochemistrySample> getWithStatus(List<ImmunohistochemistryStatus> statuses);

    Long getCountWithStatus(List<ImmunohistochemistryStatus> statuses);

    Long getCountWithStatusBetweenDates(List<ImmunohistochemistryStatus> statuses, Timestamp from, Timestamp to);

    List<ImmunohistochemistrySample> searchWithStatusAndAccesionNumber(List<ImmunohistochemistryStatus> statuses,
            String labNumber);

    /**
     * The cases in the statuses whose lab number starts with the term, or, for a
     * term of at least {@link CaseSearchTerms#MIN_NAME_PART} characters, whose
     * patient's first or last name holds it, in any letter case.
     */
    List<ImmunohistochemistrySample> searchWithStatusAndTerm(List<ImmunohistochemistryStatus> statuses, String term);

    ImmunohistochemistrySample getByPathologySampleId(Integer pathologySampleId);
}
