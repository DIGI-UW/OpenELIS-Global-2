package org.openelisglobal.locationreporting.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.locationreporting.form.LocationReportingForm;
import org.openelisglobal.locationreporting.service.LocationReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = LocationReportingController.class)
public class LocationReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationReportingService locationReportingService;

    @Test
	public void getSettings_whenOptedIn_shouldReturnFormWithOptInTrue() throws Exception {
		when(locationReportingService.isOptedIn()).thenReturn(true);

		mockMvc.perform(get("/rest/locationreporting")).andExpect(status().isOk())
				.andExpect(jsonPath("$.optIn").value(true));

		verify(locationReportingService).isOptedIn();
		verifyNoMoreInteractions(locationReportingService);
	}

    @Test
	public void getSettings_whenOptedOut_shouldReturnFormWithOptInFalse() throws Exception {
		when(locationReportingService.isOptedIn()).thenReturn(false);

		mockMvc.perform(get("/rest/locationreporting")).andExpect(status().isOk())
				.andExpect(jsonPath("$.optIn").value(false));

		verify(locationReportingService).isOptedIn();
		verifyNoMoreInteractions(locationReportingService);
	}

    @Test
    public void updateSettings_withOptInTrue_shouldEnableReporting() throws Exception {
        LocationReportingForm form = new LocationReportingForm();
        form.setOptIn(true);

        mockMvc.perform(
                post("/rest/locationreporting").contentType(MediaType.APPLICATION_JSON).content("{\"optIn\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.optIn").value(true));

        verify(locationReportingService).setOptIn(true);
    }

    @Test
    public void updateSettings_withOptInFalse_shouldDisableReporting() throws Exception {
        mockMvc.perform(
                post("/rest/locationreporting").contentType(MediaType.APPLICATION_JSON).content("{\"optIn\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.optIn").value(false));

        verify(locationReportingService).setOptIn(false);
    }

    @Test
    public void sendReportNow_shouldNotThrowException() throws Exception {
        mockMvc.perform(post("/rest/locationreporting/send")).andExpect(status().isOk());
        verify(locationReportingService).sendLocationReport();
    }
}
