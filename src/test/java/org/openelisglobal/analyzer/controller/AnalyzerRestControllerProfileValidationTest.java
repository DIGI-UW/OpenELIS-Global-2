package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class AnalyzerRestControllerProfileValidationTest {

    @Test
    public void configuredAnalyzerRequiresAnExactBridgeProfileRevision() {
        AnalyzerForm form = new AnalyzerForm();
        form.setName("Hematology bench 1");
        form.setAnalyzerType("HEMATOLOGY");

        ResponseEntity<Map<String, Object>> response = new AnalyzerRestController().createAnalyzer(form,
                new MockHttpServletRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) response.getBody().get("validationErrors");
        assertTrue(errors.contains("Profile ID and profile revision are required"));
    }
}
