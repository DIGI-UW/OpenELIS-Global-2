package org.openelisglobal.resultlimit.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.valueholder.Test;

public interface ResultLimitService extends BaseObjectService<ResultLimit, String> {

    List<ResultLimit> getAllResultLimits() throws LIMSRuntimeException;

    List<ResultLimit> getPageOfResultLimits(int startingRecNo) throws LIMSRuntimeException;

    void getData(ResultLimit resultLimit) throws LIMSRuntimeException;

    List<ResultLimit> getAllResultLimitsForTest(String testId) throws LIMSRuntimeException;

    /** OGC-949 M7: reference ranges scoped to a result component. */
    List<ResultLimit> getResultLimitsByComponentId(String componentId);

    /**
     * OGC-949 M7: atomically replace a test's reference ranges with {@code desired}
     * (diff-save). Rows with an id that still exist are updated in place; rows
     * without an id are inserted (numeric result-type FK resolved here); existing
     * rows absent from {@code desired} are deleted. Runs in one transaction so a
     * partial failure rolls the whole set back.
     */
    void saveRangesForTest(String testId, List<ResultLimit> desired, String sysUserId);

    ResultLimit getResultLimitById(String resultLimitId) throws LIMSRuntimeException;

    String getDisplayAgeRange(ResultLimit resultLimit, String separator);

    String getDisplayValidRange(ResultLimit resultLimit, String significantDigits, String separator);

    String getDisplayReportingRange(ResultLimit resultLimit, String significantDigits, String separator);

    String getDisplayCriticalRange(ResultLimit resultLimit, String significantDigits, String separator);

    String getDisplayReferenceRange(ResultLimit resultLimit, String significantDigits, String separator);

    String getDisplayNormalRange(double low, double high, String significantDigits, String separator);

    ResultLimit getResultLimitForTestAndPatient(String testId, Patient patient);

    ResultLimit getResultLimitForTestAndPatient(Test test, Patient patient);

    List<IdValuePair> getPredefinedAgeRanges();

    List<ResultLimit> getResultLimits(String testId);

    List<ResultLimit> getResultLimits(Test test);

    ResultLimit getResultLimitForAnalysis(Analysis analysis);
}
