package org.openelisglobal.inventory.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "inventory_storage_location")
public class InventoryStorageLocation extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_storage_location_generator")
    @SequenceGenerator(name = "inventory_storage_location_generator", sequenceName = "inventory_storage_location_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "name", nullable = false, length = 255)
    @NotNull
    @Size(min = 1, max = 255)
    private String name;

    @Column(name = "location_code", length = 50, unique = true)
    private String locationCode;

    @Column(name = "location_type", nullable = false, length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "temperature_min", precision = 5, scale = 2)
    private BigDecimal temperatureMin;

    @Column(name = "temperature_max", precision = 5, scale = 2)
    private BigDecimal temperatureMax;

    @ManyToOne
    @JoinColumn(name = "parent_location_id")
    private InventoryStorageLocation parentLocation;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Business logic helper methods
    @JsonIgnore
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    @JsonIgnore
    public String getTemperatureRangeDisplay() {
        if (temperatureMin != null && temperatureMax != null) {
            return temperatureMin + "°C to " + temperatureMax + "°C";
        }
        return null;
    }
}
