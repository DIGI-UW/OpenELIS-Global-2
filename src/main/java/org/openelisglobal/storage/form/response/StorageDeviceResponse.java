package org.openelisglobal.storage.form.response;

import java.math.BigDecimal;

public class StorageDeviceResponse {
    private Integer id;
    private String name;
    private String code;
    private String type;
    private BigDecimal temperatureSetting;
    private Integer capacityLimit;
    private Boolean active;
    private String fhirUuid;
    private String ipAddress;
    private Integer port;
    private String communicationProtocol;
    private Integer roomId;
    private String roomName;
    private Integer parentRoomId;
    private String parentRoomName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public BigDecimal getTemperatureSetting() {
        return temperatureSetting;
    }

    public void setTemperatureSetting(BigDecimal temperatureSetting) {
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

    public String getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(String fhirUuid) {
        this.fhirUuid = fhirUuid;
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

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Integer getParentRoomId() {
        return parentRoomId;
    }

    public void setParentRoomId(Integer parentRoomId) {
        this.parentRoomId = parentRoomId;
    }

    public String getParentRoomName() {
        return parentRoomName;
    }

    public void setParentRoomName(String parentRoomName) {
        this.parentRoomName = parentRoomName;
    }
}
