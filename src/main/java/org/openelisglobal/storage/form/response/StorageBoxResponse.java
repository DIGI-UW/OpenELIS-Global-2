package org.openelisglobal.storage.form.response;

import java.util.Map;

public class StorageBoxResponse {
    private Integer id;
    private String label;
    private String type;
    private Integer rows;
    private Integer columns;
    private Integer capacity;
    private String positionSchemaHint;
    private String code;
    private Boolean active;
    private Boolean occupied;
    private Map<String, Map<String, String>> occupiedCoordinates;
    private String fhirUuid;
    private Integer parentRackId;
    private String rackLabel;
    private String parentRackLabel;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Integer getColumns() {
        return columns;
    }

    public void setColumns(Integer columns) {
        this.columns = columns;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getPositionSchemaHint() {
        return positionSchemaHint;
    }

    public void setPositionSchemaHint(String positionSchemaHint) {
        this.positionSchemaHint = positionSchemaHint;
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

    public Boolean getOccupied() {
        return occupied;
    }

    public void setOccupied(Boolean occupied) {
        this.occupied = occupied;
    }

    public Map<String, Map<String, String>> getOccupiedCoordinates() {
        return occupiedCoordinates;
    }

    public void setOccupiedCoordinates(Map<String, Map<String, String>> occupiedCoordinates) {
        this.occupiedCoordinates = occupiedCoordinates;
    }

    public String getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(String fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public Integer getParentRackId() {
        return parentRackId;
    }

    public void setParentRackId(Integer parentRackId) {
        this.parentRackId = parentRackId;
    }

    public String getRackLabel() {
        return rackLabel;
    }

    public void setRackLabel(String rackLabel) {
        this.rackLabel = rackLabel;
    }

    public String getParentRackLabel() {
        return parentRackLabel;
    }

    public void setParentRackLabel(String parentRackLabel) {
        this.parentRackLabel = parentRackLabel;
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
}
