package org.openelisglobal.vector.controller.rest;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO.SampleDomain;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/admin/vector/sample-types")
public class VectorSampleTypeRestController {

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TypeOfSample>> getAllSampleTypes() {
        try {
            return ResponseEntity.ok(typeOfSampleService.getTypesForDomain(SampleDomain.VECTOR));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TypeOfSample>> getActiveSampleTypes() {
        try {
            List<TypeOfSample> active = typeOfSampleService.getTypesForDomain(SampleDomain.VECTOR).stream()
                    .filter(TypeOfSample::isActive).collect(Collectors.toList());
            return ResponseEntity.ok(active);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
