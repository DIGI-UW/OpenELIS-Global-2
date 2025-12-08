package org.openelisglobal.common.management.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.management.form.PanelManagementForm;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PanelManagementController extends BaseController {

    @RequestMapping(value = "/PanelManagement", method = { RequestMethod.GET, RequestMethod.POST })
    public ModelAndView showPanelManagement(HttpServletRequest request) {
        PanelManagementForm form = new PanelManagementForm();

        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "panelManagementDefinition";
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
