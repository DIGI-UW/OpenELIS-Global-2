package org.openelisglobal.dev.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.dev.form.WebTestInfoForm;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.patient.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class webtestInfoController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] {};

    @Autowired
    LoginUserService loginService;
    @Autowired
    PatientService patientService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/webtestInfo", method = RequestMethod.GET)
    public ModelAndView showwebtestInfo(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        WebTestInfoForm form = new WebTestInfoForm();
        String xmlWad = getWebTestXmlWad();

        form.setXmlWad(xmlWad);

        return findForward(FWD_SUCCESS, form);
    }

    private String getWebTestXmlWad() {
        StringBuilder xmlBuilder = new StringBuilder();

        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xmlBuilder.append("<webTestInfo>");

        addUserInfo(xmlBuilder);
        // addUserLocked(xmlBuilder);
        // addNumberOfPatients(xmlBuilder);
        // addNumberOfSamples(xmlBuilder);

        xmlBuilder.append("</webTestInfo>");
        return xmlBuilder.toString();
    }

    private void addUserInfo(StringBuilder xmlBuilder) {
        // Login user = loginDAO.getUserProfile("user");
        LoginUser user = loginService.getUserProfile("webtest");
        if (user != null) {
            xmlBuilder.append("<webtestuser-id>");
            xmlBuilder.append(user.getSystemUserId() + "-" + user.getId());
            xmlBuilder.append("</webtestuser-id>");
            xmlBuilder.append("<webtestuser-passwd>");
            xmlBuilder.append(user.getPassword());
            xmlBuilder.append("</webtestuser-passwd>");
            xmlBuilder.append("<webtestuser-accountLocked>");
            xmlBuilder.append(user.getAccountLocked());
            xmlBuilder.append("</webtestuser-accountLocked>");
        } else {
            xmlBuilder.append("<webtestuser-id>no 'webtest' user</webtestuser-id>");
        }
    }

    // private void addNumberOfPatients(StringBuilder xmlBuilder) {
    // int count = new PatientDAOImpl().getCount();
    //
    // xmlBuilder.append("<patient-count>");
    // xmlBuilder.append(String.valueOf(count));
    // xmlBuilder.append("</patient-count>");
    //
    // }

    // private void addNumberOfSamples(StringBuilder xmlBuilder) {
    // int count = new PatientDAOImpl().getCount();
    //
    // xmlBuilder.append("<sample-count>");
    // xmlBuilder.append(String.valueOf(count));
    // xmlBuilder.append("</sample-count>");
    //
    // }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "webTestInfoDefinition";
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
