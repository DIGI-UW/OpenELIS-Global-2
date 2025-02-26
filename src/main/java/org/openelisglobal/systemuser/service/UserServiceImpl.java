package org.openelisglobal.systemuser.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.openelisglobal.program.service.ProgramService;
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
                List<IdValuePair> allTestSections = DisplayListService.getInstance()
                        .getList(ListType.TEST_SECTION_ACTIVE);
                if (userLabUnits.contains(UnifiedSystemUserController.ALL_LAB_UNITS)) {
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
                            List<IdValuePair> allTestSections = DisplayListService.getInstance()
                                    .getList(ListType.TEST_SECTION_ACTIVE);
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
        LogEvent.logWarn(this.getClass().getSimpleName(), "getUserTestSections",
                "no principal object in spring security context. Could not get tests belonging to user");
        return new ArrayList<>();

    }

    @Override
    public List<IdValuePair> getUserSampleTypes(String systemUserId, String roleName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        List<IdValuePair> testSections = getUserTestSections(systemUserId, resultsRoleId);
        List<Integer> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(Integer.valueOf(testSection.getId())));
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
        List<Integer> testUnitIds = new ArrayList<>();
        if (ObjectUtils.isNotEmpty(testSection)) {
            testSections.forEach(testSection2 -> testUnitIds.add(Integer.valueOf(testSection2.getId())));
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

    @Override
    public List<TestResultItem> filterResultsByLabUnitRoles(String systemUserId, List<TestResultItem> results,
            String roleName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        List<IdValuePair> testSections = getUserTestSections(systemUserId, resultsRoleId);
        List<Integer> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(Integer.valueOf(testSection.getId())));
        }

        List<Test> allTests = testService.getTestsByTestSectionIds(testUnitIds);
        List<String> allTestsIds = new ArrayList<>();
        allTests.forEach(test -> allTestsIds.add(test.getId()));
        return results.stream().filter(result -> allTestsIds.contains(result.getTestId())).collect(Collectors.toList());
    }

    @Override
    public List<IdValuePair> getAllDisplayUserTestsByLabUnit(String SystemUserId, String roleName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        List<IdValuePair> testSections = getUserTestSections(SystemUserId, resultsRoleId);
        List<Integer> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(Integer.valueOf(testSection.getId())));
        }

        List<Test> allTests = testService.getTestsByTestSectionIds(testUnitIds);
        List<String> allTestsIds = new ArrayList<>();
        allTests.forEach(test -> allTestsIds.add(test.getId()));

        List<IdValuePair> allDisplayUserTests = DisplayListService.getInstance()
                .getListWithLeadingBlank(DisplayListService.ListType.ALL_TESTS);
        return allDisplayUserTests.stream().filter(test -> allTestsIds.contains(test.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalysisItem> filterAnalysisResultsByLabUnitRoles(String SystemUserId, List<AnalysisItem> results,
            String roleName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        List<IdValuePair> testSections = getUserTestSections(SystemUserId, resultsRoleId);
        List<Integer> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(Integer.valueOf(testSection.getId())));
        }

        List<Test> allTests = testService.getTestsByTestSectionIds(testUnitIds);
        List<String> allTestsIds = new ArrayList<>();
        allTests.forEach(test -> allTestsIds.add(test.getId()));
        return results.stream().filter(result -> allTestsIds.contains(result.getTestId())).collect(Collectors.toList());
    }

    @Override
    public List<Analysis> filterAnalysesByLabUnitRoles(String SystemUserId, List<Analysis> results, String roleName) {
        String resultsRoleId = roleService.getRoleByName(roleName).getId();
        List<IdValuePair> testSections = getUserTestSections(SystemUserId, resultsRoleId);
        List<Integer> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(testSection -> testUnitIds.add(Integer.valueOf(testSection.getId())));
        }

        List<Test> allTests = testService.getTestsByTestSectionIds(testUnitIds);
        List<String> allTestsIds = new ArrayList<>();
        allTests.forEach(test -> allTestsIds.add(test.getId()));
        return results.stream().filter(result -> allTestsIds.contains(result.getTest().getId()))
                .collect(Collectors.toList());
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
        return allPrograms.stream()
                .filter(p -> programService.get(p.getId()).getTestSection() == null
                        || testUnitIds.contains(programService.get(p.getId()).getTestSection().getId()))
                .collect(Collectors.toList());
    }

}
