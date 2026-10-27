package org.openelisglobal.delivery.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.delivery.dao.DeliveryQueueSearch;
import org.openelisglobal.delivery.dto.DeliveryChannelDTO;
import org.openelisglobal.delivery.dto.SentMessageDTO;
import org.openelisglobal.delivery.dto.SentMessagesPageDTO;
import org.openelisglobal.delivery.service.DeliveryChannelService;
import org.openelisglobal.delivery.service.DeliveryChannelService.TestOutcome;
import org.openelisglobal.delivery.service.DeliveryOutboxService;
import org.openelisglobal.delivery.service.DeliverySettings;
import org.openelisglobal.delivery.service.DeliverySettingsService;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Channel;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.OverallStatus;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

/**
 * Result Delivery foundation: the channels and lab-wide settings an
 * administrator edits, and the Sent Messages list that report users read and
 * resend from.
 */
@RestController
@RequestMapping("/rest/delivery")
public class DeliveryRestController {

    public static class TestRequest {
        private String address;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    @Autowired
    private DeliveryChannelService channelService;
    @Autowired
    private DeliverySettingsService settingsService;
    @Autowired
    private DeliveryOutboxService outbox;

    // ----- channels (administrators)

    @GetMapping("/channels")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DeliveryChannelDTO> channels() {
        return channelService.list();
    }

    @GetMapping("/channels/{channel}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryChannelDTO> channel(@PathVariable String channel) {
        Channel parsed = parseChannel(channel);
        if (parsed == null) {
            return ResponseEntity.notFound().build();
        }
        return channelService.get(parsed).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/channels/{channel}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveChannel(HttpServletRequest request, @PathVariable String channel,
            @RequestBody DeliveryChannelDTO wanted) {
        Channel parsed = parseChannel(channel);
        if (parsed == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok(channelService.save(parsed, wanted, ControllerUtills.getSysUserId(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("field", e.getMessage()));
        }
    }

    @PostMapping("/channels/{channel}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testChannel(HttpServletRequest request, @PathVariable String channel,
            @RequestBody TestRequest body) {
        Channel parsed = parseChannel(channel);
        if (parsed == null) {
            return ResponseEntity.notFound().build();
        }
        if (body == null || GenericValidator.isBlankOrNull(body.getAddress())) {
            return ResponseEntity.badRequest().body(Map.of("field", "address"));
        }
        TestOutcome outcome = channelService.sendTest(parsed, body.getAddress().trim(),
                ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok(Map.of("success", outcome.success(), "message", outcome.message(), "sentMessageId",
                outcome.sentMessageId()));
    }

    // ----- settings (administrators)

    @GetMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public DeliverySettings settings() {
        return settingsService.get();
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveSettings(HttpServletRequest request, @RequestBody DeliverySettings wanted) {
        try {
            return ResponseEntity.ok(settingsService.save(wanted, ControllerUtills.getSysUserId(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("field", e.getMessage()));
        }
    }

    // ----- sent messages (report users and administrators)

    @GetMapping("/messages")
    @PreAuthorize("hasAnyRole('REPORTS', 'ADMIN')")
    public SentMessagesPageDTO messages(@RequestParam(required = false) String product,
            @RequestParam(required = false) String status, @RequestParam(required = false) String channel,
            @RequestParam(required = false) String eventCode, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to, @RequestParam(required = false) String labNo,
            @RequestParam(required = false) String recipient, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        DeliveryQueueSearch search = new DeliveryQueueSearch();
        search.setProduct(parseEnum(Product.class, product));
        search.setStatus(parseEnum(OverallStatus.class, status));
        search.setChannel(parseEnum(NotificationChannel.class, channel));
        search.setEventCode(eventCode);
        search.setFrom(parseDate(from, false));
        search.setTo(parseDate(to, true));
        search.setLabNumber(labNo);
        search.setRecipient(recipient);
        return outbox.list(search, page, pageSize);
    }

    @GetMapping("/messages/{id}")
    @PreAuthorize("hasAnyRole('REPORTS', 'ADMIN')")
    public ResponseEntity<SentMessageDTO> message(@PathVariable Long id) {
        return outbox.detail(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/messages/{id}/resend")
    @PreAuthorize("hasAnyRole('REPORTS', 'ADMIN')")
    public ResponseEntity<?> resend(HttpServletRequest request, @PathVariable Long id) {
        try {
            NotificationLog queued = outbox.resend(id, ControllerUtills.getSysUserId(request));
            return outbox.detail(queued.getId()).<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ----- helpers

    private static Channel parseChannel(String raw) {
        return parseEnum(Channel.class, raw);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
        if (GenericValidator.isBlankOrNull(raw)) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * A date (yyyy-mm-dd) as the start of that day, or the start of the next day
     * for an upper bound.
     */
    private static java.time.OffsetDateTime parseDate(String raw, boolean upperBound) {
        if (GenericValidator.isBlankOrNull(raw)) {
            return null;
        }
        try {
            LocalDate day = LocalDate.parse(raw.trim());
            return (upperBound ? day.plusDays(1) : day).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
