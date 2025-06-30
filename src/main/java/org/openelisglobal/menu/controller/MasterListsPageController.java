package org.openelisglobal.menu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.menu.form.AdminMenuForm;
import org.openelisglobal.menu.service.AdminMenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MasterListsPageController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] {};

    @Autowired
    private AdminMenuItemService adminMenuItemService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/MasterListsPage", method = { RequestMethod.GET, RequestMethod.POST })
    public ModelAndView showMasterListsPage(HttpServletRequest request) {

        AdminMenuForm form = new AdminMenuForm();
        form.setAdminMenuItems(adminMenuItemService.getActiveItemsSorted());
        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "masterListsPageDefinition";
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
