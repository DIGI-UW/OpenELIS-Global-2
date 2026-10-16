package org.openelisglobal.storage.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "storage_location_print_history")
public class StorageLocationPrintHistory extends BaseObject<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "location_type", nullable = false)
    private String locationType;

    @Column(name = "location_id", nullable = false)
    private String locationId;

    @Column(name = "location_code")
    private String locationCode;

    @Column(name = "printed_by", nullable = false)
    private String printedBy;

    @Column(name = "printed_date", nullable = false)
    private Date printedDate;

    @Column(name = "print_count", nullable = false)
    private Integer printCount;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getPrintedBy() {
        return printedBy;
    }

    public void setPrintedBy(String printedBy) {
        this.printedBy = printedBy;
    }

    public Date getPrintedDate() {
        return printedDate;
    }

    public void setPrintedDate(Date printedDate) {
        this.printedDate = printedDate;
    }

    public Integer getPrintCount() {
        return printCount;
    }

    public void setPrintCount(Integer printCount) {
        this.printCount = printCount;
    }
}
