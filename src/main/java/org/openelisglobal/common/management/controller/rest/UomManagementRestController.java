package org.openelisglobal.common.management.controller.rest;

import javax.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.management.form.UomManagementForm;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class UomManagementRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] {};

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/UomManagement", method = { RequestMethod.GET, RequestMethod.POST })
    public UomManagementForm showUomManagement(HttpServletRequest request) {
        UomManagementForm form = new UomManagementForm();

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "uomManagementDefinition";
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
