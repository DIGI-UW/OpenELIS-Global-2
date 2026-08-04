package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroCultureSetupDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroOrganismAdminForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

@RunWith(MockitoJUnitRunner.class)
public class MicrobiologyReferenceAdminServiceTest {

    @Mock
    private MicroOrganismDAO organismDAO;
    @Mock
    private MicroAntibioticDAO antibioticDAO;
    @Mock
    private MicroAstPanelDAO panelDAO;
    @Mock
    private MicroAstPanelAntibioticDAO panelAntibioticDAO;
    @Mock
    private MicroCultureSetupDAO cultureSetupDAO;
    @Mock
    private MethodService methodService;

    private MicrobiologyReferenceAdminService service;

    @Before
    public void setUp() {
        service = new MicrobiologyReferenceAdminServiceImpl(organismDAO, antibioticDAO, panelDAO, panelAntibioticDAO,
                cultureSetupDAO, methodService);
    }

    @Test
    public void saveOrganismNormalizesWhonetCodeAndUsesServerActor() {
        when(organismDAO.findByDisplayNameIgnoreCase("Escherichia coli")).thenReturn(Optional.empty());
        when(organismDAO.findByWhonetCodeIgnoreCase("eco")).thenReturn(Optional.empty());
        MicroOrganismAdminForm request = organism(" Escherichia coli ", " ECO ");

        MicroOrganismAdminForm saved = service.saveOrganism(null, request, "42");

        ArgumentCaptor<MicroOrganism> captor = ArgumentCaptor.forClass(MicroOrganism.class);
        verify(organismDAO).insert(captor.capture());
        assertEquals("Escherichia coli", captor.getValue().getDisplayName());
        assertEquals("eco", captor.getValue().getWhonetCode());
        assertEquals("42", captor.getValue().getLastUpdatedBy());
        assertEquals("eco", saved.whonetCode);
    }

    @Test(expected = MicroReferenceConflictException.class)
    public void saveOrganismRejectsDuplicateActiveWhonetCode() {
        MicroOrganism existing = new MicroOrganism();
        existing.setId("existing");
        existing.setIsActive("Y");
        when(organismDAO.findByDisplayNameIgnoreCase("Escherichia coli")).thenReturn(Optional.empty());
        when(organismDAO.findByWhonetCodeIgnoreCase("eco")).thenReturn(Optional.of(existing));

        service.saveOrganism(null, organism("Escherichia coli", "eco"), "42");
    }

    @Test
    public void deactivationPreservesRecordAndRecordsImpactActor() {
        MicroOrganism existing = new MicroOrganism();
        existing.setId("organism-1");
        existing.setDisplayName("Escherichia coli");
        existing.setWhonetCode("eco");
        when(organismDAO.get("organism-1")).thenReturn(Optional.of(existing));
        when(organismDAO.countWorkflowReferences("organism-1")).thenReturn(3L);

        MicroOrganismAdminForm result = service.setOrganismActive("organism-1", false, "77");

        assertEquals("N", existing.getIsActive());
        assertEquals("77", existing.getLastUpdatedBy());
        assertEquals(3L, result.referenceCount);
        verify(organismDAO).update(existing);
        verify(organismDAO, never()).delete(any());
    }

    @Test
    public void saveAntibioticNormalizesUppercaseWhonetCode() {
        when(antibioticDAO.findByDisplayNameIgnoreCase("Ciprofloxacin")).thenReturn(Optional.empty());
        when(antibioticDAO.findByWhonetCodeIgnoreCase("CIP")).thenReturn(Optional.empty());
        MicroAntibioticAdminForm request = new MicroAntibioticAdminForm();
        request.displayName = " Ciprofloxacin ";
        request.whonetCode = " cip ";
        request.antibioticClass = "Fluoroquinolone";
        request.route = "BOTH";

        MicroAntibioticAdminForm result = service.saveAntibiotic(null, request, "42");

        ArgumentCaptor<MicroAntibiotic> captor = ArgumentCaptor.forClass(MicroAntibiotic.class);
        verify(antibioticDAO).insert(captor.capture());
        assertEquals("CIP", captor.getValue().getWhonetCode());
        assertEquals("BOTH", captor.getValue().getRoute());
        assertEquals("CIP", result.whonetCode);
    }

    @Test
    public void publishingPanelCreatesNewImmutableVersionWithOrderedRows() {
        MicroAstPanel existing = new MicroAstPanel();
        existing.setId("panel-v1");
        existing.setLogicalKey("panel-family");
        existing.setVersionNumber(1);
        existing.setName("Gram negative panel");
        existing.setWorkflowType("BACTERIOLOGY");
        when(panelDAO.get("panel-v1")).thenReturn(Optional.of(existing));
        when(panelDAO.findCurrentByLogicalKey("panel-family")).thenReturn(existing);
        MicroAntibiotic cip = antibiotic("cip", "CIP");
        MicroAntibiotic gen = antibiotic("gen", "GEN");
        when(antibioticDAO.get("cip")).thenReturn(Optional.of(cip));
        when(antibioticDAO.get("gen")).thenReturn(Optional.of(gen));

        MicroAstPanelAdminForm request = new MicroAstPanelAdminForm();
        request.name = "Gram negative panel";
        request.workflowType = "BACTERIOLOGY";
        request.antibiotics = List.of(panelAntibiotic("gen", 1, "ALWAYS"), panelAntibiotic("cip", 2, "CASCADE"));

        MicroAstPanelAdminForm result = service.publishPanelVersion("panel-v1", request, "42");

        ArgumentCaptor<MicroAstPanel> panelCaptor = ArgumentCaptor.forClass(MicroAstPanel.class);
        verify(panelDAO).insert(panelCaptor.capture());
        MicroAstPanel published = panelCaptor.getValue();
        assertEquals(2, published.getVersionNumber().intValue());
        assertEquals("panel-family", published.getLogicalKey());
        assertEquals("panel-v1", published.getSupersedesPanelId());
        assertNotEquals(existing.getId(), published.getId());
        assertEquals("N", existing.getIsCurrent());
        assertEquals(2, result.versionNumber.intValue());

        ArgumentCaptor<MicroAstPanelAntibiotic> rowCaptor = ArgumentCaptor.forClass(MicroAstPanelAntibiotic.class);
        verify(panelAntibioticDAO, org.mockito.Mockito.times(2)).insert(rowCaptor.capture());
        assertEquals(List.of(1, 2),
                rowCaptor.getAllValues().stream().map(MicroAstPanelAntibiotic::getDisplayOrder).toList());
        assertTrue(rowCaptor.getAllValues().stream().allMatch(row -> published.getId().equals(row.getPanelId())));
    }

    private MicroOrganismAdminForm organism(String name, String code) {
        MicroOrganismAdminForm form = new MicroOrganismAdminForm();
        form.displayName = name;
        form.whonetCode = code;
        form.organismGroup = "Enterobacterales";
        form.initialSignificance = "USUALLY";
        return form;
    }

    private MicroAntibiotic antibiotic(String id, String code) {
        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setId(id);
        antibiotic.setDisplayName(code);
        antibiotic.setWhonetCode(code);
        antibiotic.setIsActive("Y");
        return antibiotic;
    }

    private MicroAstPanelAntibioticAdminForm panelAntibiotic(String id, int tier, String behavior) {
        MicroAstPanelAntibioticAdminForm form = new MicroAstPanelAntibioticAdminForm();
        form.antibioticId = id;
        form.tier = tier;
        form.reportBehavior = behavior;
        return form;
    }
}
