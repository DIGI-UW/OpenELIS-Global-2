package org.openelisglobal.dataexchange.fhir.controller;

import java.util.UUID;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Form definitions used by Generic Sample Order; clinical reads use the scoped
 * FHIR facade.
 */
@RestController
@RequestMapping("/rest/fhir")
@PreAuthorize("isAuthenticated()")
public class FhirQueryRestController {

    @Autowired
    private QuestionnaireStorageService questionnaireStorageService;

    @GetMapping(value = "/Questionnaire/{resourceId}", produces = { MediaType.APPLICATION_JSON_VALUE,
            "application/fhir+json" })
    public ResponseEntity<Questionnaire> getQuestionnaire(@PathVariable("resourceId") UUID resourceId) {
        return ResponseEntity.of(questionnaireStorageService.getQuestionnaire(resourceId));
    }
}
