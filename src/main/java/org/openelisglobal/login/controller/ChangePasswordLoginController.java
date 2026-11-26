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

    private static final String[] ALLOWED_FIELDS = new String[] {
            "loginName", "password", "newPassword", "confirmPassword"
    };

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
    public ModelAndView showUpdateLoginChangePassword(
            @ModelAttribute("form") @Valid ChangePasswordLoginForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // SPA clients pass apiCall=true to receive an explicit JSON status.
        boolean apiCall = "true".equals(request.getParameter("apiCall"));

        formValidator.validate(form, result);

        if (result.hasErrors()) {
            saveErrors(result);
            return apiCall ? writeApiResponse(response, result) : findForward(FWD_FAIL_INSERT, form);
        }

        try {
            LoginUser login;

            // Verify the current password.
            Optional<LoginUser> matchedLogin =
                    loginService.getValidatedLogin(form.getLoginName(), form.getPassword());

            if (!matchedLogin.isPresent()) {
                result.reject("login.error.password.current.incorrect");

            } else if (form.getPassword().equals(form.getNewPassword())) {

                // Reject reuse of the current password.
                result.reject("login.error.password.reuse");

            } else {
                login = matchedLogin.get();

                // Update fields of login before validating again.
                loginService.hashPassword(login, form.getNewPassword());

                Errors loginResult = new BeanPropertyBindingResult(login, "loginInfo");
                loginValidator.unauthenticatedPasswordUpdateValidate(login, loginResult);

                if (loginResult.hasErrors()) {
                    saveErrors(loginResult);
                    return apiCall ? writeApiResponse(response, loginResult)
                            : findForward(FWD_FAIL_INSERT, form);
                }

                // Set a transient SecurityContext for audit attribution.
                SecurityContext previous = SecurityContextHolder.getContext();

                try {
                    SecurityContext authCtx = SecurityContextHolder.createEmptyContext();
                    authCtx.setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    login.getLoginName(),
                                    null,
                                    AuthorityUtils.NO_AUTHORITIES));

                    SecurityContextHolder.setContext(authCtx);
                    loginService.update(login);

                } finally {
                    SecurityContextHolder.setContext(previous);
                }
            }

        } catch (LIMSRuntimeException e) {
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

        redirectAttributes.addFlashAttribute(
                Constants.SUCCESS_MSG,
                MessageUtil.getMessage("login.success.changePass.message"));

        return findForward(FWD_SUCCESS_INSERT, form);
    }

    /**
     * Writes the change-password outcome directly as JSON.
     * Returns HTTP 200 for success and HTTP 401 for errors.
     */
    private ModelAndView writeApiResponse(HttpServletResponse response, Errors errors) {
        JSONObject json = new JSONObject();

        if (errors == null) {
            response.setStatus(HttpStatus.SC_OK);
            json.put("success", true);

        } else {
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            json.put(
                    "error",
                    errors.getAllErrors().stream()
                            .findFirst()
                            .map(ObjectError::getCode)
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
