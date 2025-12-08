package org.openelisglobal.samplemanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.form.MainForm;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Page Controller for Sample Management React application.
 *
 * <p>
 * This controller serves the React frontend for the Sample Management feature.
 * It maps the /SampleManagement URL (from the menu database entry) to the React
 * application via a Tiles definition.
 *
 * <p>
 * Related: Feature 001-sample-management, Task T036
 */
@Controller
public class SampleManagementPageController extends BaseController {

    @RequestMapping(value = "/SampleManagement", method = RequestMethod.GET)
    public ModelAndView showSampleManagementPage(HttpServletRequest request) {
        MainForm form = new MainForm();
        request.getSession().setAttribute(SAVE_DISABLED, TRUE);
        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleManagementDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "banner.menu.sampleManagement";
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
