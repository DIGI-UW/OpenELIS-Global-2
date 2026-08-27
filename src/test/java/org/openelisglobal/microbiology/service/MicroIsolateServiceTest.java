package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;

@RunWith(MockitoJUnitRunner.class)
public class MicroIsolateServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    private MicroIsolateService service;

    @Before
    public void setUp() {
        service = new MicroIsolateServiceImpl(caseDAO, isolateDAO, activityDAO);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(mutableCase()));
    }

    @Test
    public void createIsolateRequiresCaseAndRecordsActivity() {
        MicroIsolate isolate = service.createIsolate("case-1", "ISO-1", "Gram negative rod", "Lactose fermenting",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1");

        assertEquals("case-1", isolate.getCaseId());
        assertEquals("ISO-1", isolate.getIsolateLabel());
        assertEquals("Gram negative rod", isolate.getGramStain());
        assertEquals("Lactose fermenting", isolate.getColonyMorphology());
        assertEquals(MicroIsolateIdentificationStatus.PRELIMINARY.name(), isolate.getIdentificationStatus());
        verify(isolateDAO).insert(isolate);
        verify(activityDAO).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void createIsolateMovesPositiveWorkIntoIdentification() {
        MicroCase microCase = mutableCase();
        microCase.setStage(MicroCaseStage.POSITIVE_SIGNAL.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));

        service.createIsolate("case-1", "ISO-1", "Gram negative rod", null,
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1");

        assertEquals(MicroCaseStage.IDENTIFICATION.name(), microCase.getStage());
        verify(caseDAO).update(microCase);
    }

    @Test
    public void updateIdentificationPreservesCaseActivityTrail() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setIsolateLabel("ISO-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        when(isolateDAO.update(isolate)).thenReturn(isolate);

        MicroIsolate updated = service.updateIdentification("iso-1", "org-1", "E. coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "MALDI-TOF", new BigDecimal("98"), "1");

        assertEquals("org-1", updated.getOrganismId());
        assertEquals("MALDI-TOF", updated.getIdentificationMethod());
        assertEquals(new BigDecimal("98"), updated.getIdentificationConfidence());
        assertEquals(MicroIsolateIdentificationStatus.CONFIRMED.name(), updated.getIdentificationStatus());
        verify(activityDAO).insert(any(MicroCaseActivity.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateIdentificationRejectsOutOfRangeConfidence() {
        service.updateIdentification("iso-1", "org-1", "E. coli", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT,
                MicroIsolateIdentificationStatus.CONFIRMED, "MALDI-TOF", new BigDecimal("101"), "1");
    }

    @Test(expected = IllegalStateException.class)
    public void createIsolateRejectsFinalReleasedCases() {
        MicroCase finalCase = mutableCase();
        finalCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        finalCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(finalCase));

        service.createIsolate("case-1", "ISO-1", "Gram negative rod", null,
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1");
    }

    private MicroCase mutableCase() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        return microCase;
    }
}
