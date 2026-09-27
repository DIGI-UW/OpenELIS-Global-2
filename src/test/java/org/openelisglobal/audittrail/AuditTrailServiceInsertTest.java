package org.openelisglobal.audittrail;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;

@RunWith(MockitoJUnitRunner.class)
public class AuditTrailServiceInsertTest {

    @Mock
    private ReferenceTablesService referenceTablesService;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private AuditTrailServiceImpl auditTrailService;

    @Test
    public void saveNewHistoryReturnsTheDurableHistoryId() {
        ReferenceTables referenceTable = new ReferenceTables();
        referenceTable.setId("7");
        referenceTable.setKeepHistory("Y");
        when(referenceTablesService.getReferenceTableByName(any(ReferenceTables.class))).thenReturn(referenceTable);
        when(historyService.insert(any(History.class))).thenReturn("91");

        AnalyzerSiteBindingConfirmation confirmation = new AnalyzerSiteBindingConfirmation();
        confirmation.setId("71");

        String historyId = auditTrailService.saveNewHistory(confirmation, "17", "analyzer_site_binding_confirmation");

        assertEquals("91", historyId);
        ArgumentCaptor<History> saved = ArgumentCaptor.forClass(History.class);
        verify(historyService).insert(saved.capture());
        assertEquals("71", saved.getValue().getReferenceId());
        assertEquals("17", saved.getValue().getSysUserId());
        assertEquals("7", saved.getValue().getReferenceTable());
    }
}
