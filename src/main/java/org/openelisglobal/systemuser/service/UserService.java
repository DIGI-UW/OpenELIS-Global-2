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

public interface UserService {

    void updateLoginUser(LoginUser loginUser, boolean loginUserNew, SystemUser systemUser, boolean systemUserNew,
            List<String> selectedRoles, String loggedOnUserId);

    void saveUserLabUnitRoles(SystemUser systemUser, Map<String, Set<String>> selectedLabUnitRolesMap,
            String loggedOnUserId);

    UserLabUnitRoles getUserLabUnitRoles(String systemUserId);

    List<UserLabUnitRoles> getAllUserLabUnitRoles();

    List<IdValuePair> getUserTestSections(String systemUserId, String roleId);

    List<IdValuePair> getUserSampleTypes(String systemUserId, String userRole);

    List<IdValuePair> getAllDisplayUserTestsByLabUnit(String SystemUserId, String roleName);

    List<AnalysisItem> filterAnalysisResultsByLabUnitRoles(String SystemUserId, List<AnalysisItem> results,
            String roleName);

    /**
     * Ids of every test in the lab units this user holds the given role for. The
     * predicate behind {@link #filterAnalysesByLabUnitRoles}, exposed separately so
     * a caller that pages or caps its query can push the same scope into SQL
     * instead of discarding rows after the fact.
     */
    List<String> getUserTestIdsForLabUnitRoles(String systemUserId, String roleName);

    List<Analysis> filterAnalysesByLabUnitRoles(String SystemUserId, List<Analysis> results, String roleName);

    List<TestResultItem> filterResultsByLabUnitRoles(String SystemUserId, List<TestResultItem> results,
            String roleName);

    /**
     * The programs this user may order under. Read from the cached display list,
     * which is rebuilt only when a program is saved: a program deleted straight
     * from the database therefore lingers there until the next restart, so an id
     * that no longer resolves to a record is dropped rather than dereferenced.
     */
    List<IdValuePair> getUserPrograms(String systemUserId, String userRole);

    List<IdValuePair> getUserSampleTypes(String systemUserId, String roleName, String testSectionName);
}
