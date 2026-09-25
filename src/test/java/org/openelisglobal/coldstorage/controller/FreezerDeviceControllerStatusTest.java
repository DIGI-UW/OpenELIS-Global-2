package org.openelisglobal.coldstorage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.controller.FreezerDeviceController.FreezerStatusResponse;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ThresholdProfileService;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

public class FreezerDeviceControllerStatusTest extends BaseWebContextSensitiveTest {

    @Autowired
    private FreezerDeviceController controller;

    @Autowired
    private FreezerService freezerService;

    @Autowired
    private FreezerReadingService freezerReadingService;

    @Autowired
    private ThresholdProfileService thresholdProfileService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/freezer.xml");
    }

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

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getCurrentStatus_showsTheProfileActiveNowNotTheOneAtTheLatestReading() throws Exception {
        executeDataSetWithStateManagement("testdata/threshold_evaluation.xml");
        resyncSequence("clinlims.freezer_threshold_profile_seq", "clinlims.freezer_threshold_profile");
        resyncSequence("clinlims.freezer_reading_seq", "clinlims.freezer_reading");
        OffsetDateTime latestReadingAt = OffsetDateTime.now().minusDays(2);
        freezerReadingService.saveReading(freezerService.requireFreezer(100L), latestReadingAt, new BigDecimal("-80.0"),
                null, null, FreezerReading.Status.NORMAL, true, null);
        thresholdProfileService.assignProfile(100L, 101L, latestReadingAt.plusDays(1), null, false);

        performGet("/rest/coldstorage/status").andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.freezerId == 100)].thresholdProfileName").value("Standard Freezer Profile"));
    }
}
