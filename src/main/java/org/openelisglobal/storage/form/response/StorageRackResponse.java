package org.openelisglobal.storage.form.response;

public class StorageRackResponse {
    private Integer id;
    private String label;
    private String code; // Renamed from shortCode per spec Session 2025-11-16 simplification
    private Boolean active;
    private String fhirUuid;
    private Integer parentShelfId;
    private String shelfLabel;
    private String parentShelfLabel;
    private Integer parentDeviceId;
    private String deviceName;
    private String parentDeviceName;
    private Integer parentRoomId;
    private String roomName;
    private String parentRoomName;
    private String hierarchicalPath;
    private String type;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Integer getParentShelfId() {
        return parentShelfId;
    }

    public void setParentShelfId(Integer parentShelfId) {
        this.parentShelfId = parentShelfId;
    }

    public String getShelfLabel() {
        return shelfLabel;
    }

    public void setShelfLabel(String shelfLabel) {
        this.shelfLabel = shelfLabel;
    }

    public String getParentShelfLabel() {
        return parentShelfLabel;
    }

    public void setParentShelfLabel(String parentShelfLabel) {
        this.parentShelfLabel = parentShelfLabel;
    }

    public Integer getParentDeviceId() {
        return parentDeviceId;
    }

    public void setParentDeviceId(Integer parentDeviceId) {
        this.parentDeviceId = parentDeviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getParentDeviceName() {
        return parentDeviceName;
    }

    public void setParentDeviceName(String parentDeviceName) {
        this.parentDeviceName = parentDeviceName;
    }

    public Integer getParentRoomId() {
        return parentRoomId;
    }

    public void setParentRoomId(Integer parentRoomId) {
        this.parentRoomId = parentRoomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getParentRoomName() {
        return parentRoomName;
    }

    public void setParentRoomName(String parentRoomName) {
        this.parentRoomName = parentRoomName;
    }

    public String getHierarchicalPath() {
        return hierarchicalPath;
    }

    public void setHierarchicalPath(String hierarchicalPath) {
        this.hierarchicalPath = hierarchicalPath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
