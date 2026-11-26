package org.openelisglobal.test.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.CoreMatchers;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.validator.BatchTestReassignmentFormValidator;
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
 *
 * Issue #5089: return error status when batch test reassignment persistence
 * fails instead of logging the error, proceeding to FHIR sync, and returning
 * 200 OK.
 */
@RunWith(MockitoJUnitRunner.class)
public class BatchTestReassignmentRestControllerTest {

    private static final String EMPTY_LISTS = "\"current\":\"Amylase\",\"sampleType\":\"Serum\","
            + "\"changeNotStarted\":[],\"noChangeNotStarted\":[],\"changeTechReject\":[],"
            + "\"noChangeTechReject\":[],\"changeBioReject\":[],\"noChangeBioReject\":[],"
            + "\"changeNotValidated\":[],\"noChangeNotValidated\":[]";

    @Mock
    private AnalysisService analysisService;

    @Mock
    private FhirTransformService fhirTransformService;

    @Mock
    private IStatusService statusService;

    @Spy
    private BatchTestReassignmentFormValidator formValidator = new BatchTestReassignmentFormValidator();

    @InjectMocks
    private BatchTestReassignmentRestController controller;

    private MockMvc mvc;

    @Before
    public void setUp() {
        when(statusService.getStatusID(any(StatusService.AnalysisStatus.class))).thenReturn("1");
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();
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

    @Test
    public void persistenceFailureAnswers500SaveFailedAndSkipsFhirSync() throws Exception {
        doThrow(new LIMSRuntimeException("DB constraint error")).when(analysisService).updateAnalysises(anyList(),
                anyList(), anyString());

        save("{" + EMPTY_LISTS + ",\"replace\":[]}").andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("saveFailed"))
                .andExpect(jsonPath("$.message").value("The change was not saved."));

        verify(fhirTransformService, never()).transformAnalysisByIds(any());
    }

    @Test
    public void persistenceCollisionAnswers409ConflictAndSkipsFhirSync() throws Exception {
        doThrow(new LIMSDuplicateRecordException("Duplicate record")).when(analysisService).updateAnalysises(anyList(),
                anyList(), anyString());

        save("{" + EMPTY_LISTS + ",\"replace\":[]}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate"))
                .andExpect(jsonPath("$.message").value("A record with this name already exists."));

        verify(fhirTransformService, never()).transformAnalysisByIds(any());
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
