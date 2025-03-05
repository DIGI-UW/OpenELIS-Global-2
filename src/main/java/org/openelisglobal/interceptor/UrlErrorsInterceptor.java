package org.openelisglobal.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.internationalization.MessageUtil;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UrlErrorsInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        BaseErrors errors = new BaseErrors();
        if ("denied".equals(request.getParameter("access"))) {
            errors.reject("login.error.module.not.allow", "login.error.module.not.allow");
        }
        if (errors.hasErrors()) {
            if (request.getAttribute(Constants.REQUEST_ERRORS) == null) {
                request.setAttribute(Constants.REQUEST_ERRORS, errors);
            } else {
                ((Errors) request.getAttribute(Constants.REQUEST_ERRORS)).addAllErrors(errors);
            }
        }

        List<String> warnings = new ArrayList<>();
        if ("true".equals(request.getParameter("passReminder"))) {
            warnings.add(MessageUtil.getMessage("login.password.expired.reminder"));
        }
        if (!warnings.isEmpty()) {
            if (request.getAttribute(Constants.REQUEST_WARNINGS) == null) {
                request.setAttribute(Constants.REQUEST_WARNINGS, warnings);
            } else {
                ((List<String>) request.getAttribute(Constants.REQUEST_WARNINGS)).addAll(warnings);
            }
        }

        return true;
    }
}
