package org.openelisglobal.eqa.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.eqa.controller.rest.EQACycleRestController;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.openelisglobal.login.valueholder.UserSessionData;

/**
 * OGC-609 [EQA V2.1 / T-10] — the transition endpoint must not let a caller
 * describe its own audit record.
 *
 * <p>
 * A cycle-state audit row is what an ISO 15189 assessor reads to tell a
 * deliberate QA-officer override from an automatic timer expiry. If the request
 * body can name the actor, the trigger type or the trigger event, then that
 * distinction is whatever the caller says it is — and because AUTO rows are
 * exempt from the reason requirement, claiming automation also erases the
 * explanation. These tests pin the provenance to the session.
 */
@RunWith(MockitoJUnitRunner.class)
public class EQACycleRestControllerTest {

    private static final long SESSION_USER_ID = 1L;

    @Mock
    private EQACycleService cycleService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private EQACycleRestController controller;

    @Before
    public void setUp() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId((int) SESSION_USER_ID);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(usd);
        when(cycleService.transition(anyLong(), any(), any(), any(), any(), any(), any(), anyString()))
                .thenReturn(new EQACycle());
    }

    @Test
    public void theClaimedActorIsIgnoredInFavourOfTheSessionUser() {
        controller.transition(request, 7L,
                Map.of("newState", "PANEL_RECEIVED", "reason", "panel arrived", "triggeredBy", 424242));

        ArgumentCaptor<Long> actor = ArgumentCaptor.forClass(Long.class);
        verify(cycleService).transition(eq(7L), eq(EQACycleStatus.PANEL_RECEIVED), any(), any(), any(), actor.capture(),
                anyString(), anyString());
        assertEquals("the audit actor comes from the session, never the body", Long.valueOf(SESSION_USER_ID),
                actor.getValue());
    }

    @Test
    public void anHttpCallIsAlwaysRecordedAsAManualOverride() {
        // Claiming AUTO would both strip the actor and skip the reason rule.
        controller.transition(request, 7L, Map.of("newState", "PANEL_RECEIVED", "reason", "panel arrived",
                "triggerType", "AUTO", "triggerEvent", "DEADLINE_TIMER"));

        verify(cycleService).transition(eq(7L), eq(EQACycleStatus.PANEL_RECEIVED), any(), eq(EQATriggerType.MANUAL),
                eq(EQATriggerEvent.MANUAL_OVERRIDE), any(), anyString(), anyString());
    }

    @Test
    public void theStateMachineLaneIsStillCallerSupplied() {
        // This one is a legitimate parameter, not a provenance claim: a single lab
        // participates in some schemes and runs others. Authorisation for provider
        // transitions belongs to the permission tiers in T-12.
        controller.transition(request, 7L,
                Map.of("newState", "PREP_IN_PROGRESS", "reason", "prep started", "stateMachine", "PROVIDER"));

        verify(cycleService).transition(eq(7L), eq(EQACycleStatus.PREP_IN_PROGRESS), eq(EQAStateMachine.PROVIDER),
                any(), any(), any(), anyString(), anyString());
    }

    @Test
    public void newStateIsRequired() {
        assertEquals(400, controller.transition(request, 7L, Map.of("reason", "no state")).getStatusCode().value());
    }
}
