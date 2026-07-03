package org.openelisglobal.vector.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.vector.service.VectorSpeciesService;
import org.openelisglobal.vector.valueholder.VectorSpecies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/admin/vector/species")
public class VectorSpeciesRestController {

    @Autowired
    private VectorSpeciesService vectorSpeciesService;

    @GetMapping(value = "/lifecycle-stages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, String>>> getLifecycleStages(@RequestParam String sampleTypeId) {
        try {
            List<Dictionary> stages = vectorSpeciesService.getLifecycleStagesBySampleTypeId(sampleTypeId);
            List<Map<String, String>> result = stages.stream().map(d -> {
                String code = d.getLocalAbbreviation();
                if (code == null || code.isBlank()) {
                    code = d.getId();
                }
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("code", code);
                entry.put("label", d.getLocalizedName());
                return entry;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VectorSpecies>> getSpecies(@RequestParam(required = false) String sampleTypeId) {
        try {
            List<VectorSpecies> result = sampleTypeId != null ? vectorSpeciesService.getBySampleTypeId(sampleTypeId)
                    : vectorSpeciesService.getAll();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorSpecies> getSpeciesById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(vectorSpeciesService.get(id));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorSpecies> createSpecies(@RequestBody VectorSpecies species, HttpServletRequest request) {
        try {
            String sampleTypeId = species.getSampleTypeId() != null ? String.valueOf(species.getSampleTypeId()) : null;
            Integer id = vectorSpeciesService.create(species, sampleTypeId, ControllerUtills.getSysUserId(request));
            species.setId(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(species);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorSpecies> updateSpecies(@PathVariable Integer id, @RequestBody VectorSpecies species,
            HttpServletRequest request) {
        try {
            String sampleTypeId = species.getSampleTypeId() != null ? String.valueOf(species.getSampleTypeId()) : null;
            VectorSpecies updated = vectorSpeciesService.patchUpdate(id, species, sampleTypeId,
                    ControllerUtills.getSysUserId(request));
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
