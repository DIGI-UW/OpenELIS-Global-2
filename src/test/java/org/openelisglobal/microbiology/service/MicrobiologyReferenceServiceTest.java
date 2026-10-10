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
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDAO;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDefaultDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;

@RunWith(MockitoJUnitRunner.class)
public class MicrobiologyReferenceServiceTest {

    @Mock
    private MicroOrganismDAO organismDAO;

    @Mock
    private MicroAntibioticDAO antibioticDAO;

    @Mock
    private MicroAstPanelDAO astPanelDAO;

    @Mock
    private MicroPatientOriginDAO patientOriginDAO;

    @Mock
    private MicroPatientOriginDefaultDAO patientOriginDefaultDAO;

    @Test
    public void getActiveOrganismsUsesReferenceDao() {
        MicroOrganism organism = new MicroOrganism();
        when(organismDAO.getActiveOrganisms()).thenReturn(List.of(organism));

        MicrobiologyReferenceService service = service();

        org.junit.Assert.assertTrue(organism == service.getActiveOrganisms().get(0));
    }

    @Test
    public void getActiveAstPanelsPassesOrganismGroup() {
        MicroAstPanel panel = new MicroAstPanel();
        when(astPanelDAO.getActivePanelsByOrganismGroup("Enterobacterales")).thenReturn(List.of(panel));

        MicrobiologyReferenceService service = service();

        org.junit.Assert.assertTrue(panel == service.getActiveAstPanels("Enterobacterales").get(0));
        verify(astPanelDAO).getActivePanelsByOrganismGroup("Enterobacterales");
    }

    @Test
    public void getPatientOriginsReturnsActiveVocabularyAndConfiguredDefault() {
        MicroPatientOrigin inpatient = new MicroPatientOrigin();
        inpatient.setId("origin-1");
        inpatient.setCode("INPATIENT");
        when(patientOriginDAO.getActivePatientOrigins()).thenReturn(List.of(inpatient));
        when(patientOriginDefaultDAO.findPatientOriginIdByOrganizationId("27")).thenReturn("origin-1");

        MicroPatientOriginOptions result = service().getPatientOrigins("27");

        org.junit.Assert.assertEquals(1, result.getOptions().size());
        org.junit.Assert.assertEquals("INPATIENT", result.getDefaultCode());
    }

    @Test
    public void isActivePatientOriginCodeDelegatesStableCodeValidation() {
        when(patientOriginDAO.existsActiveCode("INPATIENT")).thenReturn(true);

        org.junit.Assert.assertTrue(service().isActivePatientOriginCode("INPATIENT"));
    }

    private MicrobiologyReferenceService service() {
        return new MicrobiologyReferenceServiceImpl(organismDAO, antibioticDAO, astPanelDAO, patientOriginDAO,
                patientOriginDefaultDAO);
    }
}
