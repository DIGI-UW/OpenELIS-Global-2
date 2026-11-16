package org.openelisglobal.containertype.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.containertype.form.SampleTypeContainersForm;
import org.openelisglobal.containertype.form.SampleTypeContainersView;
import org.openelisglobal.containertype.form.TestContainersForm;
import org.openelisglobal.containertype.form.TestContainersView;
import org.openelisglobal.containertype.service.ContainerTypeRefusedException;
import org.openelisglobal.containertype.service.TestContainerService;
import org.openelisglobal.containertype.service.TestContainersRefusedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1611 FR-G5 to G7: a test's expected containers in the test editor, and a
 * sample type's default container with the tests it serves.
 */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class TestContainersRestController extends BaseRestController {

    @Autowired
    private TestContainerService testContainerService;

    @GetMapping(value = "/rest/test-catalog/tests/{testId}/containers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestContainersView> forTest(@PathVariable String testId) {
        return ResponseEntity.of(testContainerService.forTest(testId));
    }

    @PutMapping(value = "/rest/test-catalog/tests/{testId}/containers", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TestContainersView save(HttpServletRequest request, @PathVariable String testId,
            @RequestBody TestContainersForm form) {
        return testContainerService.save(testId, form, getSysUserId(request));
    }

    @GetMapping(value = "/rest/sample-type-containers/{sampleTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SampleTypeContainersView> forSampleType(@PathVariable String sampleTypeId) {
        return ResponseEntity.of(testContainerService.forSampleType(sampleTypeId));
    }

    @PutMapping(value = "/rest/sample-type-containers/{sampleTypeId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SampleTypeContainersView setDefaultContainer(@PathVariable String sampleTypeId,
            @RequestBody SampleTypeContainersForm form) {
        return testContainerService.setDefaultContainer(sampleTypeId,
                form == null ? null : form.defaultContainerTypeId());
    }

    @ExceptionHandler(ContainerTypeRefusedException.class)
    public ResponseEntity<Map<String, Object>> refused(ContainerTypeRefusedException refusal) {
        Map<String, Object> body = new HashMap<>();
        body.put("field", refusal.getField());
        body.put("code", refusal.getCode());
        if (refusal instanceof TestContainersRefusedException rowRefusal && rowRefusal.getRow() != null) {
            body.put("row", rowRefusal.getRow());
        }
        int status = "notFound".equals(refusal.getCode()) ? 404 : 400;
        return ResponseEntity.status(status).body(body);
    }
}
