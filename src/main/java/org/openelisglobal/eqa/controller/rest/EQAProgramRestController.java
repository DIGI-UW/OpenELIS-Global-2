package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.eqa.service.EQAProgramEnrollmentService;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
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
@PreAuthorize(EQAGuards.READ)
public class EQAProgramRestController extends ControllerUtills {

    private final EQAProgramService programService;

    private final EQAProgramEnrollmentService enrollmentService;

    private final SystemUserService systemUserService;

    // Constructor injection so the integration suite can drive this controller
    // with the real services, the way EQACycleRestController is exercised.
    @Autowired
    public EQAProgramRestController(EQAProgramService programService, EQAProgramEnrollmentService enrollmentService,
            SystemUserService systemUserService) {
        this.programService = programService;
        this.enrollmentService = enrollmentService;
        this.systemUserService = systemUserService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public ResponseEntity<?> createProgram(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String description = (String) body.get("description");

            if (name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Program name is required"));
            }

            String schemeType = body.get("schemeType") == null ? "" : String.valueOf(body.get("schemeType")).trim();
            if (schemeType.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Scheme type is required"));
            }

            EQAProgram program = new EQAProgram();
            program.setName(name);
            program.setDescription(description);
            program.setSchemeType(schemeTypeOf(schemeType));
            program.setProvider(blankToNull((String) body.get("provider")));
            program.setPerAnalyst(Boolean.TRUE.equals(body.get("perAnalyst")));
            program.setIsActive(true);
            program.setSysUserId(getSysUserId(request));

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
    @PreAuthorize(EQAGuards.PROVIDER)
    public ResponseEntity<?> updateProgram(HttpServletRequest request, @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            EQAProgram program = programService.get(id);
            program.setSysUserId(getSysUserId(request));

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

            if (body.containsKey("provider")) {
                program.setProvider(blankToNull((String) body.get("provider")));
            }

            if (body.containsKey("schemeType")) {
                String schemeType = body.get("schemeType") == null ? "" : String.valueOf(body.get("schemeType")).trim();
                if (schemeType.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Scheme type cannot be empty"));
                }
                program.setSchemeType(schemeTypeOf(schemeType));
            }

            if (body.containsKey("isActive")) {
                Boolean isActive = (Boolean) body.get("isActive");
                program.setIsActive(Boolean.TRUE.equals(isActive));
            }

            if (body.containsKey("perAnalyst")) {
                program.setPerAnalyst(Boolean.TRUE.equals(body.get("perAnalyst")));
            }

            program = programService.update(program);

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
    @PreAuthorize(EQAGuards.PROVIDER)
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

    /**
     * FR-V2.4-03: the scheme's analyst roster, with the display names the wizard's
     * assignment step shows. Rows come back even for a user since deactivated —
     * hiding them would silently drop an assignment already made.
     */
    @GetMapping(value = "/{id}/analysts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAnalysts(@PathVariable Long id) {
        try {
            programService.get(id);
            return ResponseEntity
                    .ok(programService.getAnalysts(id).stream().map(this::toAnalystDto).collect(Collectors.toList()));
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/{id}/analysts", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public ResponseEntity<?> updateAnalysts(HttpServletRequest request, @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            programService.get(id);

            if (!(body.get("systemUserIds") instanceof List<?> rows)) {
                return ResponseEntity.badRequest().body(Map.of("error", "systemUserIds list is required"));
            }
            // Ids arrive as numbers or strings depending on the caller; reading each
            // through String.valueOf is what keeps a JSON type mismatch from 500ing
            // (the same cast bug qa/T-01 fixed on enrollment).
            List<Long> systemUserIds = rows.stream().filter(row -> row != null)
                    .map(row -> Long.valueOf(String.valueOf(row).trim())).collect(Collectors.toList());

            return ResponseEntity.ok(programService.setAnalysts(id, systemUserIds, getSysUserId(request)).stream()
                    .map(this::toAnalystDto).collect(Collectors.toList()));
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toAnalystDto(EQASchemeAnalyst analyst) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", analyst.getId());
        dto.put("systemUserId", analyst.getSystemUserId());
        SystemUser user = systemUserService.get(String.valueOf(analyst.getSystemUserId()));
        dto.put("displayName", user == null ? String.valueOf(analyst.getSystemUserId())
                : (user.getFirstName() == null ? "" : user.getFirstName() + " ") + user.getLastName());
        return dto;
    }

    /**
     * The arrangement type is a user decision, not a default: an in-house scheme is
     * unreachable from the UI while the endpoint ignores it, and a regional or
     * split-sample scheme created without it is silently filed as international. An
     * unknown value is refused by name rather than falling back.
     */
    private EQASchemeType schemeTypeOf(String value) {
        try {
            return EQASchemeType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown scheme type: " + value);
        }
    }

    /**
     * A blank provider is stored as NULL, so the BR-004 check and every reader see
     * "no provider" as one value instead of two.
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> toProgramDto(EQAProgram program) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", program.getId());
        dto.put("name", program.getName());
        dto.put("description", program.getDescription());
        dto.put("provider", program.getProvider());
        // G1's alter-in-place: scheme_type lives on eqa_program, and the in-house
        // wizard filters on it, so it has to reach the client.
        dto.put("schemeType", program.getSchemeType() == null ? null : program.getSchemeType().name());
        dto.put("isActive", program.getIsActive());
        // FR-V2.3-04: result entry reads this to decide whether to show the
        // Analyst column, so the scheme list has to carry it.
        dto.put("perAnalyst", Boolean.TRUE.equals(program.getPerAnalyst()));
        dto.put("fhirUuid", program.getFhirUuid() != null ? program.getFhirUuid().toString() : null);
        dto.put("participantCount", enrollmentService.countActiveEnrollments(program.getId()));
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
