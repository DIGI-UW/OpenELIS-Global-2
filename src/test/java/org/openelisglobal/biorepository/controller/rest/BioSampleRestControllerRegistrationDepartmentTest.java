package org.openelisglobal.biorepository.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.controller.rest.BioSampleRestController.SampleRegistrationRequest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.RbacAction;
import org.openelisglobal.rbac.RbacPermissionService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class BioSampleRestControllerRegistrationDepartmentTest {

    @InjectMocks
    private BioSampleRestController controller;

    @Mock
    private BioSampleService bioSampleService;

    @Mock
    private SampleService sampleService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private DepartmentIsolationService departmentIsolationService;

    @Mock
    private RbacPermissionService rbacPermissionService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void registerReturnsForbiddenWhenRestrictedUserHasNoActiveDepartment() throws Exception {
        SampleRegistrationRequest request = new SampleRegistrationRequest();
        request.setBarcode("BIO-TEST-001");
        request.setSampleTypeId("1");

        MockHttpSession session = sessionWithUser(42, 178);

        when(departmentIsolationService.resolveDepartmentForScopedCreate(any(), any())).thenReturn(null);
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(any())).thenReturn(false);
        when(departmentIsolationService.getRestrictedUserTestSectionIds(any())).thenReturn(java.util.Set.of());

        mockMvc.perform(post("/rest/biorepository/sample/register")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void registerReturnsForbiddenWhenDepartmentAccessDenied() throws Exception {
        SampleRegistrationRequest request = new SampleRegistrationRequest();
        request.setBarcode("BIO-TEST-002");
        request.setSampleTypeId("1");

        MockHttpSession session = sessionWithUser(42, 178);

        when(departmentIsolationService.resolveDepartmentForScopedCreate(any(), any())).thenReturn(178);
        when(departmentIsolationService.canAccessDepartmentScopedLocation(eq(178), any())).thenReturn(false);

        mockMvc.perform(post("/rest/biorepository/sample/register")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void registerReturnsBadRequestWhenProjectBelongsToDifferentDepartment() throws Exception {
        SampleRegistrationRequest request = new SampleRegistrationRequest();
        request.setBarcode("BIO-TEST-003");
        request.setSampleTypeId("1");
        request.setProjectId("Other Dept Project");

        MockHttpSession session = sessionWithUser(42, 178);

        when(departmentIsolationService.resolveDepartmentForScopedCreate(any(), any())).thenReturn(178);
        when(departmentIsolationService.canAccessDepartmentScopedLocation(eq(178), any())).thenReturn(true);
        when(departmentIsolationService.isInventoryProjectConsistent(178, "Other Dept Project")).thenReturn(false);

        mockMvc.perform(post("/rest/biorepository/sample/register")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession sessionWithUser(int userId, int loginLabUnit) {
        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(userId);
        usd.setLoginLabUnit(loginLabUnit);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return session;
    }
}
