package org.openelisglobal.systemuser.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.service.ProgramPickerRules;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private LoginUserService loginService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private ProgramService programService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private HttpSession session;

    @Override
    @Transactional
    public void updateLoginUser(LoginUser loginUser, boolean loginUserNew, SystemUser systemUser, boolean systemUserNew,
            List<String> selectedRoles, String loggedOnUserId) {
        if (loginUserNew) {
            loginService.insert(loginUser);
        } else {
            loginService.update(loginUser);
        }

        if (systemUserNew) {
            systemUserService.insert(systemUser);
        } else {
            systemUserService.update(systemUser);
        }

        updateUserRoles(selectedRoles, systemUser, loggedOnUserId, false);
    }

    @Override
    @Transactional
    public void saveUserLabUnitRoles(SystemUser systemUser, Map<String, Set<String>> selectedLabUnitRolesMap,
            String loggedOnUserId) {
        UserLabUnitRoles userLabUnitRoles = userRoleService.getUserLabUnitRoles(systemUser.getId());
        Set<LabUnitRoleMap> labUnitRoleMaps;
        if (userLabUnitRoles == null) {
            userLabUnitRoles = new UserLabUnitRoles();
            userLabUnitRoles.setId(Integer.valueOf(systemUser.getId()));
            labUnitRoleMaps = new HashSet<>();
        } else {
            labUnitRoleMaps = userLabUnitRoles.getLabUnitRoleMap();
            for (LabUnitRoleMap roleMap : labUnitRoleMaps) {
                userRoleService.deleteLabUnitRoleMap(roleMap);
            }
            labUnitRoleMaps.clear();
        }
        Set<String> labUnitRoles = new HashSet<>();
        for (String labUnit : selectedLabUnitRolesMap.keySet()) {
            if (StringUtils.isNotEmpty(labUnit)) {
                LabUnitRoleMap labUnitRoleMap = new LabUnitRoleMap();
                labUnitRoleMap.setLabUnit(labUnit);
                labUnitRoleMap.setRoles(selectedLabUnitRolesMap.get(labUnit));
                labUnitRoleMaps.add(labUnitRoleMap);
                for (String role : selectedLabUnitRolesMap.get(labUnit)) {
                    labUnitRoles.add(role);
                }
            }
        }
        userLabUnitRoles.setLabUnitRoleMap(labUnitRoleMaps);
        userRoleService.saveOrUpdateUserLabUnitRoles(userLabUnitRoles);
        updateUserRoles(labUnitRoles.stream().collect(Collectors.toList()), systemUser, loggedOnUserId, true);
    }

    @Override
    @Transactional
    public UserLabUnitRoles getUserLabUnitRoles(String systemUserId) {
        return userRoleService.getUserLabUnitRoles(systemUserId);
    }

    @Override
    @Transactional
    public List<UserLabUnitRoles> getAllUserLabUnitRoles() {
        return userRoleService.getAllUserLabUnitRoles();
    }

    private void updateUserRoles(List<String> selectedRoles, SystemUser systemUser, String loggedOnUserId,
            Boolean isLabRole) {
        if (selectedRoles == null) {
            selectedRoles = new ArrayList<>();
        }
        List<String> currentUserRoles = userRoleService.getRoleIdsForUser(systemUser.getId());
        List<UserRole> deletedUserRoles = new ArrayList<>();
        if (isLabRole) {
            for (String role : currentUserRoles) {
                selectedRoles.add(role);
            }
        }

        for (int i = 0; i < selectedRoles.size(); i++) {
            if (!currentUserRoles.contains(selectedRoles.get(i))) {
                UserRole userRole = new UserRole();
                userRole.setSystemUserId(systemUser.getId());
                userRole.setRoleId(selectedRoles.get(i));
                userRole.setSysUserId(loggedOnUserId);
                userRoleService.insert(userRole);
            } else {
                currentUserRoles.remove(selectedRoles.get(i));
            }
        }

        for (String roleId : currentUserRoles) {
            UserRole userRole = new UserRole();
            userRole.setSystemUserId(systemUser.getId());
            userRole.setRoleId(roleId);
            userRole.setSysUserId(loggedOnUserId);
            deletedUserRoles.add(userRole);
        }

        if (deletedUserRoles.size() > 0) {
            userRoleService.deleteAll(deletedUserRoles);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdValuePair> getUserViewerTestSections(String systemUserId, String roleId) {
        // Start from the authorized active set, so this can only ever add units
        // the user is already entitled to see — never widen authorization.
        List<IdValuePair> active = getUserTestSections(systemUserId, roleId);
        // "hasContent" counts EVERY analysis, not just in-flight ones. Counting
        // only pending work meant a finalized result in a deactivated unit
        // vanished from the results pages and from reporting the instant it was
        // entered — the guardrail covers viewing historical data too, not only
        // completing pending work.
        Set<String> pendingSectionIds = analysisService.getTestSectionIdsWithAnyAnalyses();
        if (pendingSectionIds.isEmpty()) {
            return active;
        }
        Set<String> alreadyListed = active.stream().map(IdValuePair::getId).collect(Collectors.toSet());

        // Which inactive units may this user see? Re-derive from their lab-unit
        // roles rather than trusting the caller: an admin (or ALL_LAB_UNITS)
        // sees every one, anyone else only their assigned units.
        String adminRoleId = roleService.getRoleByName(Constants.ROLE_GLOBAL_ADMIN).getId();
        boolean isAdmin = userRoleService.getRoleIdsForUser(systemUserId).contains(adminRoleId);
        List<String> userLabUnits = new ArrayList<>();
        UserLabUnitRoles userLabRoles = getUserLabUnitRoles(systemUserId);
        if (userLabRoles != null) {
            userLabRoles.getLabUnitRoleMap().forEach(roles -> {
                if (roleId == null || roles.getRoles().contains(roleId)) {
                    userLabUnits.add(roles.getLabUnit());
                }
            });
        }
        boolean allLabUnits = isAdmin || userLabUnits.contains(UnifiedSystemUserController.ALL_LAB_UNITS);

        List<IdValuePair> result = new ArrayList<>(active);
        for (IdValuePair inactive : DisplayListService.getInstance().getList(ListType.TEST_SECTION_INACTIVE)) {
            // Only inactive units that still hold in-flight work come back, so
            // an emptied-then-deactivated unit disappears immediately while one
            // switched off mid-run stays reachable until that run finishes.
            if (!alreadyListed.contains(inactive.getId()) && pendingSectionIds.contains(inactive.getId())
                    && (allLabUnits || userLabUnits.contains(inactive.getId()))) {
                result.add(inactive);
            }
        }
        return result;
    }

    @Override
    public List<IdValuePair> getUserTestSections(String systemUserId, String roleId) {
        Authentication authentication = null;
        // see filter org.openelisglobal.security.AjaxFilter to handle
        // RequestContextHolder for Ajax calls via servlets
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        HttpServletRequest request = null;
        if (requestAttributes instanceof ServletRequestAttributes) {
            request = ((ServletRequestAttributes) requestAttributes).getRequest();

            Object sc = request.getSession()
                    .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (!(sc instanceof SecurityContext)) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getUserLogin",
                        "security context is not of type SecurityContext");
            } else {
                authentication = ((SecurityContext) sc).getAuthentication();
            }
        } else {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getUserLogin",
                    "requestAttributes is not of type ServletRequestAttributes");
        }
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                List<IdValuePair> userTestSections = new ArrayList<>();
                Boolean requireLabUnitAtLogin = ConfigurationProperties.getInstance()
                        .getPropertyValue(Property.REQUIRE_LAB_UNIT_AT_LOGIN).equals("true");
                UserSessionData usd = (UserSessionData) session.getAttribute("userSessionData");
                String adminRoleId = roleService.getRoleByName(Constants.ROLE_GLOBAL_ADMIN).getId();
                Boolean isadmin = userRoleService.getRoleIdsForUser(systemUserId).contains(adminRoleId);
                TestSection logintestSection = null;
                if (requireLabUnitAtLogin && !isadmin) {
                    if (usd.getLoginLabUnit() != 0) {
                        logintestSection = testSectionService.getTestSectionById(String.valueOf(usd.getLoginLabUnit()));
                        if (logintestSection != null) {
                            userTestSections.add(
                                    new IdValuePair(logintestSection.getId(), logintestSection.getLocalizedName()));
                            return userTestSections;
                        }
                    }

                }

                List<String> userLabUnits = new ArrayList<>();
                UserLabUnitRoles userLabRoles = getUserLabUnitRoles(systemUserId);
                if (userLabRoles != null) {
                    userLabRoles.getLabUnitRoleMap().forEach(roles -> {
                        if (roleId == null) {
                            userLabUnits.add(roles.getLabUnit());
                        } else {
                            if (roles.getRoles().contains(roleId)) {
                                userLabUnits.add(roles.getLabUnit());
                            }
                        }

                    });
                }
                List<IdValuePair> allTestSections = activeTestSections();
                if (isadmin || userLabUnits.contains(UnifiedSystemUserController.ALL_LAB_UNITS)) {
                    return allTestSections;
                } else {
                    userTestSections = allTestSections.stream()
                            .filter(testSection -> userLabUnits.contains(testSection.getId()))
                            .collect(Collectors.toList());
                    return userTestSections;
                }
            } else if (principal instanceof DefaultSaml2AuthenticatedPrincipal
                    || principal instanceof DefaultOAuth2User) {
                List<IdValuePair> testSections = new ArrayList<>();

                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String[] authorityExplode = authority.getAuthority().split("-");
                    if (authorityExplode.length == 3) {
                        if (roleId == null || roleService.get(roleId).getName().trim().equals(authorityExplode[1])) {
                            List<IdValuePair> allTestSections = activeTestSections();
                            if (UnifiedSystemUserController.ALL_LAB_UNITS.equals(authorityExplode[2])) {
                                return allTestSections;
                            } else {
                                List<IdValuePair> userTestSections = allTestSections.stream()
                                        .filter(testSection -> testSection.getValue().equals(authorityExplode[2]))
                                        .collect(Collectors.toList());
                                testSections.addAll(userTestSections);
                            }

                        }
                    }
                }
                return testSections;
            }
        }

        return new ArrayList<>();
    }

    private List<IdValuePair> activeTestSections() {
        List<IdValuePair> cached = DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        return testSectionService.getAllActiveTestSections().stream()
                .map(section -> new IdValuePair(section.getId(), section.getLocalizedName())).toList();
    }

    @Override
    public List<IdValuePair> getUserSampleTypes(String systemUserId, String roleName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        List<IdValuePair> testSections = getUserTestSections(systemUserId, resultsRoleId);
        List<String> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(testSection.getId()));
        }

        List<Test> allTests = testService.getTestsByTestSectionIds(testUnitIds);
        Set<String> sampleIds = new HashSet<>();
        // clear cache to create a fresh Map of testId To TypeOfSample
        List<IdValuePair> userSampleTypes = new ArrayList<>();
        if (allTests != null) {
            typeOfSampleService.clearCache();
            allTests.forEach(test -> {
                List<TypeOfSample> sampleTypes = typeOfSampleService.getTypeOfSampleForTest(test.getId());
                if (sampleTypes != null) {
                    sampleIds.addAll(sampleTypes.stream().map(e -> e.getId()).collect(Collectors.toList()));
                }
            });
        }

        sampleIds.forEach(id -> {
            TypeOfSample type = typeOfSampleService.get(id);
            if (type != null) {
                userSampleTypes.add(new IdValuePair(type.getId(), type.getLocalizedName()));
            }
        });

        return userSampleTypes;
    }

    @Override
    public List<IdValuePair> getUserSampleTypes(String systemUserId, String roleName, String testSectionName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        List<IdValuePair> testSections = getUserTestSections(systemUserId, resultsRoleId);
        TestSection testSection = testSectionService.getTestSectionByName(testSectionName);
        // List<String> testUnitIds = new ArrayList<>();
        List<String> testUnitIds = new ArrayList<>();
        if (ObjectUtils.isNotEmpty(testSection)) {
            testSections.forEach(testSection2 -> testUnitIds.add(testSection2.getId()));
            // testUnitIds=
            // testSections.stream().filter(el->el.getId().equals(testSection.getId())).map(e->e.getId()).collect(Collectors.toList());
        }
        List<Test> allTests = testService.getTestsByTestSectionIds(testUnitIds);
        // List<String> allTBTestIds =
        // typeOfSampleService.getAllActiveTestsByTestUnit(true,
        // testUnitIds).stream().map(e->e.getId()).collect(Collectors.toList());
        // List<IdValuePair> allSampleTypes =
        // DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_ACTIVE);
        Set<String> sampleIds = new HashSet<>();

        List<IdValuePair> userSampleTypes = new ArrayList<>();
        if (allTests != null) {
            typeOfSampleService.clearCache();
            allTests.forEach(test -> sampleIds.addAll(typeOfSampleService.getTypeOfSampleForTest(test.getId()).stream()
                    .map(e -> e.getId()).collect(Collectors.toList())));
        }

        sampleIds.forEach(id -> {
            TypeOfSample type = typeOfSampleService.get(id);
            if (type != null) {
                userSampleTypes.add(new IdValuePair(type.getId(), type.getLocalizedName()));
            }
        });

        // clear cache to create a fresh Map of testId To TypeOfSample
        // typeOfSampleService.clearCache();

        // List<String> allSampleTypesIds =
        // DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_ACTIVE).stream().map(e->e.getId()).collect(Collectors.toList());
        // allSampleTypesIds.forEach(sid->{
        // List<String> testIds = typeOfSampleService.getActiveTestsBySampleTypeId(sid,
        // false).stream().map(e->e.getId()).collect(Collectors.toList());
        // if(allTBTestIds.stream().anyMatch(testIds::contains)) {
        // sampleIds.add(sid);
        // }
        // });

        // List<IdValuePair> userSampleTypes = allSampleTypes.stream().filter(type ->
        // sampleIds.contains(type.getId()))
        // .collect(Collectors.toList());
        return userSampleTypes;
    }

    /**
     * OGC-189: whether the analysis behind a result row belongs to one of
     * {@code allowedUnitIds}, judged by the analysis's OWN lab unit.
     *
     * <p>
     * The row's test may be configured for a different unit than the analysis was
     * filed under — a reflexed analysis inherits the parent's unit — so matching
     * via the test hid reflexed work that was sitting in a perfectly active unit.
     * Falls back to the test's configured unit when the analysis cannot be
     * resolved, so a row is never dropped for lack of information.
     */
    private boolean analysisIsInAllowedUnit(String analysisId, String testId, Set<String> allowedUnitIds) {
        if (!GenericValidator.isBlankOrNull(analysisId)) {
            try {
                Analysis analysis = analysisService.get(analysisId);
                if (analysis != null && analysis.getTestSection() != null
                        && analysis.getTestSection().getId() != null) {
                    return allowedUnitIds.contains(analysis.getTestSection().getId());
                }
            } catch (RuntimeException e) {
                // Unresolvable id — fall through to the test's configured unit.
            }
        }
        if (GenericValidator.isBlankOrNull(testId)) {
            return true;
        }
        Test test = testService.getTestById(testId);
        if (test == null || test.getTestSection() == null || test.getTestSection().getId() == null) {
            return true;
        }
        return allowedUnitIds.contains(test.getTestSection().getId());
    }

    @Override
    public List<TestResultItem> filterResultsByLabUnitRoles(String systemUserId, List<TestResultItem> results,
            String roleName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        // OGC-189 (M2): viewer semantics — this filters results the lab has
        // already started, across 21 call sites (workplan, logbook results,
        // status results, patient results, accession lookup, patient reports).
        // With the active-only set, deactivating a lab unit made every pending
        // result in it vanish from all of them and become uncompletable.
        List<IdValuePair> testSections = getUserViewerTestSections(systemUserId, resultsRoleId);
        List<String> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(testSection.getId()));
        }
        org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "filterResultsByLabUnitRoles",
                "User " + systemUserId + " has " + (testSections != null ? testSections.size() : 0) + " test sections: "
                        + testUnitIds);

        Set<String> allowedUnitIds = new HashSet<>(testUnitIds);
        List<TestResultItem> allowed = results.stream()
                .filter(r -> analysisIsInAllowedUnit(r.getAnalysisId(), r.getTestId(), allowedUnitIds))
                .collect(Collectors.toList());
        org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "filterResultsByLabUnitRoles",
                "Input results: " + results.size() + ", allowed units: " + allowedUnitIds.size()
                        + ", Filtered results: " + allowed.size());
        return allowed;
    }

    @Override
    public List<IdValuePair> getAllDisplayUserTestsByLabUnit(String SystemUserId, String roleName) {
        Set<String> allTestsIds = getTestIdsInUserLabUnits(SystemUserId, roleName);
        List<IdValuePair> allDisplayUserTests = DisplayListService.getInstance()
                .getListWithLeadingBlank(DisplayListService.ListType.ALL_TESTS);
        return allDisplayUserTests.stream().filter(test -> allTestsIds.contains(test.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalysisItem> filterAnalysisResultsByLabUnitRoles(String SystemUserId, List<AnalysisItem> results,
            String roleName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        // OGC-189 (M2): a VIEWER over work that already exists, so it must use
        // the isActive-OR-hasContent set. getUserTestSections returns active
        // units only, which silently dropped every pending analysis whose lab
        // unit had since been switched off — the dashboard counted the work but
        // the page came back empty, and it could no longer be completed.
        List<IdValuePair> testSections = getUserViewerTestSections(SystemUserId, resultsRoleId);
        List<String> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(testSection.getId()));
        }

        // Same as the TestResultItem variant: judge by the analysis's own unit.
        Set<String> allowedUnitIds = new HashSet<>(testUnitIds);
        return results.stream().filter(r -> analysisIsInAllowedUnit(r.getAnalysisId(), r.getTestId(), allowedUnitIds))
                .collect(Collectors.toList());
    }

    @Override
    public List<Analysis> filterAnalysesByLabUnitRoles(String SystemUserId, List<Analysis> results, String roleName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        // OGC-189 (M2): viewer semantics — see the note on
        // filterAnalysisResultsByLabUnitRoles above. Completion of existing
        // work is never gated on the lab unit's status.
        List<IdValuePair> testSections = getUserViewerTestSections(SystemUserId, resultsRoleId);
        List<String> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(testSection.getId()));
        }

        // OGC-189: judge each analysis by ITS OWN lab unit, not by the unit its
        // test is configured for. A reflexed analysis is filed under the parent's
        // unit, so the two differ: matching via the test made a reflexed analysis
        // sitting in an active unit invisible because its test is configured
        // elsewhere (a deactivated unit).
        Set<String> allowedUnitIds = new HashSet<>(testUnitIds);
        return results.stream().filter(analysis -> {
            TestSection section = analysis.getTestSection();
            // No section recorded: fall back to the test's configured unit
            // rather than dropping the row.
            if (section == null || section.getId() == null) {
                Test test = analysis.getTest();
                return test == null || test.getTestSection() == null
                        || allowedUnitIds.contains(test.getTestSection().getId());
            }
            return allowedUnitIds.contains(section.getId());
        }).collect(Collectors.toList());
    }

    /**
     * The tests a user may work on in a role, as a list.
     *
     * <p>
     * A view over {@link #getTestIdsInUserLabUnits(String, String)}, so the batch
     * workplan shares the one read the rest of the request already paid for.
     */
    @Override
    public List<String> getUserTestIdsForLabUnitRoles(String systemUserId, String roleName) {
        return new ArrayList<>(getTestIdsInUserLabUnits(systemUserId, roleName));
    }

    /**
     * The tests a user may work on in a role, read once per request.
     *
     * <p>
     * Answering this reads every test in the user's lab units, so callers that ask
     * repeatedly — a report walking a patient's samples, a notification walking the
     * users who hold a role — used to read the whole catalogue once per iteration.
     * A patient with a few hundred samples made their report take minutes and the
     * browser gave up on it. Lab-unit assignments cannot change within a request,
     * so the answer is cached in the request and the reads collapse to one.
     *
     * <p>
     * The returned set is shared with the other callers in the same request and is
     * not modifiable.
     */
    @Override
    public Set<String> getTestIdsInUserLabUnits(String systemUserId, String roleName) {
        String cacheKey = "userLabUnitTestIds:" + systemUserId + ":" + roleName;
        Set<String> cached = testIdsCachedInRequest(cacheKey);
        if (cached != null) {
            return cached;
        }
        String roleId = roleService.getRoleByName(roleName).getId();
        List<IdValuePair> testSections = getUserTestSections(systemUserId, roleId);
        List<String> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(testSection.getId()));
        }
        Set<String> testIds = testIdsInSections(testUnitIds);
        cacheTestIdsInRequest(cacheKey, testIds);
        return testIds;
    }

    /**
     * The active tests of the given sections, also cached per request: users who
     * share lab units share this answer, so a notification that walks every holder
     * of a role reads the catalogue once rather than once per user.
     */
    private Set<String> testIdsInSections(List<String> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return Collections.emptySet();
        }
        String cacheKey = "labUnitSectionTestIds:" + sectionIds.stream().sorted().collect(Collectors.joining(","));
        Set<String> cached = testIdsCachedInRequest(cacheKey);
        if (cached != null) {
            return cached;
        }
        Set<String> testIds = new LinkedHashSet<>();
        testService.getTestsByTestSectionIds(sectionIds).forEach(test -> testIds.add(test.getId()));
        Set<String> shared = Collections.unmodifiableSet(testIds);
        cacheTestIdsInRequest(cacheKey, shared);
        return shared;
    }

    @SuppressWarnings("unchecked")
    private Set<String> testIdsCachedInRequest(String cacheKey) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object cached = attributes.getAttribute(cacheKey, RequestAttributes.SCOPE_REQUEST);
        return cached instanceof Set ? (Set<String>) cached : null;
    }

    private void cacheTestIdsInRequest(String cacheKey, Set<String> testIds) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(cacheKey, testIds, RequestAttributes.SCOPE_REQUEST);
        }
    }

    @Override
    public boolean hasAllLabUnits(String systemUserId, String roleName) {
        String roleId = roleService.getRoleByName(roleName).getId();
        List<IdValuePair> testSections = getUserTestSections(systemUserId, roleId);
        return testSections != null && !testSections.isEmpty() && testSections.size() == activeTestSections().size();
    }

    @Override
    public List<IdValuePair> getUserPrograms(String systemUserId, String userRole) {
        String resultsRoleId = roleService.getRoleByName(userRole).getId();
        List<IdValuePair> testSections = getUserTestSections(systemUserId, resultsRoleId);
        List<String> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(testSection.getId()));
        }

        List<IdValuePair> allPrograms = DisplayListService.getInstance().getList(ListType.PROGRAM);
        List<IdValuePair> userPrograms = new ArrayList<>();
        for (IdValuePair pair : allPrograms) {
            Optional<Program> program = programService.getMatch("id", pair.getId());
            if (program.isEmpty()) {
                continue;
            }
            if (ProgramPickerRules.servesAnyLabUnit(program.get(), testUnitIds)) {
                userPrograms.add(pair);
            }
        }
        return userPrograms;
    }

}
