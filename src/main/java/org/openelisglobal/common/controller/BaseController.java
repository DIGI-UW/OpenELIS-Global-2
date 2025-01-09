package org.openelisglobal.common.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

@Component
public abstract class BaseController extends ControllerUtills implements IActionConstants {

    // Request being autowired appears to be threadsafe because of how Spring
    // handles autowiring, despite all controllers being singletons
    // However this is still not best practice and it would be better to rely on
    // Spring's dependency injection into methods for accessing the request
    @Autowired
    protected HttpServletRequest request;

    @Autowired
    protected UserModuleService userModuleService;
    @Autowired
    protected PageBuilderService pageBuilderService;

    protected abstract String findLocalForward(String forward);

    /**
     * Must be implemented by subclasses to set the title for the requested page.
     * The value returned should be a key String from the
     * ApplicationResources.properties file.
     *
     * @return the title key for this page.
     */
    protected abstract String getPageTitleKey();

    /**
     * Must be implemented by subclasses to set the subtitle for the requested page.
     * The value returned should be a key String from the
     * ApplicationResources.properties file.
     *
     * @return the subtitle key this page.
     */
    protected abstract String getPageSubtitleKey();

    /**
     * This getPageTitleKey method accepts a request and form parameter so that a
     * subclass can override the method and conditionally return different titles.
     *
     * @param request the request
     * @param form    the form associated with this request.
     * @return the title key for this page.
     */
    protected String getPageTitleKey(HttpServletRequest request, BaseForm form) {
        return getPageTitleKey();
    }

    protected String getPageTitleKeyParameter(HttpServletRequest request, BaseForm form) {
        return null;
    }

    /**
     * This getSubtitleKey method accepts a request and form parameter so that a
     * subclass can override the method and conditionally return different
     * subtitles.
     *
     * @param request the request
     * @param form    the form associated with this request.
     * @return the subtitle key this page.
     */
    protected String getPageSubtitleKey(HttpServletRequest request, BaseForm form) {
        return getPageSubtitleKey();
    }

    protected String getPageSubtitleKeyParameter(HttpServletRequest request, BaseForm form) {
        return null;
    }

    /**
     * Template method to allow subclasses to handle special cases. The default is
     * to return the message
     *
     * @param message The message
     * @return The message
     */
    protected String getActualMessage(String message) {
        return message;
    }

    /**
     * Utility method to simplify the lookup of MessageResource Strings in the
     * ApplicationResources.properties file for this application.
     *
     * @param messageKey the message key to look up
     */
    protected String getMessageForKey(String messageKey) {
        String message = MessageUtil.getContextualMessage(messageKey);
        return MessageUtil.messageNotFound(message, messageKey) ? getActualMessage(messageKey) : message;
    }

    /**
     * Utility method to simplify the lookup of MessageResource Strings in the
     * ApplicationResources.properties file for this application.
     *
     * @param request    the HttpServletRequest
     * @param messageKey the message key to look up
     */
    protected String getMessageForKey(HttpServletRequest request, String messageKey) {
        if (null == messageKey) {
            return null;
        }
        String message = MessageUtil.getMessage(messageKey);
        return MessageUtil.messageNotFound(message, messageKey) ? getActualMessage(messageKey) : message;
        // return ResourceLocator.getInstance().getMessageResources().getMessage(locale,
        // messageKey);
    }

    protected String getMessageForKey(HttpServletRequest request, String messageKey, String arg0) {
        if (null == messageKey) {
            return null;
        }
        String message = MessageUtil.getMessage(messageKey);
        return MessageUtil.messageNotFound(message, messageKey) ? getActualMessage(messageKey) : message;
        // return ResourceLocator.getInstance().getMessageResources().getMessage(locale,
        // messageKey, arg0);
    }

    protected void setPageTitles(HttpServletRequest request, BaseForm form) {

        String pageSubtitle = null;
        String pageTitle = null;
        String pageTitleKey = getPageTitleKey(request, form);
        String pageSubtitleKey = getPageSubtitleKey(request, form);

        String pageTitleKeyParameter = getPageTitleKeyParameter(request, form);
        String pageSubtitleKeyParameter = getPageSubtitleKeyParameter(request, form);

        try {
            if (StringUtil.isNullorNill(pageTitleKeyParameter)) {
                pageTitle = getMessageForKey(request, pageTitleKey);
            } else {
                pageTitle = getMessageForKey(request, pageTitleKey, pageTitleKeyParameter);
            }
        } catch (RuntimeException e) {
            LogEvent.logError("could not get message for key: " + pageTitleKey, e);
        }

        try {
            if (StringUtil.isNullorNill(pageSubtitleKeyParameter)) {
                pageSubtitle = getMessageForKey(request, pageSubtitleKey);
            } else {
                pageSubtitle = getMessageForKey(request, pageSubtitleKey, pageSubtitleKeyParameter);
            }

        } catch (RuntimeException e) {
            LogEvent.logError("could not get message for key: " + pageSubtitleKey, e);
        }

        if (null != pageTitle) {
            request.setAttribute(PAGE_TITLE_KEY, pageTitle);
        }
        if (null != pageSubtitle) {
            request.setAttribute(PAGE_SUBTITLE_KEY, pageSubtitle);
        }
    }

    protected void setSuccessFlag(HttpServletRequest request, boolean success) {
        request.setAttribute(FWD_SUCCESS, success);
    }

    protected void setSuccessFlag(HttpServletRequest request) {
        request.setAttribute(FWD_SUCCESS, Boolean.TRUE);
    }

    protected boolean userHasPermissionForModule(HttpServletRequest request, String module) {
        if (!userModuleService.isUserAdmin(request) && ConfigurationProperties.getInstance()
                .getPropertyValue("permissions.agent").equalsIgnoreCase("ROLE")) {
            @SuppressWarnings("rawtypes")
            HashSet accessMap = (HashSet) request.getSession().getAttribute(IActionConstants.PERMITTED_ACTIONS_MAP);
            return accessMap.contains(module);
        }

        return true;
    }

    protected String findForward(String forward) {
        if (LOGIN_PAGE.equals(forward)) {
            return "redirect:LoginPage";
        }
        if (HOME_PAGE.equals(forward)) {
            return "redirect:Home";
        }
        String forwardView = findLocalForward(forward);

        if (GenericValidator.isBlankOrNull(forwardView)) {
            forwardView = "PageNotFound";
        }

        return forwardView;
    }

    protected ModelAndView findForward(String forward, BaseForm form) {
        String realForward = findForward(forward);
        if (realForward.startsWith("redirect:")) {
            return new ModelAndView(realForward);
        } else {
            setPageTitles(request, form);
            // insert global forwards here
            return new ModelAndView(pageBuilderService.setupJSPPage(realForward, request), "form", form);
        }
    }

    protected ModelAndView findForward(String forward, Map<String, Object> requestObjects, BaseForm form) {
        ModelAndView mv = findForward(forward, form);
        mv.addAllObjects(requestObjects);
        return mv;
    }

    protected ModelAndView getForwardWithParameters(ModelAndView mv, Map<String, String> params) {
        mv.addAllObjects(params);
        return mv;
    }

    protected void saveErrors(Errors errors) {
        Errors previousErrors = (Errors) request.getAttribute(Constants.REQUEST_ERRORS);
        if (previousErrors == null) {
            request.setAttribute(Constants.REQUEST_ERRORS, errors);
        } else if (previousErrors.hasErrors() && previousErrors.getObjectName().equals(errors.getObjectName())) {
            previousErrors.addAllErrors(errors);
        }
    }

    public void writeErrorsToResponse(BindingResult result, HttpServletResponse response) {
        ServletOutputStream servletOutputStream;
        try {
            servletOutputStream = response.getOutputStream();
            StringBuilder errorMsgBuilder = new StringBuilder();
            for (ObjectError error : result.getGlobalErrors()) {
                errorMsgBuilder.append(MessageUtil.getMessageOrDefault(error.getCode(), error.getArguments(),
                        error.getDefaultMessage()));
                errorMsgBuilder.append(System.lineSeparator());
            }
            for (FieldError error : result.getFieldErrors()) {
                errorMsgBuilder.append(error.getField() + ": ");
                errorMsgBuilder.append(MessageUtil.getMessageOrDefault(error.getCode(), error.getArguments(),
                        error.getDefaultMessage()));
                errorMsgBuilder.append(System.lineSeparator());
            }

            byte[] errorMsg = errorMsgBuilder.toString().getBytes();
            response.setContentType("text/plain");
            response.setContentLength(errorMsg.length);
            servletOutputStream.write(errorMsg);

        } catch (IOException e) {
            LogEvent.logError(e.getMessage(), e);
        }
    }

    protected Errors getErrors() {
        return (Errors) request.getAttribute(Constants.REQUEST_ERRORS);
    }

    // move flash attributes into request
    protected void addFlashMsgsToRequest(HttpServletRequest request) {
        Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
        if (inputFlashMap != null) {
            Boolean success = (Boolean) inputFlashMap.get(FWD_SUCCESS);
            request.setAttribute(FWD_SUCCESS, success);

            String successMessage = (String) inputFlashMap.get(Constants.SUCCESS_MSG);
            request.setAttribute(Constants.SUCCESS_MSG, successMessage);

            Errors errors = (Errors) inputFlashMap.get(Constants.REQUEST_ERRORS);
            request.setAttribute(Constants.SUCCESS_MSG, errors);

            List<String> messages = (List<String>) inputFlashMap.get(Constants.REQUEST_MESSAGES);
            request.setAttribute(Constants.REQUEST_MESSAGES, messages);

            List<String> warnings = (List<String>) inputFlashMap.get(Constants.REQUEST_WARNINGS);
            request.setAttribute(Constants.SUCCESS_MSG, warnings);
        }
    }

    // re-initialize form object in the request
    protected <T extends BaseForm> T resetFormSessionObject(String objectName, T form) {
        try {
            @SuppressWarnings("unchecked")
            T newForm = (T) form.getClass().newInstance();
            request.getSession().setAttribute(objectName, newForm);
            return newForm;
        } catch (InstantiationException | IllegalAccessException e) {
            LogEvent.logError(e.getMessage(), e);
            return form;
        }
    }

    public <T extends BaseForm> T resetSessionFormToType(BaseForm form, Class<T> classType) {
        try {
            T newForm = classType.newInstance();
            request.getSession().setAttribute("form", newForm);
            return newForm;
        } catch (InstantiationException | IllegalAccessException e) {
            LogEvent.logError(e.getMessage(), e);
            return null;
        }
    }
}
