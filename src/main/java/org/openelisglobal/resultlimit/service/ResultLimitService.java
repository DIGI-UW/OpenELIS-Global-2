package org.openelisglobal.resultlimit.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResultLimitService extends BaseObjectService<ResultLimit, String> {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<ResultLimit> getAllResultLimits() throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<ResultLimit> getPageOfResultLimits(int startingRecNo) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void getData(ResultLimit resultLimit) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ResultLimit> getAllResultLimitsForTest(String testId) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitById(String resultLimitId) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayAgeRange(ResultLimit resultLimit, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayValidRange(ResultLimit resultLimit, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayReportingRange(ResultLimit resultLimit, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayCriticalRange(ResultLimit resultLimit, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayReferenceRange(ResultLimit resultLimit, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayNormalRange(double low, double high, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForTestAndPatient(String testId, Patient patient);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForTestAndPatient(Test test, Patient patient);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<IdValuePair> getPredefinedAgeRanges();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ResultLimit> getResultLimits(String testId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ResultLimit> getResultLimits(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForAnalysis(Analysis analysis);
}
