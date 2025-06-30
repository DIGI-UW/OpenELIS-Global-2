package org.openelisglobal.login.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
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
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
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
            BindingResult result, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        // Login newLogin = new Login();
        // // populate valueholder from form
        // PropertyUtils.copyProperties(newLogin, form);
        try {
            LoginUser login;
            // get user information if password correct
            Optional<LoginUser> matchedLogin = loginService.getValidatedLogin(form.getLoginName(), form.getPassword());
            if (!matchedLogin.isPresent()) {
                result.reject("login.error.message");
            } else {
                login = matchedLogin.get();
                // update fields of login before validating again
                loginService.hashPassword(login, form.getNewPassword());
                Errors loginResult = new BeanPropertyBindingResult(login, "loginInfo");
                loginValidator.unauthenticatedPasswordUpdateValidate(login, loginResult);

                if (loginResult.hasErrors()) {
                    saveErrors(loginResult);
                    return findForward(FWD_FAIL_INSERT, form);
                }
                loginService.update(login);
            }

        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            result.reject("login.error.message");
        }
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        redirectAttributes.addFlashAttribute(Constants.SUCCESS_MSG,
                MessageUtil.getMessage("login.success.changePass.message"));
        return findForward(FWD_SUCCESS_INSERT, form);
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
