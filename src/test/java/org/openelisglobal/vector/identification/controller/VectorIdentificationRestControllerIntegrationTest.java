package org.openelisglobal.vector.identification.controller;

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

public class VectorIdentificationRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/vector-specimen-identification.xml");
        session = buildAuthenticatedSession();
    }

    @Test
    public void getWorklist_defaultPendingFilter_includesNotStartedSample() throws Exception {
        mockMvc.perform(get("/rest/vector/identification/worklist").session(session)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accessionNumber").value("VCT-ID-001"));
    }

    @Test
    public void getWorklist_completeFilter_excludesNotStartedSample() throws Exception {
        mockMvc.perform(get("/rest/vector/identification/worklist").param("status", "complete").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getWorklist_completedSample_appearsInCompleteFilter() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.sample SET identification_status = 'COMPLETE' WHERE id = 800");

        mockMvc.perform(get("/rest/vector/identification/worklist").param("status", "complete").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accessionNumber").value("VCT-ID-001"));
    }

    @Test
    public void getSpecimensForLot_validLot_returnsThreeSpecimens() throws Exception {
        mockMvc.perform(get("/rest/vector/identification/lots/800/specimens").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    public void getSpecimensForLot_unknownLot_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/rest/vector/identification/lots/9999/specimens").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getSpecimensForLot_preIdentifiedItem_hasIdentificationStatus() throws Exception {
        mockMvc.perform(get("/rest/vector/identification/lots/800/specimens").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sampleItemId == 803)].identificationStatus").value("CONFIRMED"));
    }

    @Test
    public void getIdentification_preSeededSpecimen_returns200() throws Exception {
        mockMvc.perform(get("/rest/vector/identification/specimens/803/identification").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sampleItemId").value(803))
                .andExpect(jsonPath("$.identificationMethod").value("MORPHOLOGICAL"))
                .andExpect(jsonPath("$.confidence").value("CONFIRMED"));
    }

    @Test
    public void getIdentification_unidentifiedSpecimen_returns404() throws Exception {
        mockMvc.perform(get("/rest/vector/identification/specimens/801/identification").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    public void identify_validRequest_returns200WithIdentification() throws Exception {
        String body = "{\"vectorSpeciesId\":800,\"identificationMethod\":\"MORPHOLOGICAL\",\"confidence\":\"CONFIRMED\"}";

        mockMvc.perform(post("/rest/vector/identification/specimens/801/identify")
                .contentType(MediaType.APPLICATION_JSON).content(body).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.identification.sampleItemId").value(801));
    }

    @Test
    public void identify_pathParamMismatch_returns400() throws Exception {
        String body = "{\"sampleItemId\":999,\"vectorSpeciesId\":800,\"identificationMethod\":\"MORPHOLOGICAL\",\"confidence\":\"CONFIRMED\"}";

        mockMvc.perform(post("/rest/vector/identification/specimens/801/identify")
                .contentType(MediaType.APPLICATION_JSON).content(body).session(session))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void identify_invalidConfidence_returns400() throws Exception {
        String body = "{\"vectorSpeciesId\":800,\"identificationMethod\":\"MORPHOLOGICAL\",\"confidence\":\"WRONG\"}";

        mockMvc.perform(post("/rest/vector/identification/specimens/801/identify")
                .contentType(MediaType.APPLICATION_JSON).content(body).session(session))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void bulkIdentify_twoSpecimens_returns200() throws Exception {
        String body = "{\"sampleItemIds\":[801,802],\"vectorSpeciesId\":800,\"identificationMethod\":\"MORPHOLOGICAL\",\"confidence\":\"CONFIRMED\"}";

        mockMvc.perform(post("/rest/vector/identification/specimens/bulk-identify")
                .contentType(MediaType.APPLICATION_JSON).content(body).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.identifications.length()").value(2));
    }

    @Test
    public void bulkIdentify_emptyList_returns400() throws Exception {
        String body = "{\"sampleItemIds\":[],\"vectorSpeciesId\":800,\"identificationMethod\":\"MORPHOLOGICAL\",\"confidence\":\"CONFIRMED\"}";

        mockMvc.perform(post("/rest/vector/identification/specimens/bulk-identify")
                .contentType(MediaType.APPLICATION_JSON).content(body).session(session))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void getResultCandidates_unknownLot_returns404() throws Exception {
        mockMvc.perform(get("/rest/vector/identification/lots/9999/result-candidates").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getResultCandidates_knownLotNoResults_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/rest/vector/identification/lots/800/result-candidates").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void addBloodmealPanel_unknownSpecimen_returns404() throws Exception {
        mockMvc.perform(post("/rest/vector/identification/specimens/9999/bloodmeal-panel").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    public void addBloodmealPanel_panelNotConfigured_returns404() throws Exception {
        // Item 803 is Mosquito type — passes the type check.
        // "Mosquito Blood-Meal Identification Panel" is not in the test DB → 404.
        mockMvc.perform(post("/rest/vector/identification/specimens/803/bloodmeal-panel").session(session))
                .andExpect(status().isNotFound());
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
