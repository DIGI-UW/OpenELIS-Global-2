package org.openelisglobal.eqa.controller.rest;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/eqa/orders")
public class EQAOrdersRestController extends ControllerUtills {

    @Autowired
    private SampleEQAService sampleEQAService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> listOrders(@RequestParam(required = false) String status,
            @RequestParam(required = false) Long programId, @RequestParam(required = false) String priority,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String search) {

        List<SampleEQA> samples = sampleEQAService.findEqaSamples();

        if (programId != null) {
            samples = samples.stream()
                    .filter(s -> s.getEqaProgram() != null && programId.equals(s.getEqaProgram().getId()))
                    .collect(Collectors.toList());
        }

        if (priority != null && !priority.isBlank()) {
            samples = samples.stream()
                    .filter(s -> s.getEqaPriority() != null && priority.equalsIgnoreCase(s.getEqaPriority().name()))
                    .collect(Collectors.toList());
        }

        if (from != null && !from.isBlank()) {
            LocalDate fromDate = LocalDate.parse(from);
            Timestamp fromTs = Timestamp.valueOf(fromDate.atStartOfDay());
            samples = samples.stream().filter(s -> s.getEqaDeadline() != null && !s.getEqaDeadline().before(fromTs))
                    .collect(Collectors.toList());
        }

        if (to != null && !to.isBlank()) {
            LocalDate toDate = LocalDate.parse(to);
            Timestamp toTs = Timestamp.valueOf(toDate.atTime(LocalTime.MAX));
            samples = samples.stream().filter(s -> s.getEqaDeadline() != null && !s.getEqaDeadline().after(toTs))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            samples = samples.stream()
                    .filter(s -> (s.getEqaProviderSampleId() != null
                            && s.getEqaProviderSampleId().toLowerCase().contains(searchLower))
                            || (s.getEqaProgram() != null && s.getEqaProgram().getName() != null
                                    && s.getEqaProgram().getName().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            samples = samples.stream().filter(s -> status.equalsIgnoreCase(deriveStatus(s)))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> dtos = samples.stream().map(this::toOrderDto).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSummary() {
        List<SampleEQA> samples = sampleEQAService.findEqaSamples();

        long pending = samples.stream().filter(s -> "PENDING".equals(deriveStatus(s))).count();
        long inProgress = samples.stream().filter(s -> "IN_PROGRESS".equals(deriveStatus(s))).count();
        long overdue = samples.stream().filter(s -> "OVERDUE".equals(deriveStatus(s))).count();

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Timestamp monthStart = Timestamp.valueOf(startOfMonth);
        long completedThisMonth = samples.stream().filter(s -> "COMPLETED".equals(deriveStatus(s)))
                .filter(s -> s.getLastupdated() != null && !s.getLastupdated().before(monthStart)).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("pending", pending);
        summary.put("inProgress", inProgress);
        summary.put("overdue", overdue);
        summary.put("completedThisMonth", completedThisMonth);

        return ResponseEntity.ok(summary);
    }

    private String deriveStatus(SampleEQA sample) {
        if (sample.getEqaDeadline() != null
                && sample.getEqaDeadline().before(new Timestamp(System.currentTimeMillis()))) {
            return "OVERDUE";
        }
        return "PENDING";
    }

    private Map<String, Object> toOrderDto(SampleEQA sample) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", sample.getId());
        dto.put("sampleId", sample.getSampleId());
        dto.put("labNumber", sample.getEqaProviderSampleId());
        dto.put("programName", sample.getEqaProgram() != null ? sample.getEqaProgram().getName() : null);
        dto.put("providerName", sample.getEqaProgram() != null ? sample.getEqaProgram().getProviderName() : null);
        dto.put("status", deriveStatus(sample));
        dto.put("deadline", sample.getEqaDeadline());
        dto.put("priority", sample.getEqaPriority() != null ? sample.getEqaPriority().name() : null);
        dto.put("dateEntered", sample.getLastupdated());
        return dto;
    }
}
