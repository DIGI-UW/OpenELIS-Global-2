package org.openelisglobal.coldstorage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.config.ControllerSetup;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ControllerSetup is registered first on purpose: with equal precedence the
 * winner is registration order, and the 409 case would answer 500.
 */
public class FreezerMonitoringExceptionHandlerPrecedenceTest {

    private static final String VALIDATION_MESSAGE = "Device code FRZ-1 is already in use";
    private static final String DAO_MESSAGE = "Error in StorageDeviceDAOImpl insert";

    @RestController
    static class ColdStorageThrowingController {

        @GetMapping("/coldstorage/devices")
        public String create() {
            throw new LIMSRuntimeException(VALIDATION_MESSAGE);
        }

        @GetMapping("/coldstorage/devices/broken")
        public String hibernateFault() {
            throw new LIMSRuntimeException(DAO_MESSAGE, new IllegalStateException("connection reset"));
        }
    }

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ColdStorageThrowingController())
                .setControllerAdvice(new ControllerSetup(), new FreezerMonitoringExceptionHandler()).build();
    }

    @Test
    public void coldStorageValidationFailure_answers409WithTheReason() throws Exception {
        mockMvc.perform(get("/coldstorage/devices")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("storage_validation_error"))
                .andExpect(jsonPath("$.message").value(VALIDATION_MESSAGE));
    }

    @Test
    public void wrappedInfrastructureFailure_answers500WithoutTheInternalMessage() throws Exception {
        mockMvc.perform(get("/coldstorage/devices/broken")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal_error"))
                .andExpect(jsonPath("$.message").value("Internal Server Error"));
    }
}
