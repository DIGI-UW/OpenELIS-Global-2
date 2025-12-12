package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form object for StorageDevice entity - used for REST API input validation
 * Following OpenELIS pattern: Form objects for transport, entities for
 * persistence
 */
public class StorageDeviceForm {

    private String id;

    @NotBlank(message = "Device name is required")
    @Size(max = 255, message = "Device name must not exceed 255 characters")
    private String name;

    @Size(max = 10, message = "Device code must not exceed 10 characters")
    private String code; // Optional - will be auto-generated if not provided

    @NotBlank(message = "Device type is required")
    @Pattern(regexp = "freezer|refrigerator|cabinet|other", message = "Device type must be one of: freezer, refrigerator, cabinet, other")
    private String type;

    private Double temperatureSetting;

    private Integer capacityLimit;

    private Boolean active = true;

    // Note: parentRoomId is required for creation but optional for updates
    // (parent cannot be changed after creation, backend ignores this field on PUT)
    private String parentRoomId;

    // Device connectivity configuration fields for network-connected equipment
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Pattern(regexp = "^$|^((25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(?!$)|$)){4}|^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:)|(([0-9A-Fa-f]{1,4}:){1,6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,5}(:[0-9A-Fa-f]{1,4}){1,2})|(([0-9A-Fa-f]{1,4}:){1,4}(:[0-9A-Fa-f]{1,4}){1,3})|(([0-9A-Fa-f]{1,4}:){1,3}(:[0-9A-Fa-f]{1,4}){1,4})|(([0-9A-Fa-f]{1,4}:){1,2}(:[0-9A-Fa-f]{1,4}){1,5})|([0-9A-Fa-f]{1,4}:((:[0-9A-Fa-f]{1,4}){1,6}))|(:((:[0-9A-Fa-f]{1,4}){1,7}|:))|(([0-9A-Fa-f]{1,4}:){1,4}:((25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(?!$)|$)){4}))$", message = "IP address must be valid IPv4 or IPv6 format")
    private String ipAddress;

    private Integer port; // Validated by database constraint: 1-65535

    @Size(max = 20, message = "Communication protocol must not exceed 20 characters")
    private String communicationProtocol;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getTemperatureSetting() {
        return temperatureSetting;
    }

    public void setTemperatureSetting(Double temperatureSetting) {
        this.temperatureSetting = temperatureSetting;
    }

    public Integer getCapacityLimit() {
        return capacityLimit;
    }

    public void setCapacityLimit(Integer capacityLimit) {
        this.capacityLimit = capacityLimit;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getParentRoomId() {
        return parentRoomId;
    }

    public void setParentRoomId(String parentRoomId) {
        this.parentRoomId = parentRoomId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getCommunicationProtocol() {
        return communicationProtocol;
    }

    public void setCommunicationProtocol(String communicationProtocol) {
        this.communicationProtocol = communicationProtocol;
    }
}
