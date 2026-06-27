package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    }

    @Test
    public void createIsolateRequiresCaseAndRecordsActivity() {
        when(caseDAO.get("case-1")).thenReturn(Optional.of(new MicroCase()));

        MicroIsolate isolate = service.createIsolate("case-1", "ISO-1", "org-1", "E. coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1");

        assertEquals("case-1", isolate.getCaseId());
        assertEquals("ISO-1", isolate.getIsolateLabel());
        assertEquals(MicroIsolateIdentificationStatus.PRELIMINARY.name(), isolate.getIdentificationStatus());
        verify(isolateDAO).insert(isolate);
        verify(activityDAO).insert(any(MicroCaseActivity.class));
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
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED, "1");

        assertEquals("org-1", updated.getOrganismId());
        assertEquals(MicroIsolateIdentificationStatus.CONFIRMED.name(), updated.getIdentificationStatus());
        verify(activityDAO).insert(any(MicroCaseActivity.class));
    }
}
