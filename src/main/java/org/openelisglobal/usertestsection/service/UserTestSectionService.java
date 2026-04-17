package org.openelisglobal.usertestsection.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserTestSectionService {
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestSection> getAllUserTestSectionsByName(HttpServletRequest request, String testSectionName)
            throws LIMSRuntimeException;

    // // bugzilla 2371
    // List<Test> getPageOfTestsBySysUserId(HttpServletRequest request, int
    // startingRecNo, String
    // doingSearch,
    // String searchStr) throws LIMSRuntimeException;

    // List<TestSection> getAllUserTestSections(HttpServletRequest request) throws
    // LIMSRuntimeException;

    // bugzilla 2291 (added boolean onlyTestsFullySetup)
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getAllUserTests(HttpServletRequest request, boolean onlyTestsFullySetup) throws LIMSRuntimeException;

    // public List<Object> getSampleTestAnalytes(HttpServletRequest request,
    // List<Object> sample_Tas,
    // List<Object> testSections) throws LIMSRuntimeException;

    // List<SamplePdf> getSamplePdfList(HttpServletRequest request, Locale locale,
    // String
    // sampStatus, String humanDomain)
    // throws LIMSRuntimeException;

    // bugzilla 2433
    // List<Analysis> getAnalyses(HttpServletRequest request, List<Object> analyses,
    // List<Object>
    // testSections)
    // throws LIMSRuntimeException;
}
