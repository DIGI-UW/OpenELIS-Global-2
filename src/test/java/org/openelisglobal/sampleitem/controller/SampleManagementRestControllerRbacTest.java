package org.openelisglobal.sampleitem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.RbacAction;
import org.openelisglobal.rbac.RbacPermissionService;
import org.openelisglobal.sampleitem.form.CreateAliquotForm;
import org.openelisglobal.sampleitem.service.SampleManagementService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class SampleManagementRestControllerRbacTest {

    @InjectMocks
    private SampleManagementRestController controller;

    @Mock
    private SampleManagementService sampleManagementService;

    @Mock
    private SampleItemService sampleItemService;

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
    public void createAliquotReturnsForbiddenWithoutProcessPermission() throws Exception {
        CreateAliquotForm form = new CreateAliquotForm();
        form.setParentSampleItemId("10");
        form.setQuantityToTransfer(BigDecimal.ONE);

        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(42);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        SampleItem sampleItem = new SampleItem();
        when(sampleItemService.getData("10")).thenReturn(sampleItem);
        when(departmentIsolationService.canAccessSampleItem(eq(sampleItem), any())).thenReturn(true);
        when(rbacPermissionService.hasPermission(any(), eq(RbacAction.PROCESS_SAMPLES))).thenReturn(false);

        mockMvc.perform(post("/rest/sample-management/aliquot")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isForbidden());
    }
}
