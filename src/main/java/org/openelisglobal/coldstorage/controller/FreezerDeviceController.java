package org.openelisglobal.coldstorage.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ThresholdEvaluationService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({ "/rest/coldstorage", "/rest/freezer-monitoring" })
@SuppressWarnings("unused")
public class FreezerDeviceController extends BaseRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreezerDeviceController.class);

    private final FreezerService freezerService;
    private final FreezerReadingService freezerReadingService;
    private final ThresholdEvaluationService thresholdEvaluationService;
    private final StorageLocationService storageLocationService;
    private final SystemUserService systemUserService;
    private final DepartmentIsolationService departmentIsolationService;

    public FreezerDeviceController(FreezerService freezerService, FreezerReadingService freezerReadingService,
            ThresholdEvaluationService thresholdEvaluationService, StorageLocationService storageLocationService,
            SystemUserService systemUserService, DepartmentIsolationService departmentIsolationService) {
        this.freezerService = freezerService;
        this.freezerReadingService = freezerReadingService;
        this.thresholdEvaluationService = thresholdEvaluationService;
        this.storageLocationService = storageLocationService;
        this.systemUserService = systemUserService;
        this.departmentIsolationService = departmentIsolationService;
    }

    @GetMapping("/status")
    public List<FreezerStatusResponse> getCurrentStatus(
            @RequestParam(name = "roomId", required = false) Long roomFilter,
            @RequestParam(name = "status", required = false) FreezerReading.Status statusFilter,
            HttpServletRequest request) {
        return freezerService.getActiveFreezers().stream()
                .filter(freezer -> canAccessFreezer(freezer, request))
                .filter(freezer -> roomFilter == null || (freezer.getStorageRoom() != null
                        && roomFilter.equals(freezer.getStorageRoom().getId().longValue())))
                .map(this::toStatusResponse)
                .filter(response -> statusFilter == null || response.getStatus() == statusFilter)
                .collect(Collectors.toList());
    }

    @GetMapping("/id/{freezerId}/readings")
    public ResponseEntity<List<SensorReadingResponse>> getReadings(@PathVariable Long freezerId,
            @RequestParam OffsetDateTime start, @RequestParam OffsetDateTime end, HttpServletRequest request) {
        Freezer freezer = freezerService.requireFreezer(freezerId);
        if (!canAccessFreezer(freezer, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return freezerReadingService.getReadingsBetween(freezerId, start, end).stream().map(SensorReadingResponse::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), ResponseEntity::ok));
    }

    @GetMapping("/{name}/latest")
    public ResponseEntity<SensorReadingResponse> getLatestByName(@PathVariable String name, HttpServletRequest request) {
        return freezerService.findByName(name)
                .filter(freezer -> canAccessFreezer(freezer, request))
                .flatMap(freezer -> freezerReadingService.getLatestReading(freezer.getId()))
                .map(SensorReadingResponse::from).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{name}/recent")
    public ResponseEntity<List<SensorReadingResponse>> getRecentByName(@PathVariable String name,
            @RequestParam(defaultValue = "10") @Min(1) @Max(250) int limit, HttpServletRequest request) {
        return freezerService.findByName(name)
                .filter(freezer -> canAccessFreezer(freezer, request))
                .map(freezer -> ResponseEntity.ok(freezerReadingService.getRecentReadings(freezer.getId(), limit)
                        .stream().map(SensorReadingResponse::from).collect(Collectors.toList())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/devices")
    public List<Freezer> listDevices(@RequestParam(name = "search", required = false) String search,
            HttpServletRequest request) {
        return freezerService.getAllFreezers(search).stream().filter(freezer -> canAccessFreezer(freezer, request))
                .collect(Collectors.toList());
    }

    @GetMapping("/devices/{id}")
    public ResponseEntity<Freezer> getDevice(@PathVariable Long id, HttpServletRequest request) {
        return freezerService.findById(id)
                .map(freezer -> canAccessFreezer(freezer, request) ? ResponseEntity.ok(freezer)
                        : ResponseEntity.status(HttpStatus.FORBIDDEN).<Freezer>build())
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/name/{name}")
    public ResponseEntity<Freezer> getDeviceByName(@PathVariable String name, HttpServletRequest request) {
        return freezerService.findByName(name)
                .map(freezer -> canAccessFreezer(freezer, request) ? ResponseEntity.ok(freezer)
                        : ResponseEntity.status(HttpStatus.FORBIDDEN).<Freezer>build())
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/storage-devices")
    public List<StorageDeviceResponse> listStorageDevices(HttpServletRequest request) {
        return storageLocationService.getAllDevices().stream().filter(StorageDevice::getActive)
                .filter(device -> canAccessRoom(device.getParentRoom(), request))
                .map(StorageDeviceResponse::from).collect(Collectors.toList());
    }

    @GetMapping("/users")
    public List<IdValuePair> listUsers() {
        return systemUserService.getAll().stream()
                .filter(user -> user.getIsActive() != null && IActionConstants.YES.equals(user.getIsActive()))
                .map(user -> new IdValuePair(user.getId(), user.getDisplayName())).collect(Collectors.toList());
    }

    @PostMapping("/devices")
    public ResponseEntity<Freezer> createDevice(@RequestBody @Valid Freezer freezer,
            @RequestParam(name = "roomId", required = true) Long roomId,
            HttpServletRequest request) {
        StorageRoom room = storageLocationService.getRoom(roomId.intValue());
        if (!canAccessRoom(room, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Freezer created = freezerService.createFreezer(freezer, roomId, getSysUserId(request));
        return ResponseEntity.ok(created);
    }

    @PutMapping("/devices/{id}")
    public ResponseEntity<Freezer> updateDevice(@PathVariable Long id, @RequestBody @Valid Freezer freezer,
            @RequestParam(name = "roomId", required = true) Long roomId,
            HttpServletRequest request) {
        Freezer existing = freezerService.requireFreezer(id);
        StorageRoom targetRoom = storageLocationService.getRoom(roomId.intValue());
        if (!canAccessFreezer(existing, request) || !canAccessRoom(targetRoom, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Freezer updated = freezerService.updateFreezer(id, freezer, roomId, getSysUserId(request));
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/devices/{id}/toggle-status")
    public ResponseEntity<Void> toggleDeviceStatus(@PathVariable Long id, @RequestBody ToggleStatusRequest request,
            HttpServletRequest httpRequest) {
        Freezer freezer = freezerService.requireFreezer(id);
        if (!canAccessFreezer(freezer, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        freezerService.setDeviceStatus(id, request.getActive());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/devices/{id}/delete")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id, HttpServletRequest request) {
        Freezer freezer = freezerService.requireFreezer(id);
        if (!canAccessFreezer(freezer, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        freezerService.deleteFreezer(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/devices/{id}/thresholds")
    public ResponseEntity<Freezer> updateDeviceThresholds(@PathVariable Long id,
            @RequestBody @Valid UpdateThresholdsRequest request, HttpServletRequest httpRequest) {
        Freezer freezer = freezerService.requireFreezer(id);
        if (!canAccessFreezer(freezer, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Freezer updated = freezerService.updateThresholds(id, request.getTargetTemperature(),
                request.getWarningThreshold(), request.getCriticalThreshold(), request.getPollingIntervalSeconds(),
                getSysUserId(httpRequest));
        return ResponseEntity.ok(updated);
    }

    private boolean canAccessFreezer(Freezer freezer, HttpServletRequest request) {
        return freezer != null && canAccessRoom(freezer.getStorageRoom(), request);
    }

    private boolean canAccessRoom(StorageRoom room, HttpServletRequest request) {
        return room != null && departmentIsolationService.canAccessStorageRoom(room, request);
    }

    private FreezerStatusResponse toStatusResponse(Freezer freezer) {
        FreezerReading latest = freezerReadingService.getLatestReading(freezer.getId()).orElse(null);
        ThresholdProfile profile = resolveActiveProfile(freezer,
                latest != null ? latest.getRecordedAt() : OffsetDateTime.now());
        BigDecimal targetTemperature = deriveTargetTemperature(profile);
        return FreezerStatusResponse.from(freezer, latest, targetTemperature);
    }

    private ThresholdProfile resolveActiveProfile(Freezer freezer, OffsetDateTime timestamp) {
        if (thresholdEvaluationService == null) {
            return null;
        }
        try {
            return thresholdEvaluationService.resolveActiveProfile(freezer, timestamp);
        } catch (Exception ex) {
            LOGGER.debug("No active threshold profile for freezer {}: {}", freezer.getName(), ex.getMessage());
            return null;
        }
    }

    private BigDecimal deriveTargetTemperature(ThresholdProfile profile) {
        if (profile == null) {
            return null;
        }
        if (profile.getWarningMin() != null && profile.getWarningMax() != null) {
            return profile.getWarningMin().add(profile.getWarningMax()).divide(BigDecimal.valueOf(2), 2,
                    RoundingMode.HALF_UP);
        }
        if (profile.getCriticalMin() != null && profile.getCriticalMax() != null) {
            return profile.getCriticalMin().add(profile.getCriticalMax()).divide(BigDecimal.valueOf(2), 2,
                    RoundingMode.HALF_UP);
        }
        if (profile.getWarningMax() != null) {
            return profile.getWarningMax();
        }
        if (profile.getCriticalMax() != null) {
            return profile.getCriticalMax();
        }
        if (profile.getWarningMin() != null) {
            return profile.getWarningMin();
        }
        return profile.getCriticalMin();
    }

    @Data
    public static class FreezerStatusResponse {
        private Long freezerId;
        private String freezerName;
        private String locationName;
        private String deviceType;
        private String protocol;
        private BigDecimal targetTemperatureCelsius;
        private FreezerReading.Status status;
        private BigDecimal temperatureCelsius;
        private BigDecimal humidityPercentage;
        private OffsetDateTime recordedAt;

        public static FreezerStatusResponse from(Freezer freezer, FreezerReading reading,
                BigDecimal targetTemperature) {
            FreezerStatusResponse response = new FreezerStatusResponse();
            response.setFreezerId(freezer.getId());
            response.setFreezerName(freezer.getName());
            response.setLocationName(freezer.getStorageRoom() != null ? freezer.getStorageRoom().getName() : null);
            response.setDeviceType(
                    freezer.getLinkedDeviceTypeString() != null ? freezer.getLinkedDeviceTypeString() : "unknown");
            response.setProtocol(freezer.getProtocol() != null ? freezer.getProtocol().name() : null);
            response.setTargetTemperatureCelsius(targetTemperature);
            response.setStatus(reading != null ? reading.getStatus() : FreezerReading.Status.NORMAL);
            response.setTemperatureCelsius(reading != null ? reading.getTemperatureCelsius() : null);
            response.setHumidityPercentage(reading != null ? reading.getHumidityPercentage() : null);
            response.setRecordedAt(reading != null ? reading.getRecordedAt() : null);
            return response;
        }
    }

    @Data
    public static class StorageDeviceResponse {
        private Integer id;
        private String name;
        private String code;
        private String type;
        private String roomName;

        public static StorageDeviceResponse from(StorageDevice device) {
            StorageDeviceResponse response = new StorageDeviceResponse();
            response.setId(device.getId());
            response.setName(device.getName());
            response.setCode(device.getCode());
            response.setType(device.getType());
            response.setRoomName(device.getParentRoom() != null ? device.getParentRoom().getName() : null);
            return response;
        }
    }

    @Data
    public static class SensorReadingResponse {
        private OffsetDateTime recordedAt;
        private BigDecimal temperatureCelsius;
        private BigDecimal humidityPercentage;
        private FreezerReading.Status status;

        public static SensorReadingResponse from(FreezerReading reading) {
            SensorReadingResponse response = new SensorReadingResponse();
            response.setRecordedAt(reading.getRecordedAt());
            response.setTemperatureCelsius(reading.getTemperatureCelsius());
            response.setHumidityPercentage(reading.getHumidityPercentage());
            response.setStatus(reading.getStatus());
            return response;
        }
    }

    @Data
    public static class ToggleStatusRequest {
        private Boolean active;
    }

    @Data
    public static class UpdateThresholdsRequest {
        private BigDecimal targetTemperature;
        private BigDecimal warningThreshold;
        private BigDecimal criticalThreshold;
        private Integer pollingIntervalSeconds;
    }
}
