package org.openelisglobal.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.form.MainForm;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomePageController extends BaseController {

    @RequestMapping(value = "/HomePage", method = RequestMethod.GET)
    public ModelAndView showHomePage(HttpServletRequest request) {
        MainForm form = new MainForm();

        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "homePageDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
