package org.openelisglobal.vector.deconvolution.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

public class VectorDeconvolutionRestControllerTest extends BaseWebContextSensitiveTest {

    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/vector-decon-initiate.xml");
        session = buildAuthenticatedSession();
    }

    @Test
    public void getWorklist_noEligibleSamples_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/rest/vector/deconvolution/worklist").session(session)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getWorklist_sampleInPendingStatus_includesRowInResponse() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.vector_pool SET deconvolution_status = 'PENDING' WHERE id = 700");

        mockMvc.perform(get("/rest/vector/deconvolution/worklist").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accessionNumber").value("VCT-DECON-001"))
                .andExpect(jsonPath("$[0].deconvolutionStatus").value("PENDING"));
    }

    @Test
    public void getDeconvolution_unknownPoolId_returns404() throws Exception {
        mockMvc.perform(get("/rest/vector/deconvolution/pool/999999").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getDeconvolution_poolWithNotApplicableStatus_returns404() throws Exception {
        mockMvc.perform(get("/rest/vector/deconvolution/pool/700").session(session)).andExpect(status().isNotFound());
    }

    @Test
    public void getDeconvolution_poolInPendingStatus_returns200WithSampleId() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.vector_pool SET deconvolution_status = 'PENDING' WHERE id = 700");

        mockMvc.perform(get("/rest/vector/deconvolution/pool/700").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.parentSampleId").value(700))
                .andExpect(jsonPath("$.newDeconvolutionStatus").value("PENDING"));
    }

    @Test
    public void previewReflexes_existingPool_returns200() throws Exception {
        mockMvc.perform(get("/rest/vector/deconvolution/preview/700").session(session)).andExpect(status().isOk());
    }

    @Test
    public void initiate_nullPoolId_returns400() throws Exception {
        String body = "{\"poolCount\": 2, \"organismsPerPool\": 3}";

        mockMvc.perform(post("/rest/vector/deconvolution/initiate").contentType(MediaType.APPLICATION_JSON)
                .content(body).session(session)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void initiate_nonexistentPool_returns409() throws Exception {
        String body = "{\"vectorPoolId\": 9999999, \"poolCount\": 2, \"organismsPerPool\": 3}";

        mockMvc.perform(post("/rest/vector/deconvolution/initiate").contentType(MediaType.APPLICATION_JSON)
                .content(body).session(session)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void forceComplete_poolInPendingStatus_returns200WithCompleteStatus() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.vector_pool SET deconvolution_status = 'PENDING' WHERE id = 700");

        mockMvc.perform(put("/rest/vector/deconvolution/pool/700/complete").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.newDeconvolutionStatus").value("COMPLETE"));
    }

    private MockHttpSession buildAuthenticatedSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN", "ROLE_RESULTS")
                .build();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));

        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);

        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return httpSession;
    }
}
