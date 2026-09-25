package org.openelisglobal.coldstorage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        super.setUp();
        executeDataSetWithStateManagement("testdata/freezer.xml");
    }

    /**
     * Humidity alerting reads the assigned threshold profile, which no screen
     * showed, so an operator could not see what humidity a device was alerting on
     * (issue #4261).
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    public void getCurrentStatus_exposesTheAssignedProfilesHumidityBands() throws Exception {
        executeDataSetWithStateManagement("testdata/threshold_evaluation.xml");

        performGet("/rest/coldstorage/status").andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.freezerId == 100)].thresholdProfileName").value("Ultra-Low Freezer Profile"))
                .andExpect(jsonPath("$[?(@.freezerId == 100)].humidityWarningMin").value(30.0))
                .andExpect(jsonPath("$[?(@.freezerId == 100)].humidityWarningMax").value(70.0));
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
