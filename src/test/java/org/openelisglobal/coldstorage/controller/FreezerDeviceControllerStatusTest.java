package org.openelisglobal.coldstorage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.controller.FreezerDeviceController.FreezerStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

public class FreezerDeviceControllerStatusTest extends BaseWebContextSensitiveTest {

    @Autowired
    private FreezerDeviceController controller;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/freezer.xml");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetCurrentStatus_DerivesStaleCutoffFromConfiguredPollInterval() {
        List<FreezerStatusResponse> statuses = controller.getCurrentStatus(null, null);

        assertFalse("fixture should expose at least one active freezer", statuses.isEmpty());
        for (FreezerStatusResponse status : statuses) {
            assertEquals("cutoff for " + status.getFreezerName() + " should be 3 x the PT5M poll interval",
                    Long.valueOf(900L), status.getStaleAfterSeconds());
        }
    }
}
