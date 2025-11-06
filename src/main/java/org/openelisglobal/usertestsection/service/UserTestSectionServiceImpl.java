package org.openelisglobal.usertestsection.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserTestSectionServiceImpl implements UserTestSectionService {

    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private UserModuleService userModuleService;

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getAllUserTestSectionsByName(HttpServletRequest request, String testSectionName)
            throws LIMSRuntimeException {
        List<TestSection> list = new ArrayList<>();

        try {
            if (ConfigurationProperties.getInstance().getPropertyValue("enable.user.test.section")
                    .equals(IActionConstants.NO)) {
                list = testSectionService.getTestSections(testSectionName);
            } else {
                UserSessionData usd = (UserSessionData) request.getSession()
                        .getAttribute(IActionConstants.USER_SESSION_DATA);
                // bugzilla 2160

                if (!userModuleService.isUserAdmin(request)) {
                    list = testSectionService.getTestSectionsBySysUserId(testSectionName, usd.getSystemUserId());
                } else {
                    list = testSectionService.getTestSections(testSectionName);
                }
            }
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserTestSectionServiceImpl getAllUserTestSectionsByName()", e);
        }
        return list;
    }

    // @Override
    // @Transactional(readOnly = true)
    // public List getPageOfTestsBySysUserId(HttpServletRequest request, int
    // startingRecNo, String
    // doingSearch,
    // String searchStr) throws LIMSRuntimeException {
    //
    // List list = new ArrayList();
    // TestService testService = SpringContext.getBean(TestService.class);
    //
    // try {
    // if
    // (ConfigurationProperties.getInstance().getPropertyValue("enable.user.test.section").equals(IActionConstants.NO))
    // {
    //
    // if (!StringUtil.isNullorNill(doingSearch) &&
    // doingSearch.equals(IActionConstants.YES)) {
    // list = testService.getPageOfSearchedTests(startingRecNo, searchStr);
    // } else {
    // list = testService.getPageOfTests(startingRecNo);
    // }
    // } else {
    // UserSessionData usd = (UserSessionData) request.getSession()
    // .getAttribute(IActionConstants.USER_SESSION_DATA);
    // // bugzilla 2160
    // UserModuleService userModuleService =
    // SpringContext.getBean(UserModuleService.class);
    // if (!userModuleService.isUserAdmin(request)) {
    // if (!StringUtil.isNullorNill(doingSearch) &&
    // doingSearch.equals(IActionConstants.YES)) {
    //
    // list = testService.getPageOfSearchedTestsBySysUserId(startingRecNo,
    // usd.getSystemUserId(),
    // searchStr);
    // } else {
    // list = testService.getPageOfTestsBySysUserId(startingRecNo,
    // usd.getSystemUserId());
    // }
    // } else {
    //
    // if (!StringUtil.isNullorNill(searchStr)) {
    // list = testService.getPageOfSearchedTests(startingRecNo, searchStr);
    // } else {
    // list = testService.getPageOfTests(startingRecNo);
    // }
    //
    // }
    // // end if bugzilla 2371
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError(e);
    // throw new LIMSRuntimeException("Error in UserTestSectionServiceImpl
    // getPageOfTestsBySysUserId()", e);
    // }
    // return list;
    // }

    // @Override
    // @Transactional(readOnly = true)
    // public List getAllUserTestSections(HttpServletRequest request) throws
    // LIMSRuntimeException {
    //
    // List list = new ArrayList();
    // try {
    // if
    // (ConfigurationProperties.getInstance().getPropertyValue("enable.user.test.section").equals(IActionConstants.NO))
    // {
    // list = testSectionService.getAllTestSections();
    // } else {
    // UserSessionData usd = (UserSessionData) request.getSession()
    // .getAttribute(IActionConstants.USER_SESSION_DATA);
    // // bugzilla 2160
    // if (!userModuleService.isUserAdmin(request)) {
    // list =
    // testSectionService.getAllTestSectionsBySysUserId(usd.getSystemUserId());
    // } else {
    // list = testSectionService.getAllTestSections();
    // }
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError(e);
    // throw new LIMSRuntimeException("Error in UserTestSectionServiceImpl
    // getAllUserTestSections()", e);
    // }
    // return list;
    // }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllUserTests(HttpServletRequest request, boolean onlyTestsFullySetup)
            throws LIMSRuntimeException {
        List<Test> list = new ArrayList<>();
        TestService testService = SpringContext.getBean(TestService.class);

        try {
            if (ConfigurationProperties.getInstance().getPropertyValue("enable.user.test.section")
                    .equals(IActionConstants.NO)) {
                list = testService.getAllTests(onlyTestsFullySetup);
            } else {
                UserSessionData usd = (UserSessionData) request.getSession()
                        .getAttribute(IActionConstants.USER_SESSION_DATA);
                // bugzilla 2160
                UserModuleService userModuleService = SpringContext.getBean(UserModuleService.class);
                if (!userModuleService.isUserAdmin(request)) {
                    list = testService.getAllTestsBySysUserId(usd.getSystemUserId(), onlyTestsFullySetup);
                } else {
                    list = testService.getAllTests(onlyTestsFullySetup);
                }
            }
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserTestSectionServiceImpl getAllUserTests()", e);
        }
        return list;
    }

    // @Override
    // @Transactional(readOnly = true)
    // public List getSampleTestAnalytes(HttpServletRequest request, List
    // sample_Tas, List testSections)
    // throws LIMSRuntimeException {
    //
    // try {
    // if
    // (ConfigurationProperties.getInstance().getPropertyValue("enable.user.test.section").equals(IActionConstants.NO))
    // {
    // return sample_Tas;
    // } else {
    // // bugzilla 2160
    // UserModuleService userModuleService =
    // SpringContext.getBean(UserModuleService.class);
    // if (!userModuleService.isUserAdmin(request)) {
    // for (int i = 0; i < sample_Tas.size(); i++) {
    // Sample_TestAnalyte sample_ta = (Sample_TestAnalyte) sample_Tas.get(i);
    // Test_TestAnalyte test = sample_ta.getTestTestAnalyte();
    // for (int j = 0; j < testSections.size(); j++) {
    // TestSection testSection = (TestSection) testSections.get(j);
    // if (!test.getTest().getTestSection().getId().equals(testSection.getId())) {
    // if (sample_Tas.size() > 0) {
    // sample_Tas.remove(i);
    // }
    // }
    // }
    // }
    // }
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("UserTestSectionServiceImpl", "getSampleTestAnalytes()",
    // e.toString());
    // throw new LIMSRuntimeException("Error in UserTestSectionServiceImpl
    // getSampleTestAnalytes()", e);
    // }
    // return sample_Tas;
    // }

    // @Override
    // @Transactional(readOnly = true)
    // public List getSamplePdfList(HttpServletRequest request, Locale locale,
    // String sampStatus,
    // String humanDomain)
    // throws LIMSRuntimeException {
    //
    // List samplePdfList = new java.util.ArrayList();
    //
    // try {
    // List statuses = new ArrayList();
    // statuses.add(sampStatus);
    // // bugzilla 2437: made getSamplesByStatusAndDomain more generic to take in
    // list
    // // of statuses of samples to retrieve
    // List sampleList = sampleService.getSamplesByStatusAndDomain(statuses,
    // humanDomain);
    // FileValidationProvider fvp = new FileValidationProvider();
    //
    // String pdfMsg = MessageUtil.getMessage("human.sample.pdf.message");
    // // ResourceLocator.getInstance().getMessageResources().getMessage(locale,
    // // "human.sample.pdf.message");
    //
    // for (int i = 0; i < sampleList.size(); i++) {
    // Sample sam = (Sample) sampleList.get(i);
    // String msg = fvp.validate(sam.getAccessionNumber());
    // LabelValuePair lvp = new LabelValuePair();
    // lvp.setValue(sam.getAccessionNumber());
    // if (msg.equals(IActionConstants.VALID)) {
    // lvp.setLabel(sam.getAccessionNumber() + " - " + pdfMsg);
    // } else {
    // lvp.setLabel(sam.getAccessionNumber());
    // }
    //
    // samplePdfList.add(lvp);
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError(e);
    // throw new LIMSRuntimeException("Error in UserTestSectionServiceImpl
    // getSamplePdfList()", e);
    // }
    // return samplePdfList;
    // }

    // @Override
    // @Transactional(readOnly = true)
    // public List getAnalyses(HttpServletRequest request, List analyses, List
    // testSections) throws
    // LIMSRuntimeException {
    //
    // List newAnalyses = new ArrayList();
    // try {
    // if
    // (ConfigurationProperties.getInstance().getPropertyValue("enable.user.test.section").equals(IActionConstants.NO))
    // {
    // return newAnalyses;
    // } else {
    // UserModuleService userModuleService =
    // SpringContext.getBean(UserModuleService.class);
    // if (!userModuleService.isUserAdmin(request)) {
    // newAnalyses = new ArrayList();
    // for (int i = 0; i < analyses.size(); i++) {
    // org.openelisglobal.analysis.valueholder.Analysis analysis =
    // (org.openelisglobal.analysis.valueholder.Analysis) analyses
    // .get(i);
    // for (int j = 0; j < testSections.size(); j++) {
    // TestSection testSection = (TestSection) testSections.get(j);
    // if (analysis.getTestSection().getId().equals(testSection.getId())) {
    // newAnalyses.add(analysis);
    // }
    // }
    // }
    // } else {
    // return analyses;
    // }
    // }
    //
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError(e);
    // throw new LIMSRuntimeException("Error in UserTestSectionServiceImpl
    // getAnalyses()",
    // e);
    // }
    // return newAnalyses;
    //
    // }

    // @Override
    // @Transactional(readOnly = true)
    // public List getSampleTestAnalytes(HttpServletRequest request, List
    // sample_Tas, List testSections)
    // throws LIMSRuntimeException {
    //
    // try {
    // if
    // (ConfigurationProperties.getInstance().getPropertyValue("enable.user.test.section").equals(IActionConstants.NO))
    // {
    // return sample_Tas;
    // } else {
    // // bugzilla 2160
    // UserModuleService userModuleService =
    // SpringContext.getBean(UserModuleService.class);
    // if (!userModuleService.isUserAdmin(request)) {
    // for (int i = 0; i < sample_Tas.size(); i++) {
    // Sample_TestAnalyte sample_ta = (Sample_TestAnalyte) sample_Tas.get(i);
    // Test_TestAnalyte test = sample_ta.getTestTestAnalyte();
    // for (int j = 0; j < testSections.size(); j++) {
    // TestSection testSection = (TestSection) testSections.get(j);
    // if (!test.getTest().getTestSection().getId().equals(testSection.getId())) {
    // if (sample_Tas.size() > 0) {
    // sample_Tas.remove(i);
    // }
    // }
    // }
    // }
    // }
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("UserTestSectionServiceImpl", "getSampleTestAnalytes()",
    // e.toString());
    // throw new LIMSRuntimeException("Error in UserTestSectionServiceImpl
    // getSampleTestAnalytes()", e);
    // }
    // return sample_Tas;
    // }

}
