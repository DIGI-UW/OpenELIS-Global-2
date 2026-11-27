package org.openelisglobal.program.dao.cytology;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;

public interface CytologySampleDAO extends BaseDAO<CytologySample, Integer> {
    List<CytologySample> getWithStatus(List<CytologyStatus> statuses);

    Long getCountWithStatusBetweenDates(List<CytologyStatus> statuses, Timestamp from, Timestamp to);

    List<CytologySample> searchWithStatusAndAccesionNumber(List<CytologyStatus> statuses, String labNumber);

    /**
     * The cases in the statuses whose lab number starts with the term, or, for a
     * term of at least
     * {@link org.openelisglobal.program.dao.CaseSearchTerms#MIN_NAME_PART}
     * characters, whose patient's first or last name holds it, in any letter case.
     */
    List<CytologySample> searchWithStatusAndTerm(List<CytologyStatus> statuses, String term);

    Long getCountWithStatus(List<CytologyStatus> statuses);
}
