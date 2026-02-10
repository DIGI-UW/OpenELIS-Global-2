package org.openelisglobal.eqa.controller.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/eqa/programs")
public class EQAProgramRestController {

    @Autowired
    private EQAProgramService programService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EQA Coordinator')")
    public ResponseEntity<?> createProgram(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String description = (String) body.get("description");

            if (name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Program name is required"));
            }

            EQAProgram program = new EQAProgram();
            program.setName(name);
            program.setDescription(description);
            program.setIsActive(true);

            Long id = programService.insert(program);
            program = programService.get(id);
            return ResponseEntity.ok(toProgramDto(program));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> listPrograms(@RequestParam(required = false) Boolean activeOnly) {
        List<EQAProgram> programs;
        if (Boolean.TRUE.equals(activeOnly)) {
            programs = programService.findActivePrograms();
        } else {
            programs = programService.getAll();
        }

        List<Map<String, Object>> dtos = programs.stream().map(this::toProgramDto).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProgram(@PathVariable Long id) {
        try {
            EQAProgram program = programService.get(id);
            return ResponseEntity.ok(toProgramDto(program));
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EQA Coordinator')")
    public ResponseEntity<?> updateProgram(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            EQAProgram program = programService.get(id);

            if (body.containsKey("name")) {
                String name = (String) body.get("name");
                if (name == null || name.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Program name cannot be empty"));
                }
                program.setName(name);
            }

            if (body.containsKey("description")) {
                program.setDescription((String) body.get("description"));
            }

            if (body.containsKey("isActive")) {
                Boolean isActive = (Boolean) body.get("isActive");
                if (Boolean.FALSE.equals(isActive)) {
                    programService.deactivateProgram(id);
                } else {
                    programService.activateProgram(id);
                }
                program = programService.get(id);
            } else {
                program = programService.update(program);
            }

            return ResponseEntity.ok(toProgramDto(program));
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/{id}/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getTestAssignments(@PathVariable Long id) {
        try {
            programService.get(id);

            List<EQAProgramTest> tests = programService.getTestAssignments(id);
            List<Map<String, Object>> dtos = tests.stream().map(this::toTestAssignmentDto).collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/{id}/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EQA Coordinator')")
    public ResponseEntity<?> updateTestAssignments(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            programService.get(id);

            @SuppressWarnings("unchecked")
            List<Number> testIds = (List<Number>) body.get("testIds");
            if (testIds == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "testIds list is required"));
            }

            List<EQAProgramTest> existing = programService.getTestAssignments(id);
            for (EQAProgramTest pt : existing) {
                programService.removeTestAssignment(pt.getId());
            }

            for (Number testId : testIds) {
                programService.assignTest(id, testId.longValue());
            }

            List<EQAProgramTest> updated = programService.getTestAssignments(id);
            List<Map<String, Object>> dtos = updated.stream().map(this::toTestAssignmentDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toProgramDto(EQAProgram program) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", program.getId());
        dto.put("name", program.getName());
        dto.put("description", program.getDescription());
        dto.put("isActive", program.getIsActive());
        dto.put("fhirUuid", program.getFhirUuid() != null ? program.getFhirUuid().toString() : null);
        return dto;
    }

    private Map<String, Object> toTestAssignmentDto(EQAProgramTest pt) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", pt.getId());
        dto.put("testId", pt.getTestId());
        dto.put("isActive", pt.getIsActive());
        return dto;
    }
}
