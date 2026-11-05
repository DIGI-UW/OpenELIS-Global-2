package org.openelisglobal.microbiology;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.service.MicroIsolateWorkspaceServiceImpl;
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@RunWith(MockitoJUnitRunner.class)
public class MicroIsolateLockOrderTest {
    @Mock
    private MicroCaseDAO cases;
    @Mock
    private MicrobiologyCaseAccessService access;
    @Mock
    private SampleItemService sampleItems;
    @InjectMocks
    private MicroIsolateWorkspaceServiceImpl workspace;

    @Test
    public void locksOrderBeforeCaseLikeRouting() {
        verifyLocks(false);
    }

    @Test
    public void legacyCaseUsesItsSourceOrderBeforeLockingCase() {
        verifyLocks(true);
    }

    private void verifyLocks(boolean legacy) {
        var c = new MicroCase();
        c.setId("case");
        c.setLabUnitId("unit");
        if (legacy) {
            c.setSampleItemId("item");
            var sample = new Sample();
            sample.setId("order");
            var item = new SampleItem();
            item.setSample(sample);
            when(sampleItems.get("item")).thenReturn(item);
        } else {
            c.setSampleId("order");
        }
        when(cases.get("case")).thenReturn(Optional.of(c));
        when(cases.getForUpdate("case")).thenReturn(c);
        when(access.hasLabUnitRole("actor", "unit", Constants.ROLE_RESULTS)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> workspace.pick("case", null, "actor"));
        var order = inOrder(cases);
        order.verify(cases).lockOrder("order");
        order.verify(cases).getForUpdate("case");
    }
}
