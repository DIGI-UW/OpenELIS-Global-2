package org.openelisglobal.vector.deconvolution.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.vector.deconvolution.service.VectorDeconvolutionService;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class VectorDeconvolutionValidationTest {
    @Test
    public void poolCountRefusal_returnsTranslationKeyAndCounts() throws Exception {
        VectorDeconvolutionService service = mock(VectorDeconvolutionService.class);
        when(service.initiate(any(), any()))
                .thenThrow(new LocalizedValidationException("vectorDec.error.poolCountExceedsMembers",
                        "Do not display this fallback", Map.of("max", "6", "requested", "1000000")));
        VectorDeconvolutionRestController controller = new VectorDeconvolutionRestController();
        ReflectionTestUtils.setField(controller, "deconvolutionService", service);
        UserSessionData actor = new UserSessionData();
        actor.setSytemUserId(1);
        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/rest/vector/deconvolution/initiate")
                        .requestAttr(IActionConstants.USER_SESSION_DATA, actor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vectorPoolId\":700,\"poolCount\":1000000,\"organismsPerPool\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageKey").value("vectorDec.error.poolCountExceedsMembers"))
                .andExpect(jsonPath("$.messageArgs.max").value("6"))
                .andExpect(jsonPath("$.messageArgs.requested").value("1000000"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }
}
