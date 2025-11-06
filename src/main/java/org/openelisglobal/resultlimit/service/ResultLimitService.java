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
