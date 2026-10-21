package org.openelisglobal.microbiology;

import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.Test;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.microbiology.service.*;

public class MicroIncomingDeliveryListenerTest {
    private AnalyzerResults row(String component, String specimen) {
        var row = new AnalyzerResults();
        row.setSourceConnectionId("connection");
        row.setSourceMessageId("delivery");
        row.setInstrumentSpecimenId(specimen);
        row.setTestId("test");
        row.setComponentId(component);
        row.setRawTestCode("raw-" + component);
        row.setRawSubIdentity("sub-" + component);
        return row;
    }

    @Test
    public void componentsWithDifferentRawCodesAreDispatchedTogether() {
        var adapter = mock(MicroIncomingAnalyzerAdapter.class);
        var listener = new MicroIncomingDeliveryListener(adapter);
        var first = row("first", "tube");
        var second = row("second", "tube");
        var other = row("third", "other");
        listener.receive(new MicroIncomingDelivery(List.of(first, second, other), "actor"));
        verify(adapter).route(List.of(first, second), "actor");
        verify(adapter).route(List.of(other), "actor");
        verifyNoMoreInteractions(adapter);
    }

    @Test
    public void missingInstrumentSpecimenUsesTheAccessionWithoutMergingDifferentOrders() {
        var adapter = mock(MicroIncomingAnalyzerAdapter.class);
        var first = row("first", null);
        first.setAccessionNumber("one");
        var second = row("second", null);
        second.setAccessionNumber("two");
        new MicroIncomingDeliveryListener(adapter).receive(new MicroIncomingDelivery(List.of(first, second), "actor"));
        verify(adapter).route(List.of(first), "actor");
        verify(adapter).route(List.of(second), "actor");
        verifyNoMoreInteractions(adapter);
    }
}
