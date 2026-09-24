package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroCultureSetupDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

@RunWith(MockitoJUnitRunner.class)
public class MicrobiologyConfigurationServiceTest {

    @Mock
    private MicroOrganismDAO organismDAO;
    @Mock
    private MicroAntibioticDAO antibioticDAO;
    @Mock
    private MicroAstPanelDAO panelDAO;
    @Mock
    private MicroAstPanelAntibioticDAO panelAntibioticDAO;
    @Mock
    private MicroBreakpointStandardDAO standardDAO;
    @Mock
    private MicroBreakpointRuleDAO ruleDAO;
    @Mock
    private MicroCultureSetupDAO cultureSetupDAO;

    private MicrobiologyConfigurationService service;

    @Before
    public void setUp() {
        service = new MicrobiologyConfigurationServiceImpl(organismDAO, antibioticDAO, panelDAO, panelAntibioticDAO,
                standardDAO, ruleDAO, cultureSetupDAO);
    }

    @Test
    public void getOrCreateOrganismReusesAndReactivatesReferenceData() {
        MicroOrganism existing = new MicroOrganism();
        existing.setId("organism-1");
        existing.setIsActive("N");
        when(organismDAO.getAllMatching(Map.of("whonetCode", "ECOUAT"))).thenReturn(List.of(existing));
        when(organismDAO.update(existing)).thenReturn(existing);

        MicroOrganism result = service.getOrCreateOrganism("Escherichia coli (UAT)", "ECOUAT", "panel-1");

        assertSame(existing, result);
        assertEquals("Y", result.getIsActive());
        assertEquals("panel-1", result.getDefaultAstPanelId());
        verify(organismDAO).update(existing);
    }

    @Test
    public void getOrCreateOrganismCreatesMissingReferenceData() {
        when(organismDAO.getAllMatching(Map.of("whonetCode", "ECOUAT"))).thenReturn(List.of());

        MicroOrganism result = service.getOrCreateOrganism("Escherichia coli (UAT)", "ECOUAT", "panel-1");

        assertEquals("Escherichia coli (UAT)", result.getDisplayName());
        assertEquals("ECOUAT", result.getWhonetCode());
        assertEquals("panel-1", result.getDefaultAstPanelId());
        verify(organismDAO).insert(result);
    }
}
