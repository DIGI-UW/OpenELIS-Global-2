package org.openelisglobal.systemuser.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserService {

    @PreAuthorize("hasAuthority('PRIV_USER_MANAGE')")
    void updateLoginUser(LoginUser loginUser, boolean loginUserNew, SystemUser systemUser, boolean systemUserNew,
            List<String> selectedRoles, String loggedOnUserId);

    @PreAuthorize("hasAuthority('PRIV_USER_MANAGE')")
    void saveUserLabUnitRoles(SystemUser systemUser, Map<String, Set<String>> selectedLabUnitRolesMap,
            String loggedOnUserId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    UserLabUnitRoles getUserLabUnitRoles(String systemUserId);

    @PreAuthorize("hasAuthority('PRIV_USER_MANAGE')")
    List<UserLabUnitRoles> getAllUserLabUnitRoles();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<IdValuePair> getUserTestSections(String systemUserId, String roleId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<IdValuePair> getUserSampleTypes(String systemUserId, String userRole);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<IdValuePair> getAllDisplayUserTestsByLabUnit(String SystemUserId, String roleName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<AnalysisItem> filterAnalysisResultsByLabUnitRoles(String SystemUserId, List<AnalysisItem> results,
            String roleName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Analysis> filterAnalysesByLabUnitRoles(String SystemUserId, List<Analysis> results, String roleName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestResultItem> filterResultsByLabUnitRoles(String SystemUserId, List<TestResultItem> results,
            String roleName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<IdValuePair> getUserPrograms(String systemUserId, String userRole);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<IdValuePair> getUserSampleTypes(String systemUserId, String roleName, String testSectionName);
}
