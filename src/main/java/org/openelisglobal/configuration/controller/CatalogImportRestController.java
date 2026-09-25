package org.openelisglobal.configuration.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.configuration.service.CatalogImportService;
import org.openelisglobal.configuration.service.CatalogImportService.ImportPlan;
import org.openelisglobal.configuration.service.UnresolvedReferenceService;
import org.openelisglobal.configuration.valueholder.UnresolvedReference;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The Import Catalog (CSV) page: preview a set of files, apply them, and work
 * through the names the import could not resolve.
 */
@RestController
@RequestMapping("/rest/configuration")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogImportRestController {

    /** One name waiting for a decision, as the panel lists it. */
    public record UnresolvedItem(String id, String referenceType, String referenceValue, String domain, String fileName,
            Integer lineNumber, String context, int occurrences) {
    }

    /** A record the operator can point an unresolved name at. */
    public record Candidate(String id, String name) {
    }

    public record ResolveRequest(String action, String targetId) {
    }

    @Autowired
    private CatalogImportService importService;

    @Autowired
    private UnresolvedReferenceService unresolvedReferenceService;

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @GetMapping(value = "/import/domains", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getDomains() {
        return importService.getImportableDomains();
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportPlan> preview(@RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "domains", required = false) List<String> domains, HttpServletRequest request) {
        return respond(files, domains, request, false);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportPlan> apply(@RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "domains", required = false) List<String> domains, HttpServletRequest request) {
        return respond(files, domains, request, true);
    }

    @GetMapping(value = "/unresolved", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UnresolvedItem> getUnresolved() {
        List<UnresolvedItem> items = new ArrayList<>();
        for (UnresolvedReference reference : unresolvedReferenceService.getOpen()) {
            items.add(new UnresolvedItem(reference.getId(), reference.getReferenceType(), reference.getReferenceValue(),
                    reference.getDomain(), reference.getFileName(), reference.getLineNumber(), reference.getContext(),
                    reference.getOccurrences()));
        }
        return items;
    }

    @GetMapping(value = "/unresolved/candidates", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Candidate> getCandidates(@RequestParam String referenceType) {
        List<Candidate> candidates = new ArrayList<>();
        switch (referenceType) {
        case UnresolvedReference.TYPE_TEST -> {
            for (Test test : testService.getAllActiveTests(false)) {
                candidates.add(new Candidate(test.getId(), test.getDescription()));
            }
        }
        case UnresolvedReference.TYPE_SAMPLE_TYPE -> {
            for (TypeOfSample type : typeOfSampleService.getAllTypeOfSamples()) {
                candidates.add(new Candidate(type.getId(), type.getLocalizedName()));
            }
        }
        case UnresolvedReference.TYPE_TEST_SECTION -> {
            for (TestSection section : testSectionService.getAllTestSections()) {
                candidates.add(new Candidate(section.getId(), section.getTestSectionName()));
            }
        }
        case UnresolvedReference.TYPE_UNIT_OF_MEASURE -> {
            for (UnitOfMeasure unit : unitOfMeasureService.getAll()) {
                candidates.add(new Candidate(unit.getId(), unit.getUnitOfMeasureName()));
            }
        }
        default -> {
            return candidates;
        }
        }
        candidates.sort(Comparator.comparing(Candidate::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return candidates;
    }

    @PostMapping(value = "/unresolved/{id}/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> resolve(@PathVariable String id, @RequestBody ResolveRequest body,
            HttpServletRequest request) {
        try {
            UnresolvedReference resolved = unresolvedReferenceService.resolve(id, body.action(), body.targetId(),
                    ControllerUtills.getSysUserId(request));
            return ResponseEntity.ok(Map.of("id", resolved.getId(), "status", resolved.getStatus()));
        } catch (RuntimeException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", message(e)));
        }
    }

    /**
     * A batch the service could not take (a file it cannot place in a domain) or
     * could not keep (a configuration tree the webapp cannot write to) is a 422
     * whose body carries the reason, so the page can show it instead of an empty
     * result.
     */
    private ResponseEntity<ImportPlan> respond(List<MultipartFile> files, List<String> domains,
            HttpServletRequest request, boolean apply) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        try {
            return ResponseEntity.ok(apply ? importService.apply(files, domains, sysUserId)
                    : importService.preview(files, domains, sysUserId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().body(new ImportPlan(null,
                    List.of(new CatalogImportService.FilePlan(null, null, 0, 0, 0, List.of(), message(e))), 0));
        }
    }

    private static String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
