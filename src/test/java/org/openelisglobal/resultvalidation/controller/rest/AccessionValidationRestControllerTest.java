package org.openelisglobal.resultvalidation.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.audittrail.dao.HistoryDAO;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.qc.service.QcHoldService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.reports.service.DocumentTrackService;
import org.openelisglobal.reports.service.DocumentTypeService;
import org.openelisglobal.reports.valueholder.DocumentType;
import org.openelisglobal.result.service.ResultEntryAcknowledgementService;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.service.ResultValidationService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.testresult.service.TestResultService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class AccessionValidationRestControllerTest {

    @Mock
    private AnalysisService analysisService;
    @Mock
    private TestResultService testResultService;
    @Mock
    private SampleHumanService sampleHumanService;
    @Mock
    private DocumentTrackService documentTrackService;
    @Mock
    private TestSectionService testSectionService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private ReferenceTablesService referenceTablesService;
    @Mock
    private DocumentTypeService documentTypeService;
    @Mock
    private ResultValidationService resultValidationService;
    @Mock
    private NoteService noteService;
    @Mock
    private FhirTransformService fhirTransformService;

    @Mock
    private UserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private SearchResultsService searchService;
    @Mock
    private QcHoldService qcHoldService;
    @Mock
    private ResultEntryAcknowledgementService acknowledgementService;
    @Mock
    private SampleService sampleService;
    @Mock
    private HistoryDAO historyDAO;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    private Object previousFactory;
    private MockMvc mockMvc;
    private AccessionValidationRestController controller;
    private MockHttpSession session;
    private IResultUpdate updater;

    @Before
    public void setUp() {
        previousFactory = ReflectionTestUtils.getField(SpringContext.class, "factory");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);
        when(beanFactory.getBean(QcHoldService.class)).thenReturn(qcHoldService);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(qcHoldService.analysisIdsBlockedFromRelease(any())).thenReturn(Collections.emptySet());
        when(configurationProperties.getPropertyValueLowerCase(any())).thenReturn("false");

        ReferenceTables rt = new ReferenceTables();
        rt.setId("1");
        when(referenceTablesService.getReferenceTableByName("RESULT")).thenReturn(rt);

        DocumentType dt = new DocumentType();
        dt.setId("2");
        when(documentTypeService.getDocumentTypeByName("resultExport")).thenReturn(dt);

        controller = new AccessionValidationRestController(analysisService, testResultService, sampleHumanService,
                documentTrackService, testSectionService, systemUserService, referenceTablesService,
                documentTypeService, resultValidationService, noteService, fhirTransformService);

        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "roleService", roleService);
        ReflectionTestUtils.setField(controller, "searchService", searchService);
        ReflectionTestUtils.setField(controller, "qcHoldService", qcHoldService);
        ReflectionTestUtils.setField(controller, "acknowledgementService", acknowledgementService);
        ReflectionTestUtils.setField(controller, "sampleService", sampleService);
        ReflectionTestUtils.setField(controller, "historyDAO", historyDAO);

        session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setSession(session);
        mockRequest.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        ReflectionTestUtils.setField(controller, "request", mockRequest);

        List<List<Object>> pages = new ArrayList<>();
        AnalysisItem item = new AnalysisItem();
        item.setAccessionNumber("ACC001");
        pages.add(new ArrayList<>(List.of(item)));
        session.setAttribute(IActionConstants.RESULTS_SESSION_CACHE, pages);

        updater = mock(IResultUpdate.class);
        when(acknowledgementService.alertsForValidationItems(any())).thenReturn(Collections.emptyList());
        when(acknowledgementService.acknowledgementRecorder(any(), any(), any())).thenReturn(updater);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
    }

    @Test
    public void showAccessionValidationRangeSave_WhenPersistThrowsLIMSRuntimeException_Returns500() throws Exception {
        doThrow(new LIMSRuntimeException("DB error")).when(resultValidationService).persistdata(any(), any(), any(),
                any(), any(), any(), any(), any(), any());

        mockMvc.perform(post("/rest/AccessionValidation").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultList\":[],\"paging\":{\"currentPage\":\"1\",\"totalPages\":\"1\"}}"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error").value("saveFailed"))
                .andExpect(jsonPath("$.message").value("The change was not saved."));

        verify(updater, never()).postTransactionalCommitUpdate(any());
        verify(fhirTransformService, never()).transformPersistResultValidationFhirObjects(any(), any(), any(), any(),
                any(), any());
    }

    @Test
    public void showAccessionValidationRangeSave_WhenPersistThrowsLIMSDuplicateRecordException_Returns409()
            throws Exception {
        doThrow(new LIMSDuplicateRecordException("Duplicate record")).when(resultValidationService).persistdata(any(),
                any(), any(), any(), any(), any(), any(), any(), any());

        mockMvc.perform(post("/rest/AccessionValidation").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultList\":[],\"paging\":{\"currentPage\":\"1\",\"totalPages\":\"1\"}}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("duplicate"))
                .andExpect(jsonPath("$.message").value("A record with this name already exists."));

        verify(updater, never()).postTransactionalCommitUpdate(any());
        verify(fhirTransformService, never()).transformPersistResultValidationFhirObjects(any(), any(), any(), any(),
                any(), any());
    }
}
