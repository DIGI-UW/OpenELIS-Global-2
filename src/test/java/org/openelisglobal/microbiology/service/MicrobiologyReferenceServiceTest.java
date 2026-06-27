package org.openelisglobal.microbiology.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroCultureSetupDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

@RunWith(MockitoJUnitRunner.class)
public class MicrobiologyReferenceServiceTest {

    @Mock
    private MicroOrganismDAO organismDAO;

    @Mock
    private MicroAntibioticDAO antibioticDAO;

    @Mock
    private MicroAstPanelDAO astPanelDAO;

    @Mock
    private MicroCultureSetupDAO cultureSetupDAO;

    @Test
    public void getActiveOrganismsUsesReferenceDao() {
        MicroOrganism organism = new MicroOrganism();
        when(organismDAO.getActiveOrganisms()).thenReturn(List.of(organism));

        MicrobiologyReferenceService service = service();

        org.junit.Assert.assertTrue(organism == service.getActiveOrganisms().get(0));
    }

    @Test
    public void getActiveAstPanelsPassesWorkflowName() {
        MicroAstPanel panel = new MicroAstPanel();
        when(astPanelDAO.getActivePanelsByWorkflowType("BACTERIOLOGY")).thenReturn(List.of(panel));

        MicrobiologyReferenceService service = service();

        org.junit.Assert.assertTrue(panel == service.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).get(0));
        verify(astPanelDAO).getActivePanelsByWorkflowType("BACTERIOLOGY");
    }

    @Test
    public void getActiveCultureSetupForMethodPassesWorkflowName() {
        MicroCultureSetup setup = new MicroCultureSetup();
        when(cultureSetupDAO.getActiveSetupForMethod("12", "MYCOBACTERIOLOGY_TB")).thenReturn(setup);

        MicrobiologyReferenceService service = service();

        org.junit.Assert.assertTrue(
                setup == service.getActiveCultureSetupForMethod("12", MicroWorkflowType.MYCOBACTERIOLOGY_TB));
    }

    private MicrobiologyReferenceService service() {
        return new MicrobiologyReferenceServiceImpl(organismDAO, antibioticDAO, astPanelDAO, cultureSetupDAO);
    }
}
