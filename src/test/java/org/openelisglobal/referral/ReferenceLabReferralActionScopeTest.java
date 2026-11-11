package org.openelisglobal.referral;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.referral.controller.rest.ReferenceLabResultsRestController;
import org.openelisglobal.referral.service.ReferenceLabResultsService;
import org.openelisglobal.referral.service.ReferralService;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The Referred Out list shows a user only the referrals whose order they may
 * read by lab unit, and its actions follow the list: a referral outside the
 * user's units is not found, and nothing is changed on it.
 */
public class ReferenceLabReferralActionScopeTest {

    private ReferenceLabResultsService referenceLabResultsService;
    private ReferralService referralService;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        ReferenceLabResultsRestController controller = new ReferenceLabResultsRestController();
        referenceLabResultsService = mock(ReferenceLabResultsService.class);
        referralService = mock(ReferralService.class);
        ReflectionTestUtils.setField(controller, "referenceLabResultsService", referenceLabResultsService);
        ReflectionTestUtils.setField(controller, "referralService", referralService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void everyActionOnAReferralOutsideTheUsersUnitsIsNotFound() throws Exception {
        when(referenceLabResultsService.isReferralVisible(eq("7"), any())).thenReturn(false);

        mockMvc.perform(put("/rest/reference-lab-results/referrals/7/accept")).andExpect(status().isNotFound());
        mockMvc.perform(put("/rest/reference-lab-results/referrals/7/reject").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reasonCode\":\"OTHER\",\"reasonText\":\"wrong tube\"}")).andExpect(status().isNotFound());
        mockMvc.perform(put("/rest/reference-lab-results/referrals/7/mark-lost")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"lost in transit\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/rest/reference-lab-results/referrals/7/notify")).andExpect(status().isNotFound());

        verify(referenceLabResultsService, never()).acceptReferral(anyString(), any());
        verify(referralService, never()).markReferralRejected(anyString(), any(), any(), any());
        verify(referralService, never()).markReferralAsLost(anyString(), any(), any());
        verify(referralService, never()).nudgeReferenceLab(anyString(), any(), any());
    }

    @Test
    public void anActionOnAReferralInsideTheUsersUnitsGoesThrough() throws Exception {
        when(referenceLabResultsService.isReferralVisible(eq("7"), any())).thenReturn(true);

        mockMvc.perform(put("/rest/reference-lab-results/referrals/7/accept")).andExpect(status().isNoContent());

        verify(referenceLabResultsService).acceptReferral(eq("7"), any());
    }
}
