package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.dao.InventoryStorageLocationDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryStorageLocationServiceImpl extends AuditableBaseObjectServiceImpl<InventoryStorageLocation, Long>
        implements InventoryStorageLocationService {

    @Autowired
    private InventoryStorageLocationDAO storageLocationDAO;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    public InventoryStorageLocationServiceImpl() {
        super(InventoryStorageLocation.class);
        this.auditTrailLog = true; // Enable generic audit trail
    }

    @Override
    protected InventoryStorageLocationDAO getBaseObjectDAO() {
        return storageLocationDAO;
    }

    @Override
    @Transactional
    public Long insert(InventoryStorageLocation location) {
        // Ensure UUID is set before insert
        if (location.getFhirUuid() == null) {
            location.setFhirUuid(UUID.randomUUID());
        }
        // Audit logging is automatic via auditTrailLog = true in constructor
        return super.insert(location);
    }

    @Override
    @Transactional
    public InventoryStorageLocation update(InventoryStorageLocation location) {
        // Audit logging is automatic via auditTrailLog = true in constructor
        return super.update(location);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStorageLocation> getAllActive() {
        return storageLocationDAO.getAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStorageLocation> getByLocationType(LocationType locationType) {
        return storageLocationDAO.getByLocationType(locationType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStorageLocation> getChildLocations(Long parentLocationId) {
        return storageLocationDAO.getChildLocations(parentLocationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryStorageLocation> getTopLevelLocations() {
        return storageLocationDAO.getTopLevelLocations();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStorageLocation getByLocationCode(String locationCode) {
        return storageLocationDAO.getByLocationCode(locationCode);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStorageLocation getByFhirUuid(String fhirUuid) {
        return storageLocationDAO.getByFhirUuid(fhirUuid);
    }

    @Override
    @Transactional
    public void deactivateLocation(Long locationId, String sysUserId) {
        InventoryStorageLocation location = get(locationId);
        if (location != null) {
            // Check if location has active lots
            if (hasActiveLots(locationId)) {
                throw new IllegalStateException(
                        "Cannot deactivate location with active lots. Please move or dispose of all lots first.");
            }

            location.setIsActive(false);
            location.setSysUserId(sysUserId);
            location.setLastupdated(new Timestamp(System.currentTimeMillis()));
            update(location);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveLots(Long locationId) {
        List<InventoryLot> lots = inventoryLotDAO.getByStorageLocationId(locationId);
        if (lots == null || lots.isEmpty()) {
            return false;
        }

        // Check if any lot is available for use
        for (InventoryLot lot : lots) {
            if (lot.isAvailableForUse()) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public String getLocationPath(Long locationId) {
        InventoryStorageLocation location = get(locationId);
        if (location == null) {
            return "";
        }

        List<String> path = new ArrayList<>();
        InventoryStorageLocation current = location;

        // Build path from current location up to root
        while (current != null) {
            path.add(0, current.getName()); // Add at beginning to reverse order
            current = current.getParentLocation();
        }

        return String.join(" > ", path);
    }
}
