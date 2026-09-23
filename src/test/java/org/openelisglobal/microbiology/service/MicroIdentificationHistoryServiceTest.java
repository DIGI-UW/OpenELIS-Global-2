package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
import org.openelisglobal.microbiology.dao.MicroCaseAmendmentDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateIdentificationEventDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroIdentificationEventType;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;

@RunWith(MockitoJUnitRunner.class)
public class MicroIdentificationHistoryServiceTest {

    @Mock
    private MicroIsolateIdentificationEventDAO eventDAO;

    @Mock
    private MicroCaseAmendmentDAO amendmentDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    private MicroIdentificationHistoryService service;

    @Before
    public void setUp() {
        service = new MicroIdentificationHistoryServiceImpl(eventDAO, amendmentDAO, isolateDAO);
    }

    @Test
    public void amendmentReidentificationRequiresReasonAndCapturesBeforeAfter() {
        MicroCaseAmendment amendment = new MicroCaseAmendment();
        amendment.setId("amendment-1");
        when(amendmentDAO.getOpenByCaseId("case-1")).thenReturn(amendment);
        MicroIsolate previous = isolate("org-old", "Escherichia coli");
        MicroIsolate updated = isolate("org-new", "Klebsiella pneumoniae");

        try {
            service.recordChange(previous, updated, " ", "9");
            fail("Expected a reason for amendment re-identification");
        } catch (IllegalArgumentException expected) {
            assertEquals("REIDENTIFICATION_REASON_REQUIRED", expected.getMessage());
        }

        service.recordChange(previous, updated, "Confirmatory identification", "9");

        ArgumentCaptor<MicroIsolateIdentificationEvent> captor = ArgumentCaptor
                .forClass(MicroIsolateIdentificationEvent.class);
        verify(eventDAO).insert(captor.capture());
        assertEquals(MicroIdentificationEventType.REIDENTIFIED.name(), captor.getValue().getEventType());
        assertEquals("org-old", captor.getValue().getPreviousOrganismId());
        assertEquals("org-new", captor.getValue().getNewOrganismId());
        assertEquals("amendment-1", captor.getValue().getAmendmentId());
        assertEquals("9", captor.getValue().getChangedBy());
    }

    @Test
    public void cancellationRestoresPriorIdentificationAndAppendsRevertEvent() {
        MicroIsolateIdentificationEvent change = new MicroIsolateIdentificationEvent();
        change.setIsolateId("iso-1");
        change.setAmendmentId("amendment-1");
        change.setEventType(MicroIdentificationEventType.REIDENTIFIED.name());
        change.setPreviousOrganismId("org-old");
        change.setPreviousOrganismText("Escherichia coli");
        change.setNewOrganismId("org-new");
        change.setNewOrganismText("Klebsiella pneumoniae");
        when(eventDAO.getByAmendmentId("amendment-1")).thenReturn(List.of(change));
        MicroIsolate current = isolate("org-new", "Klebsiella pneumoniae");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(current));

        service.revertAmendment("amendment-1", "Cancelled correction", "9");

        assertEquals("org-old", current.getOrganismId());
        assertEquals("Escherichia coli", current.getPreliminaryOrganismText());
        verify(isolateDAO).update(current);
        ArgumentCaptor<MicroIsolateIdentificationEvent> captor = ArgumentCaptor
                .forClass(MicroIsolateIdentificationEvent.class);
        verify(eventDAO).insert(captor.capture());
        assertEquals(MicroIdentificationEventType.AMENDMENT_REVERTED.name(), captor.getValue().getEventType());
    }

    private MicroIsolate isolate(String organismId, String organismText) {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setOrganismId(organismId);
        isolate.setPreliminaryOrganismText(organismText);
        isolate.setIdentificationStatus("CONFIRMED");
        return isolate;
    }
}
