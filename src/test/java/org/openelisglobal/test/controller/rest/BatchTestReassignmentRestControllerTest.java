package org.openelisglobal.test.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.CoreMatchers;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * OGC-1515: the batch reassignment save answered 200 with the form when its
 * validator refused the request, and the screen then reported "Successfully
 * saved" although nothing was cancelled. A refusal is now a 400 naming the
 * field.
 */
public class BatchTestReassignmentRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String EMPTY_LISTS = "\"current\":\"Amylase\",\"sampleType\":\"Serum\","
            + "\"changeNotStarted\":[],\"noChangeNotStarted\":[],\"changeTechReject\":[],"
            + "\"noChangeTechReject\":[],\"changeBioReject\":[],\"noChangeBioReject\":[],"
            + "\"changeNotValidated\":[],\"noChangeNotValidated\":[]";

    @Autowired
    private BatchTestReassignmentRestController controller;

    private MockMvc mvc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        Object target = AopProxyUtils.getSingletonTarget(controller);
        mvc = MockMvcBuilders.standaloneSetup(target != null ? target : controller).setValidator(validator).build();
    }

    @Test
    public void aReplacementListThatIsNotAListIsRefusedWith400NamingTheField() throws Exception {
        save("{" + EMPTY_LISTS + ",\"replace\":\"\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"))
                .andExpect(jsonPath("$.fieldErrors[*].field", CoreMatchers.hasItem("jsonWad")));
    }

    @Test
    public void aWellFormedRequestIsAccepted() throws Exception {
        save("{" + EMPTY_LISTS + ",\"replace\":[]}").andExpect(status().isOk());
    }

    private ResultActions save(String jsonWad) throws Exception {
        String body = "{\"jsonWad\":\"" + jsonWad.replace("\"", "\\\"") + "\"}";
        return mvc.perform(post("/rest/BatchTestReassignment").contentType(MediaType.APPLICATION_JSON).content(body)
                .session(adminSession()));
    }

    private static MockHttpSession adminSession() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        sessionData.setAdmin(true);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return httpSession;
    }
}
