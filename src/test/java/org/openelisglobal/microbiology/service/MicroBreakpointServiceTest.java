package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;

@RunWith(MockitoJUnitRunner.class)
public class MicroBreakpointServiceTest {

    @Mock
    private MicroBreakpointStandardDAO standardDAO;

    @Mock
    private MicroBreakpointRuleDAO ruleDAO;
    @Mock
    private org.openelisglobal.siteinformation.service.SiteInformationService settings;

    @Test
    public void defaultResolvesPortableAuthorityAndEditionAndReflectsActivation() {
        var authority = new org.openelisglobal.siteinformation.valueholder.SiteInformation();
        authority.setValue("EUCAST");
        var edition = new org.openelisglobal.siteinformation.valueholder.SiteInformation();
        edition.setValue("2026-2026.1");
        when(settings.getSiteInformationByName(MicroBreakpointServiceImpl.DEFAULT_AUTHORITY)).thenReturn(authority);
        when(settings.getSiteInformationByName(MicroBreakpointServiceImpl.DEFAULT_VERSION)).thenReturn(edition);
        var standard = new MicroBreakpointStandard();
        standard.setId("portable-id");
        standard.setIsActive("Y");
        standard.setLifecycleStatus("ACTIVE");
        when(standardDAO.findByAuthorityAndVersion("EUCAST", "2026-2026.1"))
                .thenReturn(java.util.Optional.of(standard));
        var service = new MicroBreakpointServiceImpl(standardDAO, ruleDAO, settings);
        var result = service.getDefaultStandard();
        org.junit.Assert.assertTrue(result.configured && result.available);
        assertEquals("portable-id", result.standardId);
        standard.setLifecycleStatus("LOADED");
        org.junit.Assert.assertFalse(service.getDefaultStandard().available);
    }

    @Test
    public void savingDefaultUsesExistingSettingsAndAuthenticatedAuditActor() {
        var authority = new org.openelisglobal.siteinformation.valueholder.SiteInformation();
        authority.setId("setting-authority");
        var edition = new org.openelisglobal.siteinformation.valueholder.SiteInformation();
        edition.setId("setting-edition");
        when(settings.getSiteInformationByName(MicroBreakpointServiceImpl.DEFAULT_AUTHORITY)).thenReturn(authority);
        when(settings.getSiteInformationByName(MicroBreakpointServiceImpl.DEFAULT_VERSION)).thenReturn(edition);
        var standard = new MicroBreakpointStandard();
        standard.setId("eu");
        standard.setAuthority("EUCAST");
        standard.setVersion("2026-2026.1");
        standard.setIsActive("Y");
        standard.setLifecycleStatus("ACTIVE");
        when(standardDAO.get("eu")).thenReturn(java.util.Optional.of(standard));
        when(standardDAO.findByAuthorityAndVersion("EUCAST", "2026-2026.1"))
                .thenReturn(java.util.Optional.of(standard));
        new MicroBreakpointServiceImpl(standardDAO, ruleDAO, settings).setDefaultStandard("eu", "42");
        assertEquals("EUCAST", authority.getValue());
        assertEquals("2026-2026.1", edition.getValue());
        assertEquals("42", authority.getSysUserId());
        assertEquals("42", edition.getSysUserId());
        verify(settings).persistData(authority, false);
        verify(settings).persistData(edition, false);
        org.mockito.Mockito.verifyZeroInteractions(ruleDAO);
    }

    @Test
    public void inactiveEditionCannotBeSavedAsDefault() {
        var standard = new MicroBreakpointStandard();
        standard.setLifecycleStatus("LOADED");
        when(standardDAO.get("eu")).thenReturn(java.util.Optional.of(standard));
        try {
            new MicroBreakpointServiceImpl(standardDAO, ruleDAO, settings).setDefaultStandard("eu", "42");
            org.junit.Assert.fail("Activate before choosing a default");
        } catch (IllegalArgumentException expected) {
            assertEquals("AST_BREAKPOINT_STANDARD_NOT_AVAILABLE", expected.getMessage());
        }
        org.mockito.Mockito.verifyZeroInteractions(settings);
    }

    @Test
    public void getActiveStandardUsesAuthorityAndVersion() {
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        when(standardDAO.getActiveStandard("CLSI", "2026")).thenReturn(standard);

        MicroBreakpointService service = new MicroBreakpointServiceImpl(standardDAO, ruleDAO, settings);

        org.junit.Assert.assertTrue(standard == service.getActiveStandard("CLSI", "2026"));
    }

    @Test
    public void findBreakpointRuleDelegatesTheMatchingDimensions() {
        MicroBreakpointRule rule = new MicroBreakpointRule();
        when(ruleDAO.findBestRule("std", "org", "Enterobacterales", "abx", "MIC", "7", "MIC")).thenReturn(rule);

        MicroBreakpointService service = new MicroBreakpointServiceImpl(standardDAO, ruleDAO, settings);

        org.junit.Assert.assertTrue(
                rule == service.findBreakpointRule("std", "org", "Enterobacterales", "abx", "MIC", "7", "MIC"));
        verify(ruleDAO).findBestRule("std", "org", "Enterobacterales", "abx", "MIC", "7", "MIC");
    }

    @Test
    public void getActiveStandardsListsSelectableStandardsForAstRunSetup() {
        MicroBreakpointStandard clsi = new MicroBreakpointStandard();
        MicroBreakpointStandard eucast = new MicroBreakpointStandard();
        when(standardDAO.getActiveStandards()).thenReturn(List.of(clsi, eucast));

        MicroBreakpointService service = new MicroBreakpointServiceImpl(standardDAO, ruleDAO, settings);

        assertEquals(List.of(clsi, eucast), service.getActiveStandards());
    }
}
