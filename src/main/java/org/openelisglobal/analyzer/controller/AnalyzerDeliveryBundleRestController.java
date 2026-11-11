package org.openelisglobal.analyzer.controller;

import org.openelisglobal.analyzerimport.service.AnalyzerDeliveryBundleService;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only view of what the Bridge parsed and delivered. */
@RestController
@RequestMapping("/rest/analyzer/deliveries")
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYSER_IMPORT')")
public class AnalyzerDeliveryBundleRestController extends BaseRestController {

    private final AnalyzerDeliveryBundleService bundles;

    public AnalyzerDeliveryBundleRestController(AnalyzerDeliveryBundleService bundles) {
        this.bundles = bundles;
    }

    @GetMapping(value = "/{id}/bundle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> bundle(@PathVariable String id) {
        return bundles.getBundle(id).map(json -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
