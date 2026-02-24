package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.eqa.service.EQALabProgramEnrollmentService;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentTestMap;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/eqa/my-programs")
public class EQAMyProgramsRestController extends ControllerUtills {

    @Autowired
    private EQALabProgramEnrollmentService enrollmentService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> listMyPrograms() {
        List<EQALabProgramEnrollment> enrollments = enrollmentService.findAll();
        List<Map<String, Object>> dtos = enrollments.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMyProgram(@PathVariable Long id) {
        try {
            EQALabProgramEnrollment enrollment = enrollmentService.get(id);
            return ResponseEntity.ok(toDto(enrollment));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createMyProgram(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            String programName = (String) body.get("programName");
            String provider = (String) body.get("provider");

            if (programName == null || programName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Program name is required"));
            }
            if (provider == null || provider.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Provider is required"));
            }

            EQALabProgramEnrollment enrollment = new EQALabProgramEnrollment();
            enrollment.setProgramName(programName);
            enrollment.setProvider(provider);
            enrollment.setDescription((String) body.get("description"));
            enrollment.setIsActive(body.get("isActive") != null ? (Boolean) body.get("isActive") : true);
            enrollment.setSysUserId(getSysUserId(request));

            List<Long> labUnitIds = toLongList(body.get("labUnitIds"));
            List<Long> testIds = toLongList(body.get("testIds"));
            List<Long> panelIds = toLongList(body.get("panelIds"));

            EQALabProgramEnrollment created = enrollmentService.createEnrollment(enrollment, labUnitIds, testIds,
                    panelIds);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMyProgram(HttpServletRequest request, @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            String programName = (String) body.get("programName");
            String provider = (String) body.get("provider");

            if (programName == null || programName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Program name is required"));
            }
            if (provider == null || provider.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Provider is required"));
            }

            EQALabProgramEnrollment updated = new EQALabProgramEnrollment();
            updated.setProgramName(programName);
            updated.setProvider(provider);
            updated.setDescription((String) body.get("description"));
            updated.setIsActive(body.get("isActive") != null ? (Boolean) body.get("isActive") : true);
            updated.setSysUserId(getSysUserId(request));

            List<Long> labUnitIds = toLongList(body.get("labUnitIds"));
            List<Long> testIds = toLongList(body.get("testIds"));
            List<Long> panelIds = toLongList(body.get("panelIds"));

            EQALabProgramEnrollment result = enrollmentService.updateEnrollment(id, updated, labUnitIds, testIds,
                    panelIds);
            return ResponseEntity.ok(toDto(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteMyProgram(@PathVariable Long id) {
        try {
            enrollmentService.softDelete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getProviders() {
        return ResponseEntity.ok(enrollmentService.getDistinctProviders());
    }

    private Map<String, Object> toDto(EQALabProgramEnrollment enrollment) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", enrollment.getId());
        dto.put("programName", enrollment.getProgramName());
        dto.put("provider", enrollment.getProvider());
        dto.put("description", enrollment.getDescription());
        dto.put("isActive", enrollment.getIsActive());
        dto.put("createdDate", enrollment.getCreatedDate());
        dto.put("lastModified", enrollment.getLastModified());

        List<Map<String, Object>> labUnits = enrollment.getLabUnits().stream().map(lu -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", lu.getTestSectionId());
            return m;
        }).collect(Collectors.toList());
        dto.put("labUnits", labUnits);

        List<Map<String, Object>> tests = new ArrayList<>();
        List<Map<String, Object>> panels = new ArrayList<>();
        for (EQALabEnrollmentTestMap tm : enrollment.getTestMaps()) {
            if (tm.getTestId() != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", tm.getTestId());
                tests.add(m);
            }
            if (tm.getPanelId() != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", tm.getPanelId());
                panels.add(m);
            }
        }
        dto.put("tests", tests);
        dto.put("panels", panels);

        return dto;
    }

    @SuppressWarnings("unchecked")
    private List<Long> toLongList(Object obj) {
        if (obj == null) {
            return null;
        }
        List<Number> numbers = (List<Number>) obj;
        return numbers.stream().map(Number::longValue).collect(Collectors.toList());
    }
}
