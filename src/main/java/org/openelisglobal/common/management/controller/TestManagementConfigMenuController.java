package org.openelisglobal.common.management.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.management.form.TestManagementConfigForm;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TestManagementConfigMenuController extends BaseController {

    @RequestMapping(value = "/TestManagementConfigMenu", method = { RequestMethod.GET, RequestMethod.POST })
    public ModelAndView showTestManagementConfigMenu(HttpServletRequest request) {
        TestManagementConfigForm form = new TestManagementConfigForm();

        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testManagementConfigDefinition";
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
