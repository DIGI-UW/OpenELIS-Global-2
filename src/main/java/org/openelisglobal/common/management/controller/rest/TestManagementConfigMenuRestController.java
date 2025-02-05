package org.openelisglobal.common.management.controller.rest;

import javax.servlet.http.HttpServletRequest;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.management.form.TestManagementConfigForm;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class TestManagementConfigMenuRestController extends BaseController {

    @RequestMapping(value = "/TestManagementConfigMenu", method = { RequestMethod.GET, RequestMethod.POST })
    public TestManagementConfigForm showTestManagementConfigMenu(HttpServletRequest request) {
        TestManagementConfigForm form = new TestManagementConfigForm();

        // return findForward(FWD_SUCCESS, form);
        return form;
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
