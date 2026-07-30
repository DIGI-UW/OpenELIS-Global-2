package org.openelisglobal.login.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.form.ChangePasswordLoginForm;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.validator.ChangePasswordLoginFormValidator;
import org.openelisglobal.login.validator.LoginValidator;
import org.openelisglobal.login.valueholder.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChangePasswordLoginController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "loginName", "password", "newPassword",
            "confirmPassword" };

    @Autowired
    private ChangePasswordLoginFormValidator formValidator;
    @Autowired
    private LoginValidator loginValidator;
    @Autowired
    private LoginUserService loginService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/ChangePasswordLogin", method = RequestMethod.GET)
    public ModelAndView showChangePasswordLogin(HttpServletRequest request) {
        ChangePasswordLoginForm form = new ChangePasswordLoginForm();
        form.setFormAction("ChangePasswordLogin");
        return findForward(FWD_SUCCESS, form);
    }

    @RequestMapping(value = "/ChangePasswordLogin", method = RequestMethod.POST)
    public ModelAndView showUpdateLoginChangePassword(@ModelAttribute("form") @Valid ChangePasswordLoginForm form,
            BindingResult result, RedirectAttributes redirectAttributes, HttpServletRequest request,
            HttpServletResponse response)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        // SPA clients pass apiCall=true to receive an explicit JSON status instead of
        // the legacy JSP forward/redirect, which is indistinguishable from a security
        // bounce to /LoginPage (same pattern as /ValidateLogin)
        boolean apiCall = "true".equals(request.getParameter("apiCall"));
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            return apiCall ? writeApiResponse(response, result) : findForward(FWD_FAIL_INSERT, form);
        }

        // Login newLogin = new Login();
        // // populate valueholder from form
        // PropertyUtils.copyProperties(newLogin, form);
        try {
            LoginUser login;
            // get user information if password correct
            Optional<LoginUser> matchedLogin = loginService.getValidatedLogin(form.getLoginName(), form.getPassword());
            if (!matchedLogin.isPresent()) {
                result.reject("login.error.password.current.incorrect");
            } else {
                login = matchedLogin.get();
                // update fields of login before validating again
                loginService.hashPassword(login, form.getNewPassword());
                Errors loginResult = new BeanPropertyBindingResult(login, "loginInfo");
                loginValidator.unauthenticatedPasswordUpdateValidate(login, loginResult);

                if (loginResult.hasErrors()) {
                    saveErrors(loginResult);
                    return apiCall ? writeApiResponse(response, loginResult) : findForward(FWD_FAIL_INSERT, form);
                }
                // The change-password-before-login flow has no SecurityContext
                // (user has no session yet), but we've just validated their
                // credentials. Set a transient SecurityContext to the user
                // themselves so the audit row is attributed to the user changing
                // their own password rather than the update failing for lack of
                // a user context.
                SecurityContext previous = SecurityContextHolder.getContext();
                try {
                    SecurityContext authCtx = SecurityContextHolder.createEmptyContext();
                    authCtx.setAuthentication(new UsernamePasswordAuthenticationToken(login.getLoginName(), null,
                            AuthorityUtils.NO_AUTHORITIES));
                    SecurityContextHolder.setContext(authCtx);
                    loginService.update(login);
                } finally {
                    SecurityContextHolder.setContext(previous);
                }
            }

        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            result.reject("login.error.update.message");
        }
        if (result.hasErrors()) {
            saveErrors(result);
            return apiCall ? writeApiResponse(response, result) : findForward(FWD_FAIL_INSERT, form);
        }

        if (apiCall) {
            return writeApiResponse(response, null);
        }
        redirectAttributes.addFlashAttribute(Constants.SUCCESS_MSG,
                MessageUtil.getMessage("login.success.changePass.message"));
        return findForward(FWD_SUCCESS_INSERT, form);
    }

    /**
     * Writes the change-password outcome directly to the response as JSON: 200
     * {"success": true} when errors is null, otherwise 401 {"error": "<message
     * id>"}. Returns null so Spring MVC skips view resolution (a JSP forward here
     * would be redirected to /LoginPage by the security chain for unauthenticated
     * users, masking the outcome).
     */
    private ModelAndView writeApiResponse(HttpServletResponse response, Errors errors) {
        JSONObject json = new JSONObject();
        if (errors == null) {
            response.setStatus(HttpStatus.SC_OK);
            json.put("success", true);
        } else {
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            json.put("error", errors.getAllErrors().stream().findFirst().map(ObjectError::getCode)
                    .orElse("login.error.update.message"));
        }
        response.setContentType("application/json");
        try {
            response.getWriter().print(json);
        } catch (IOException e) {
            LogEvent.logError(e);
        }
        return null;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "loginChangePasswordDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/LoginPage";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "loginChangePasswordDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "login.changePass";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "login.changePass";
    }
}
