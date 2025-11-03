package org.openelisglobal.storage.valueholder;

import java.util.UUID;
import jakarta.persistence.*;
import org.openelisglobal.common.valueholder.BaseObject;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "storage_position")
public class StoragePosition extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "storage_position_generator")
    @GenericGenerator(name = "storage_position_generator", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", 
        parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "storage_position_seq"))
    @Column(name = "id")
    private String id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "coordinate", nullable = false, length = 50)
    private String coordinate;

    @Column(name = "row_index")
    private Integer rowIndex;

    @Column(name = "column_index")
    private Integer columnIndex;

    @Column(name = "occupied", nullable = false)
    private Boolean occupied = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_rack_id", nullable = false)
    private StorageRack parentRack;

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public Integer getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(Integer columnIndex) {
        this.columnIndex = columnIndex;
    }

    public Boolean getOccupied() {
        return occupied;
    }

    public void setOccupied(Boolean occupied) {
        this.occupied = occupied;
    }

    public boolean isOccupied() {
        return occupied != null && occupied;
    }

    public StorageRack getParentRack() {
        return parentRack;
    }

    public void setParentRack(StorageRack parentRack) {
        this.parentRack = parentRack;
    }
}

