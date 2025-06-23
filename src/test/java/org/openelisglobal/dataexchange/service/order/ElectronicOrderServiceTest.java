package org.openelisglobal.dataexchange.service.order;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.springframework.beans.factory.annotation.Autowired;

public class ElectronicOrderServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/electronic-order.xml");
    }

    @Test
    public void testGetAllElectronicOrdersOrderedBy() {
        List<ElectronicOrder> electronicOrders = electronicOrderService
                .getAllElectronicOrdersOrderedBy(ElectronicOrder.SortOrder.LAST_UPDATED_DESC);
        assertEquals(3, electronicOrders.size());
        assertEquals("2", electronicOrders.get(0).getId());
    }

}
