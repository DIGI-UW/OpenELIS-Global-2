package org.openelisglobal.dataexchange.fhir.controller;

import org.apache.http.impl.client.CloseableHttpClient;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
public class InternalFhirApi {

    @Autowired
    CloseableHttpClient httpClient;

    @Autowired
    FhirApiWorkflowService fhirApiWorkflowService;

    private static final String[] ALLOWED_FIELDS = new String[] { "resourceType" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @PutMapping(value = "/{resourceType}/**")
    public ResponseEntity<String> receiveFhirRequest(@PathVariable("resourceType") ResourceType resourceType) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "receiveFhirRequest",
                "received notification for resource of type: " + resourceType);
        fhirApiWorkflowService.processWorkflow(resourceType);

        return ResponseEntity.ok("");
    }
}
