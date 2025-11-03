package org.openelisglobal.storage.valueholder;

import java.util.UUID;
import jakarta.persistence.*;
import org.openelisglobal.common.valueholder.BaseObject;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "storage_rack")
public class StorageRack extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "storage_rack_generator")
    @GenericGenerator(name = "storage_rack_generator", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", 
        parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "storage_rack_seq"))
    @Column(name = "id")
    private String id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "rows", nullable = false)
    private Integer rows = 0;

    @Column(name = "columns", nullable = false)
    private Integer columns = 0;

    @Column(name = "position_schema_hint", length = 50)
    private String positionSchemaHint;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_shelf_id", nullable = false)
    private StorageShelf parentShelf;

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    public Integer getCapacity() {
        if (rows == null || columns == null || rows == 0 || columns == 0) {
            return 0;
        }
        return rows * columns;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public String getPositionSchemaHint() {
        return positionSchemaHint;
    }

    public void setPositionSchemaHint(String positionSchemaHint) {
        this.positionSchemaHint = positionSchemaHint;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active != null && active;
    }

    public StorageShelf getParentShelf() {
        return parentShelf;
    }

    public void setParentShelf(StorageShelf parentShelf) {
        this.parentShelf = parentShelf;
    }
}

