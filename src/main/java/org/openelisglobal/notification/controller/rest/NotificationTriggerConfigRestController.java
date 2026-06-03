package org.openelisglobal.notification.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.notification.service.DefaultPayloadTemplates;
import org.openelisglobal.notification.service.EventVariableRegistry;
import org.openelisglobal.notification.service.NotificationLogService;
import org.openelisglobal.notification.service.NotificationPayloadTemplateService;
import org.openelisglobal.notification.service.NotificationTriggerConfigService;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLogChannel;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/rest")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationTriggerConfigRestController {

    @Autowired
    private NotificationTriggerConfigService configService;

    @Autowired
    private NotificationPayloadTemplateService payloadTemplateService;

    @Autowired
    private NotificationLogService notificationLogService;

    @GetMapping("/NotificationTriggerConfig")
    public Map<String, Object> list() {
        List<NotificationTriggerConfig> configs = configService.getAllConfigs();
        Map<String, Object> response = new HashMap<>();
        response.put("triggers", configs.stream().map(this::toDto).collect(Collectors.toList()));
        response.put("availableChannels", Arrays.asList(NotificationChannel.values()));
        response.put("availableRecipientTypes", Arrays.asList(NotificationRecipientType.values()));
        return response;
    }

    @PostMapping("/NotificationTriggerConfig")
    public Map<String, Object> save(@RequestBody SaveRequest body, HttpServletRequest request) {
        String sysUserId = ControllerUtills.getSysUserId(request);
        if (body == null || body.triggers() == null) {
            return Map.of("error", "triggers list is required");
        }
        configService.saveAll(body.triggers(), sysUserId);
        return list();
    }

    /**
     * Returns one DTO per editable payload type — current persisted subject +
     * message plus their factory defaults, plus the merge-field registry and sample
     * preview values. The admin Template editor binds to this shape; keeping
     * defaults/variables server-side keeps the runtime and the editor in lockstep.
     */
    @GetMapping("/NotificationTriggerConfig/templates")
    public Map<String, Object> listTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        for (NotificationPayloadType type : editableTypes()) {
            templates.add(toTemplateDto(type));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("templates", templates);
        response.put("channels", Arrays.asList(NotificationChannel.values()));
        return response;
    }

    @PutMapping("/NotificationTriggerConfig/templates/{type}")
    public Map<String, Object> saveTemplate(@PathVariable("type") NotificationPayloadType type,
            @RequestBody TemplateSaveRequest body, HttpServletRequest request) {
        if (!DefaultPayloadTemplates.hasDefault(type)) {
            return Map.of("error", "templates for " + type + " are not editable");
        }
        if (body == null) {
            return Map.of("error", "request body is required");
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        NotificationPayloadTemplate existing = payloadTemplateService.getSystemDefaultPayloadTemplateForType(type);
        if (existing == null) {
            return Map.of("error", "no template row exists for " + type + "; expected a Liquibase seed");
        }
        existing.setSubjectTemplate(body.subjectTemplate() == null ? "" : body.subjectTemplate());
        existing.setMessageTemplate(body.messageTemplate() == null ? "" : body.messageTemplate());
        payloadTemplateService.updatePayloadTemplateMessagesAndSubject(existing, sysUserId);
        return toTemplateDto(type);
    }

    @GetMapping("/NotificationTriggerConfig/sent-messages")
    public Map<String, Object> listSentMessages(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "eventCode", required = false) String eventCode,
            @RequestParam(value = "status", required = false) String status) {
        Optional<String> eventCodeFilter = Optional.ofNullable(eventCode).filter(s -> !s.isBlank());
        Optional<String> statusFilter = Optional.ofNullable(status).filter(s -> !s.isBlank());
        List<NotificationLog> items = notificationLogService.findPage(eventCodeFilter, statusFilter, page, size);
        long total = notificationLogService.countMatching(eventCodeFilter, statusFilter);

        Map<String, Object> response = new HashMap<>();
        response.put("messages", items.stream().map(this::toLogListDto).collect(Collectors.toList()));
        response.put("totalItems", total);
        response.put("page", page);
        response.put("size", size);
        return response;
    }

    @GetMapping("/NotificationTriggerConfig/sent-messages/{id}")
    public Map<String, Object> getSentMessage(@PathVariable("id") Long id) {
        NotificationLog log = notificationLogService.get(id);
        if (log == null) {
            return Map.of("error", "notification log " + id + " not found");
        }
        return toLogDetailDto(log);
    }

    @PostMapping("/NotificationTriggerConfig/sent-messages/{id}/resend")
    public ResponseEntity<Map<String, Object>> resendSentMessage(@PathVariable("id") Long id,
            HttpServletRequest request) {
        String sysUserId = ControllerUtills.getSysUserId(request);
        try {
            NotificationLog resend = notificationLogService.resend(id, sysUserId);
            return ResponseEntity.ok(toLogDetailDto(resend));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toLogListDto(NotificationLog log) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", log.getId());
        dto.put("eventCode", log.getEventCode());
        dto.put("firedAt", log.getFiredAt() == null ? null : log.getFiredAt().toString());
        dto.put("triggeredByUserId", log.getTriggeredByUserId());
        dto.put("recipientType", log.getRecipientType());
        dto.put("recipientDisplayName", log.getRecipientDisplayName());
        dto.put("recipientEmail", log.getRecipientEmail());
        dto.put("recipientPhone", log.getRecipientPhone());
        dto.put("referenceAccession", log.getReferenceAccession());
        dto.put("referenceWorkflow", log.getReferenceWorkflow());
        dto.put("overallStatus", log.getOverallStatus());
        dto.put("resentFromId", log.getResentFromId());
        dto.put("channels", log.getChannels().stream().map(this::toChannelDto).collect(Collectors.toList()));
        return dto;
    }

    private Map<String, Object> toLogDetailDto(NotificationLog log) {
        Map<String, Object> dto = toLogListDto(log);
        dto.put("renderedSubject", log.getRenderedSubject());
        dto.put("renderedMessage", log.getRenderedMessage());
        return dto;
    }

    private Map<String, Object> toChannelDto(NotificationLogChannel ch) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("channel", ch.getChannel());
        dto.put("status", ch.getStatus());
        dto.put("errorMessage", ch.getErrorMessage());
        dto.put("attempts", ch.getAttempts());
        dto.put("lastAttemptedAt", ch.getLastAttemptedAt() == null ? null : ch.getLastAttemptedAt().toString());
        return dto;
    }

    private List<NotificationPayloadType> editableTypes() {
        List<NotificationPayloadType> types = new ArrayList<>();
        for (NotificationPayloadType type : NotificationPayloadType.values()) {
            if (DefaultPayloadTemplates.hasDefault(type)) {
                types.add(type);
            }
        }
        return types;
    }

    private Map<String, Object> toTemplateDto(NotificationPayloadType type) {
        NotificationPayloadTemplate persisted = payloadTemplateService.getSystemDefaultPayloadTemplateForType(type);
        DefaultPayloadTemplates.Default factory = DefaultPayloadTemplates.forType(type);

        Map<String, Object> dto = new HashMap<>();
        dto.put("type", type.name());
        dto.put("subjectTemplate",
                persisted == null ? factory.getSubject() : nullToEmpty(persisted.getSubjectTemplate()));
        dto.put("messageTemplate",
                persisted == null ? factory.getMessage() : nullToEmpty(persisted.getMessageTemplate()));
        dto.put("defaultSubject", factory.getSubject());
        dto.put("defaultMessage", factory.getMessage());
        dto.put("availableVariables", EventVariableRegistry.variablesFor(type).stream().map(v -> {
            Map<String, String> m = new HashMap<>();
            m.put("token", v.getToken());
            m.put("displayLabel", v.getDisplayLabel());
            m.put("description", v.getDescription());
            return m;
        }).collect(Collectors.toList()));
        dto.put("sampleValues", EventVariableRegistry.sampleValuesFor(type));
        return dto;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private Map<String, Object> toDto(NotificationTriggerConfig cfg) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", cfg.getId());
        dto.put("eventCode", cfg.getEventCode());
        dto.put("enabled", cfg.isEnabled());
        dto.put("channels", cfg.getChannels());
        dto.put("recipientTypes", cfg.getRecipientTypes());
        dto.put("payloadTemplateId", cfg.getPayloadTemplate() == null ? null : cfg.getPayloadTemplate().getId());
        return dto;
    }

    public record SaveRequest(List<NotificationTriggerConfig> triggers) {
    }

    public record TemplateSaveRequest(String subjectTemplate, String messageTemplate) {
    }
}
