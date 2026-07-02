package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroWhonetReadinessForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

@RunWith(MockitoJUnitRunner.class)
public class MicroWhonetReadinessServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroAstRunDAO astRunDAO;

    @Mock
    private MicroAstReadingDAO astReadingDAO;

    @Mock
    private MicroOrganismDAO organismDAO;

    @Mock
    private MicroAntibioticDAO antibioticDAO;

    private MicroWhonetReadinessService service;

    @Before
    public void setUp() {
        service = new MicroWhonetReadinessServiceImpl(caseDAO, isolateDAO, astRunDAO, astReadingDAO, organismDAO,
                antibioticDAO);
    }

    @Test
    public void missingOrganismMappingBlocksWhonetReadiness() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));

        MicroWhonetReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.whonetReady);
        assertTrue(readiness.blockers.contains("ORGANISM_MAPPING_REQUIRED"));
    }

    @Test
    public void mappedIsolateWithAstReadingIsWhonetReady() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setOrganismId("org-1");
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        MicroAstReading reading = new MicroAstReading();
        reading.setAntibioticId("abx-1");
        MicroOrganism organism = new MicroOrganism();
        organism.setWhonetCode("eco");
        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setWhonetCode("cip");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));
        when(astReadingDAO.getByRunId("run-1")).thenReturn(List.of(reading));
        when(antibioticDAO.get("abx-1")).thenReturn(Optional.of(antibiotic));

        MicroWhonetReadinessForm readiness = service.getReadiness("case-1");

        assertTrue(readiness.whonetReady);
        assertTrue(readiness.blockers.isEmpty());
    }

    @Test
    public void organismWithoutWhonetCodeBlocksWhonetReadiness() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setOrganismId("org-1");
        MicroOrganism organism = new MicroOrganism();
        organism.setWhonetCode("");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism));

        MicroWhonetReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.whonetReady);
        assertTrue(readiness.blockers.contains("ORGANISM_MAPPING_REQUIRED"));
    }

    @Test
    public void antibioticWithoutWhonetCodeBlocksWhonetReadiness() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setOrganismId("org-1");
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        MicroAstReading reading = new MicroAstReading();
        reading.setAntibioticId("abx-1");
        MicroOrganism organism = new MicroOrganism();
        organism.setWhonetCode("eco");
        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setWhonetCode("");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));
        when(astReadingDAO.getByRunId("run-1")).thenReturn(List.of(reading));
        when(antibioticDAO.get("abx-1")).thenReturn(Optional.of(antibiotic));

        MicroWhonetReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.whonetReady);
        assertTrue(readiness.blockers.contains("ANTIBIOTIC_MAPPING_REQUIRED"));
    }
}
