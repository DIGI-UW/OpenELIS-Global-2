package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointActivationEventDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointActivationEvent;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;

@RunWith(MockitoJUnitRunner.class)
public class MicroBreakpointAdminServiceTest {

    @Mock
    private MicroBreakpointStandardDAO standardDAO;
    @Mock
    private MicroBreakpointRuleDAO ruleDAO;
    @Mock
    private MicroBreakpointActivationEventDAO activationEventDAO;
    @Mock
    private MicroAstRunDAO astRunDAO;

    private MicroBreakpointAdminService service;

    @Before
    public void setUp() {
        service = new MicroBreakpointAdminServiceImpl(standardDAO, ruleDAO, activationEventDAO, astRunDAO);
    }

    @Test
    public void activateMakesOnlyRequestedPublisherVersionActiveAndAuditsActor() {
        MicroBreakpointStandard previous = standard("old", "CLSI", "2025", "ACTIVE");
        MicroBreakpointStandard requested = standard("new", "CLSI", "2026", "LOADED");
        when(standardDAO.get("new")).thenReturn(Optional.of(requested));
        when(standardDAO.getActiveForAuthority("CLSI")).thenReturn(List.of(previous));

        service.activate("new", Date.valueOf("2026-08-04"), "42");

        assertEquals("LOADED", previous.getLifecycleStatus());
        assertEquals("ACTIVE", requested.getLifecycleStatus());
        assertEquals(Date.valueOf("2026-08-04"), requested.getEffectiveDate());
        assertEquals("42", requested.getLastUpdatedBy());
        verify(standardDAO).update(previous);
        verify(standardDAO).update(requested);

        ArgumentCaptor<MicroBreakpointActivationEvent> events = ArgumentCaptor
                .forClass(MicroBreakpointActivationEvent.class);
        verify(activationEventDAO, org.mockito.Mockito.times(2)).insert(events.capture());
        assertEquals(List.of("DEACTIVATED", "ACTIVATED"),
                events.getAllValues().stream().map(MicroBreakpointActivationEvent::getAction).toList());
        assertEquals(List.of("42", "42"),
                events.getAllValues().stream().map(MicroBreakpointActivationEvent::getActorId).toList());
    }

    @Test(expected = MicroReferenceConflictException.class)
    public void archiveRejectsStandardWithUnresolvedRuns() {
        MicroBreakpointStandard standard = standard("standard", "EUCAST", "16.0", "LOADED");
        when(standardDAO.get("standard")).thenReturn(Optional.of(standard));
        when(astRunDAO.countUnresolvedByBreakpointStandardId("standard")).thenReturn(2L);

        service.archive("standard", "42");
    }

    private MicroBreakpointStandard standard(String id, String authority, String version, String status) {
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setId(id);
        standard.setAuthority(authority);
        standard.setVersion(version);
        standard.setLifecycleStatus(status);
        return standard;
    }
}
