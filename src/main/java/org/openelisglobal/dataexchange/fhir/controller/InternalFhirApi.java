package org.openelisglobal.dataexchange.fhir.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.ResourceType;
import org.json.JSONObject;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
public class InternalFhirApi {

    @Autowired
    CloseableHttpClient httpClient;

    @Autowired
    FhirConfig fhirConfig;

    @Autowired
    FhirApiWorkflowService fhirApiWorkflowService;

    private static final String[] ALLOWED_FIELDS = new String[] { "resourceType" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/**")
    public ResponseEntity<JSONObject> receiveGetFhirRequest(HttpServletRequest request)
            throws ClientProtocolException, IOException {
        String requestString = fhirConfig.getLocalFhirStorePath() + request.getPathInfo() + "?"
                + request.getQueryString();
        HttpGet forwardRequest = new HttpGet(requestString);
        String username = fhirConfig.getUsername();
        String password = fhirConfig.getPassword();
        if (!GenericValidator.isBlankOrNull(username)) {
            String encoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            forwardRequest.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
        }
        System.out.println("forwarding to fhir store: " + forwardRequest.getURI());
        CloseableHttpResponse response = httpClient.execute(forwardRequest);
        System.out.println("response from  " + forwardRequest.getURI());
        return ResponseEntity.status(response.getStatusLine().getStatusCode())
                .body(new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8")));
    }

    @PostMapping(value = "/**")
    public String receivePostFhirRequest(HttpServletRequest request) {
        return "forward:" + request.getServletPath().replaceFirst("fhir", "fhir/facade") + request.getPathInfo() + "?"
                + request.getQueryString();
    }

    @PutMapping(value = "/workflow/{resourceType}/**")
    public ResponseEntity<String> receiveFhirRequest(@PathVariable("resourceType") ResourceType resourceType) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "receiveFhirRequest",
                "received notification for resource of type: " + resourceType);
        fhirApiWorkflowService.processWorkflow(resourceType);

        return ResponseEntity.ok("");
    }
}
