package org.openelisglobal.common.management.controller.rest;

import javax.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.management.form.TestSectionManagementForm;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class TestSectionManagementRestController extends BaseController {

    @RequestMapping(value = "/TestSectionManagement", method = { RequestMethod.GET, RequestMethod.POST })
    public TestSectionManagementForm showTestSectionManagement(HttpServletRequest request) {
        TestSectionManagementForm form = new TestSectionManagementForm();

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testSectionManagementDefinition";
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
