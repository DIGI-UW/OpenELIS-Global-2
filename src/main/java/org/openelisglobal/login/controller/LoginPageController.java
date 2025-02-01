package org.openelisglobal.login.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.login.bean.UserSession;
import org.openelisglobal.login.bean.UserSession.LoginMethod;
import org.openelisglobal.login.form.LoginForm;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginPageController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "loginName", "password" };
    public static final String ALL_LAB_UNITS = "AllLabUnits";

    @Value("${org.itech.login.saml:false}")
    private Boolean useSAML;

    @Value("${org.itech.login.oauth:false}")
    private Boolean useOAUTH;

    private static String authorizationRequestBaseUri = "oauth2/authorization";
    Map<String, String> oauth2AuthenticationUrls = new HashMap<>();

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    SystemUserService systemUserService;
    @Autowired
    UserRoleService userRoleService;
    @Autowired
    RoleService roleService;
    @Autowired
    private UserService userService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    LocalizationService localizationService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/LoginPage", method = RequestMethod.GET)
    public ModelAndView showLoginPage(HttpServletRequest request, Principal principal) {
        if (principal != null) {
            return new ModelAndView(findForward(HOME_PAGE));
        }

        String forward = FWD_SUCCESS;
        LoginForm form = new LoginForm();
        if (useSAML) {
            form.setUseSAML(useSAML);
        }
        if (useOAUTH) {
            ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository).as(Iterable.class);
            if (type != ResolvableType.NONE && ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
                @SuppressWarnings("unchecked")
                Iterable<ClientRegistration> clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
                clientRegistrations.forEach(registration -> oauth2AuthenticationUrls.put(registration.getClientName(),
                        authorizationRequestBaseUri + "/" + registration.getRegistrationId()));
            }
            form.setOauthUrls(oauth2AuthenticationUrls);
        }

        // add flash attributes from other controllers into request
        addFlashMsgsToRequest(request);

        // add error messages from authentication fail controller
        Errors errors = (Errors) request.getSession().getAttribute(Constants.LOGIN_ERRORS);
        if (errors != null) {
            request.setAttribute(Constants.REQUEST_ERRORS, errors);
            request.getSession().removeAttribute(Constants.LOGIN_ERRORS);
        }

        form.setFormAction("ValidateLogin");

        return findForward(forward, form);
    }

    @GetMapping(value = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UserSession getSesssionDetails(HttpServletRequest request, CsrfToken token) {
        boolean authenticated = !userModuleService.isSessionExpired(request);
        UserSession session = new UserSession();
        session.setAuthenticated(authenticated);
        session.setSessionId(request.getSession().getId());
        if (authenticated) {
            SystemUser user = systemUserService.get(getSysUserId(request));
            setLoginMethod(request, session);
            session.setUserId(user.getId());
            session.setLoginName(user.getLoginName());
            session.setFirstName(user.getFirstName());
            session.setLastName(user.getLastName());
            if (token != null) {
                session.setCSRF(token.getToken());
            }
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            if (usd.getLoginLabUnit() != 0) {
                TestSection testSection = testSectionService.getTestSectionById(String.valueOf(usd.getLoginLabUnit()));
                if (testSection != null) {
                    session.setLoginLabUnit(testSection.getLocalizedName());
                }
            }
            setLabunitRolesForExistingUser(request, session);
        }
        return session;
    }

    private void setLoginMethod(HttpServletRequest request, UserSession session) {
        if (Boolean.TRUE.equals(request.getSession().getAttribute("samlSession"))) {
            session.setLoginMethod(LoginMethod.SAML);
        } else if (Boolean.TRUE.equals(request.getSession().getAttribute("oauthSession"))) {
            session.setLoginMethod(LoginMethod.OAUTH);
        } else {
            session.setLoginMethod(LoginMethod.FORM);
        }
    }

    private void setLabunitRolesForExistingUser(HttpServletRequest request, UserSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                setLabunitRolesForExistingUserFromDB(session);
                Set<String> roles = new HashSet<>();
                for (String roleId : userRoleService.getRoleIdsForUser(session.getUserId())) {
                    roles.add(roleService.getRoleById(roleId).getName().trim());
                }
                session.setRoles(roles);
            } else if (principal instanceof DefaultSaml2AuthenticatedPrincipal) {
                setLabunitRolesForExistingUserFromGrantedAuthorities(session, authentication);
            } else if (principal instanceof DefaultOAuth2User) {
                setLabunitRolesForExistingUserFromGrantedAuthorities(session, authentication);
            }
        }
    }

    private void setLabunitRolesForExistingUserFromGrantedAuthorities(UserSession session,
            Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Map<String, List<String>> userLabRolesMap = new HashMap<>();
        Set<String> roles = new HashSet<>();
        for (GrantedAuthority authority : authorities) {
            String[] authorityExplode = authority.getAuthority().split("-");
            if (authorityExplode.length == 2) {
                roles.add(authorityExplode[1]);
            } else if (authorityExplode.length == 3) {
                List<String> userLabRoles = userLabRolesMap.getOrDefault(authorityExplode[2], new ArrayList<>());
                userLabRoles.add(authorityExplode[1]);
                roles.add(authorityExplode[1]);
                userLabRolesMap.put(authorityExplode[2], userLabRoles);
            }
        }
        session.setRoles(roles);
        session.setUserLabRolesMap(userLabRolesMap);
    }

    @PostMapping(value = "/rest/setUserLoginLabUnit/{labUnitId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void setUserLoginLabUnit(@PathVariable String labUnitId) {
        if (ConfigurationProperties.getInstance().getPropertyValue(Property.REQUIRE_LAB_UNIT_AT_LOGIN).equals("true")) {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            Integer loginLabUnit = Integer.valueOf(labUnitId);
            TestSection testSection = testSectionService.getTestSectionById(labUnitId);
            if (testSection != null) {
                usd.setLoginLabUnit(loginLabUnit);
            }
            request.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
            request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        }
    }

    private void setLabunitRolesForExistingUserFromDB(UserSession session) {
        UserLabUnitRoles roles = userService.getUserLabUnitRoles(session.getUserId());
        if (roles != null) {
            Set<LabUnitRoleMap> roleMaps = roles.getLabUnitRoleMap();
            List<String> userLabUnits = new ArrayList<>();
            roleMaps.forEach(map -> userLabUnits.add(map.getLabUnit()));
            Map<String, List<String>> userLabRolesMap = new HashMap<>();
            if (userLabUnits.contains(ALL_LAB_UNITS)) {
                roleMaps.stream().filter(map -> map.getLabUnit().equals(ALL_LAB_UNITS))
                        .forEach(map -> userLabRolesMap.put(map.getLabUnit(), map.getRoles().stream()
                                .map(r -> roleService.getRoleById(r).getName().trim()).collect(Collectors.toList())));
            } else {
                for (LabUnitRoleMap map : roleMaps) {
                    userLabRolesMap.put(testSectionService.get(map.getLabUnit()).getLocalizedName(),
                            map.getRoles().stream().map(r -> roleService.getRoleById(r).getName().trim())
                                    .collect(Collectors.toList()));
                }
            }

            session.setUserLabRolesMap(userLabRolesMap);
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "loginPageDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "login.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "login.subTitle";
    }
}
