package org.openelisglobal.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorController {

    @RequestMapping(value = "errors", method = RequestMethod.GET)
    public ModelAndView renderErrorPage(HttpServletRequest httpRequest) {

        ModelAndView errorPage = new ModelAndView("404ErrorPage");
        String errorMsg = "";
        int httpErrorCode = getErrorCode(httpRequest);

        switch (httpErrorCode) {
        case 400: {
            errorMsg = "Http Error Code: 400. Bad Request";
            break;
        }
        case 401: {
            errorMsg = "Http Error Code: 401. Unauthorized";
            break;
        }
        case 404: {
            errorMsg = "Http Error Code: 404. Resource not found";
            break;
        }
        case 500: {
            errorMsg = "Http Error Code: 500. Internal Server Error";
            break;
        }
        }
        errorPage.addObject("errorMsg", errorMsg);
        return errorPage;
    }

    private int getErrorCode(HttpServletRequest httpRequest) {
        return (Integer) httpRequest.getAttribute("jakarta.servlet.error.status_code");
    }
}
