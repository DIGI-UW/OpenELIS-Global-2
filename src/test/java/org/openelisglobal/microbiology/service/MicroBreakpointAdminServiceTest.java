package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
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
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointActivationEventDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointActivationEvent;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
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
    @Mock
    private MicroOrganismDAO organismDAO;
    @Mock
    private MicroAntibioticDAO antibioticDAO;

    private MicroBreakpointAdminService service;

    @Before
    public void setUp() {
        service = new MicroBreakpointAdminServiceImpl(standardDAO, ruleDAO, activationEventDAO, astRunDAO, organismDAO,
                antibioticDAO);
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

    @Test
    public void standardListUsesPagedDaoContract() {
        MicroBreakpointStandard loaded = standard("standard", "CLSI", "2026", "LOADED");
        when(standardDAO.search("synthetic", "LOADED", "CLSI", "name", 20, 20)).thenReturn(List.of(loaded));
        when(standardDAO.countSearch("synthetic", "LOADED", "CLSI")).thenReturn(1L);
        when(ruleDAO.countByStandardId("standard")).thenReturn(2L);

        org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm query = new org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm();
        query.q = "synthetic";
        query.status = "LOADED";
        query.authority = "CLSI";
        query.page = 2;

        assertEquals(1L, service.getStandards(query).total);
        verify(standardDAO, never()).getAll();
    }

    @Test
    public void standardListNormalizesUnsupportedStatusBeforeDaoQuery() {
        org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm query = new org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm();
        query.status = "INACTIVE";

        service.getStandards(query);

        verify(standardDAO).search("", "ALL", null, "name", 0, 20);
        verify(standardDAO).countSearch("", "ALL", null);
    }

    @Test
    public void directRuleLookupRequiresTheRequestedStandard() {
        MicroBreakpointRule rule = new MicroBreakpointRule();
        rule.setId("rule-1");
        rule.setStandardId("standard");
        rule.setOrganismGroup("Enterobacterales");
        rule.setAntibioticId("cip");
        rule.setMethod("MIC");
        rule.setBreakpointType("MIC");
        rule.setLocallyCustomized(true);
        when(standardDAO.get("standard")).thenReturn(Optional.of(standard("standard", "CLSI", "2026", "LOADED")));
        when(ruleDAO.get("rule-1")).thenReturn(Optional.of(rule));

        assertEquals("rule-1", service.getRule("standard", "rule-1").id);
    }

    @Test
    public void techniqueAwareRulePersistsWithItsDerivedMeasurementType() {
        when(standardDAO.get("standard"))
                .thenReturn(Optional.of(standard("standard", "CLSI", "2026", "LOADED")));
        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setId("cip");
        when(antibioticDAO.get("cip")).thenReturn(Optional.of(antibiotic));
        when(ruleDAO.findByNaturalKey("standard", null, "Enterobacterales", "cip", "DISK_DIFFUSION", null,
                "ZONE")).thenReturn(Optional.empty());
        org.openelisglobal.microbiology.form.MicroBreakpointRuleAdminForm request =
                new org.openelisglobal.microbiology.form.MicroBreakpointRuleAdminForm();
        request.organismGroup = "Enterobacterales";
        request.antibioticId = "cip";
        request.method = MicroAstTechnique.DISK_DIFFUSION.name();
        request.breakpointType = "ZONE";
        request.susceptibleValue = new java.math.BigDecimal("20");

        service.saveRule("standard", null, request, "42");

        ArgumentCaptor<MicroBreakpointRule> rule = ArgumentCaptor.forClass(MicroBreakpointRule.class);
        verify(ruleDAO).insert(rule.capture());
        assertEquals(MicroAstTechnique.DISK_DIFFUSION.name(), rule.getValue().getMethod());
        assertEquals("ZONE", rule.getValue().getBreakpointType());
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
