package org.openelisglobal.coldstorage.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import org.openelisglobal.coldstorage.service.ThresholdProfileService;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({ "/rest/coldstorage", "/rest/freezer-monitoring" })
public class FreezerThresholdController extends BaseRestController {

    private final ThresholdProfileService thresholdProfileService;

    public FreezerThresholdController(ThresholdProfileService thresholdProfileService) {
        this.thresholdProfileService = thresholdProfileService;
    }

    @GetMapping("/thresholds")
    public List<ThresholdProfileResponse> listThresholds() {
        return thresholdProfileService.listProfiles().stream().map(ThresholdProfileResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/thresholds")
    public ThresholdProfileResponse createThreshold(@RequestBody @Valid CreateThresholdProfileRequest request) {
        ThresholdProfile profile = request.toEntity();
        String username = resolveCurrentUser();
        ThresholdProfile created = thresholdProfileService.createProfile(profile, username);
        return ThresholdProfileResponse.from(created);
    }

    @PostMapping("/{freezerId}/thresholds/{profileId}/assign")
    public ResponseEntity<FreezerThresholdAssignmentResponse> assignThreshold(@PathVariable Long freezerId,
            @PathVariable Long profileId, @RequestBody(required = false) AssignThresholdRequest request) {
        OffsetDateTime effectiveStart = request != null ? request.getEffectiveStart() : null;
        OffsetDateTime effectiveEnd = request != null ? request.getEffectiveEnd() : null;
        boolean isDefault = request != null && Boolean.TRUE.equals(request.getIsDefault());
        var assignment = thresholdProfileService.assignProfile(freezerId, profileId, effectiveStart, effectiveEnd,
                isDefault);
        return ResponseEntity.ok(FreezerThresholdAssignmentResponse.from(assignment));
    }

    private String resolveCurrentUser() {
        org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }

    @Data
    public static class ThresholdProfileResponse {
        private Long id;
        private String name;
        private BigDecimal warningMin;
        private BigDecimal warningMax;
        private BigDecimal criticalMin;
        private BigDecimal criticalMax;
        private BigDecimal humidityWarningMin;
        private BigDecimal humidityWarningMax;
        private BigDecimal humidityCriticalMin;
        private BigDecimal humidityCriticalMax;
        private Integer minExcursionMinutes;
        private Integer maxDurationMinutes;

        public static ThresholdProfileResponse from(ThresholdProfile profile) {
            ThresholdProfileResponse response = new ThresholdProfileResponse();
            response.setId(profile.getId());
            response.setName(profile.getName());
            response.setWarningMin(profile.getWarningMin());
            response.setWarningMax(profile.getWarningMax());
            response.setCriticalMin(profile.getCriticalMin());
            response.setCriticalMax(profile.getCriticalMax());
            response.setHumidityWarningMin(profile.getHumidityWarningMin());
            response.setHumidityWarningMax(profile.getHumidityWarningMax());
            response.setHumidityCriticalMin(profile.getHumidityCriticalMin());
            response.setHumidityCriticalMax(profile.getHumidityCriticalMax());
            response.setMinExcursionMinutes(profile.getMinExcursionMinutes());
            response.setMaxDurationMinutes(profile.getMaxDurationMinutes());
            return response;
        }
    }

    @Data
    public static class CreateThresholdProfileRequest {
        @NotBlank
        private String name;
        private String description;
        private BigDecimal warningMin;
        private BigDecimal warningMax;
        private BigDecimal criticalMin;
        private BigDecimal criticalMax;
        private BigDecimal humidityWarningMin;
        private BigDecimal humidityWarningMax;
        private BigDecimal humidityCriticalMin;
        private BigDecimal humidityCriticalMax;
        @Min(0)
        @Max(180)
        private Integer minExcursionMinutes;
        private Integer maxDurationMinutes;

        public ThresholdProfile toEntity() {
            ThresholdProfile profile = new ThresholdProfile();
            profile.setName(name);
            profile.setDescription(description);
            profile.setWarningMin(warningMin);
            profile.setWarningMax(warningMax);
            profile.setCriticalMin(criticalMin);
            profile.setCriticalMax(criticalMax);
            profile.setHumidityWarningMin(humidityWarningMin);
            profile.setHumidityWarningMax(humidityWarningMax);
            profile.setHumidityCriticalMin(humidityCriticalMin);
            profile.setHumidityCriticalMax(humidityCriticalMax);
            profile.setMinExcursionMinutes(minExcursionMinutes != null ? minExcursionMinutes : 0);
            profile.setMaxDurationMinutes(maxDurationMinutes);
            profile.setCreatedAt(OffsetDateTime.now());
            return profile;
        }
    }

    @Data
    public static class AssignThresholdRequest {
        private OffsetDateTime effectiveStart;
        private OffsetDateTime effectiveEnd;
        private Boolean isDefault;
    }

    @Data
    public static class FreezerThresholdAssignmentResponse {
        private Long id;
        private Long freezerId;
        private Long profileId;
        private OffsetDateTime effectiveStart;
        private OffsetDateTime effectiveEnd;
        private Boolean isDefault;

        public static FreezerThresholdAssignmentResponse from(
                org.openelisglobal.coldstorage.valueholder.FreezerThresholdProfile assignment) {
            FreezerThresholdAssignmentResponse response = new FreezerThresholdAssignmentResponse();
            response.setId(assignment.getId());
            response.setFreezerId(assignment.getFreezer().getId());
            response.setProfileId(assignment.getThresholdProfile().getId());
            response.setEffectiveStart(assignment.getEffectiveStart());
            response.setEffectiveEnd(assignment.getEffectiveEnd());
            response.setIsDefault(assignment.getIsDefault());
            return response;
        }
    }
}
