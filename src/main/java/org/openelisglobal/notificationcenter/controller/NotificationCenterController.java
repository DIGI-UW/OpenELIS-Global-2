package org.openelisglobal.notificationcenter.controller;

import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.notificationcenter.form.NotificationCenterForm;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class NotificationCenterController extends BaseController {

    @GetMapping("/NotificationCenter")
    public ModelAndView showNotificationCenter() {
        return findForward(FWD_SUCCESS, new NotificationCenterForm());
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "notificationCenterDefinition";
        }
        return null;
    }

    @Override
    protected String getPageTitleKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        // TODO Auto-generated method stub
        return null;
    }
}
