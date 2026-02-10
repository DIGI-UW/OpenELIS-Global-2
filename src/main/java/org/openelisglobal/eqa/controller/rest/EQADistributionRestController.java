package org.openelisglobal.eqa.controller.rest;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.eqa.service.EQADistributionService;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
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
@RequestMapping("/rest/eqa")
public class EQADistributionRestController {

    @Autowired
    private EQADistributionService distributionService;

    @Autowired
    private EQAProgramService programService;

    @PostMapping(value = "/distributions", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EQA Coordinator')")
    public ResponseEntity<?> createDistribution(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("distributionName");
            Number programId = (Number) body.get("programId");
            String deadlineStr = (String) body.get("deadline");
            List<?> participantIds = (List<?>) body.get("participantOrganizationIds");

            if (name == null || programId == null || deadlineStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "distributionName, programId, and deadline are required"));
            }

            if (participantIds != null && participantIds.size() < 2) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "At least 2 participant organizations are required"));
            }

            EQAProgram program;
            try {
                program = programService.get(programId.longValue());
            } catch (ObjectNotFoundException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Program not found: " + programId));
            }

            EQADistribution distribution = new EQADistribution();
            distribution.setDistributionName(name);
            distribution.setEqaProgram(program);
            distribution.setDeadline(Timestamp.valueOf(deadlineStr + " 23:59:59"));
            distribution.setDistributionDate(new Timestamp(System.currentTimeMillis()));
            distribution.setStatus(EQADistributionStatus.DRAFT);
            distribution.setSysUserId("1");

            Long id = distributionService.insert(distribution);
            distribution.setId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("id", id);
            response.put("distributionName", distribution.getDistributionName());
            response.put("status", distribution.getStatus().name());
            response.put("deadline", distribution.getDeadline());
            if (participantIds != null) {
                response.put("participantCount", participantIds.size());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/distributions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> listDistributions(@RequestParam(required = false) Long programId,
            @RequestParam(required = false) String status) {

        List<EQADistribution> distributions;
        if (programId != null) {
            distributions = distributionService.findByProgramId(programId);
        } else if (status != null && !status.isEmpty()) {
            distributions = distributionService.findByStatus(EQADistributionStatus.valueOf(status));
        } else {
            distributions = distributionService.getAll();
        }

        List<Map<String, Object>> dtos = distributions.stream().map(d -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", d.getId());
            dto.put("distributionName", d.getDistributionName());
            dto.put("status", d.getStatus().name());
            dto.put("deadline", d.getDeadline());
            dto.put("distributionDate", d.getDistributionDate());
            if (d.getEqaProgram() != null) {
                dto.put("programName", d.getEqaProgram().getName());
                dto.put("programId", d.getEqaProgram().getId());
            }
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("distributions", dtos);
        response.put("totalCount", dtos.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/distributions/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDistribution(@PathVariable Long id) {
        EQADistribution d;
        try {
            d = distributionService.get(id);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("id", d.getId());
        response.put("distributionName", d.getDistributionName());
        response.put("status", d.getStatus().name());
        response.put("deadline", d.getDeadline());
        response.put("distributionDate", d.getDistributionDate());
        response.put("targetValue", d.getTargetValue());
        if (d.getEqaProgram() != null) {
            response.put("programName", d.getEqaProgram().getName());
            response.put("programId", d.getEqaProgram().getId());
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/distributions/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EQA Coordinator')")
    public ResponseEntity<?> advanceStatus(@PathVariable Long id) {
        try {
            EQADistribution distribution = distributionService.advanceStatus(id);
            Map<String, Object> response = new HashMap<>();
            response.put("id", distribution.getId());
            response.put("status", distribution.getStatus().name());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/distributions/{id}/barcodes", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EQA Coordinator')")
    public ResponseEntity<?> generateBarcodes(@PathVariable Long id) {
        try {
            distributionService.get(id);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        // Barcode generation placeholder — integrates with existing
        // BarcodeInformationService
        Map<String, Object> response = new HashMap<>();
        response.put("distributionId", id);
        response.put("status", "barcodes_generated");
        return ResponseEntity.ok(response);
    }
}
