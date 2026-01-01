package org.openelisglobal.equipmentusage.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.equipmentusage.valueholder.Equipment;

public interface EquipmentService extends BaseObjectService<Equipment, Long> {

    /**
     * Get all active equipment
     */
    List<Equipment> getAllActive();

    /**
     * Get equipment by serial number
     */
    Optional<Equipment> getBySerialNumber(String serialNumber);

    /**
     * Search equipment by name (partial matching)
     */
    List<Equipment> searchByName(String searchTerm);

    /**
     * Get equipment by department
     */
    List<Equipment> getByDepartment(String department);

    /**
     * Get equipment for dropdown/select lists (active only)
     */
    List<Equipment> getEquipmentForDropdown();

    /**
     * Create or update equipment master data
     */
    Equipment save(Equipment equipment);

    /**
     * Create or update equipment master data with user context
     */
    Equipment save(Equipment equipment, String currentUserId);

    /**
     * Deactivate equipment (soft delete)
     */
    void deactivateEquipment(Long equipmentId);

    /**
     * Deactivate equipment with user context for audit trail
     */
    void deactivateEquipment(Long equipmentId, String currentUserId);

    /**
     * Activate equipment
     */
    void activateEquipment(Long equipmentId);

    /**
     * Activate equipment with user context for audit trail
     */
    void activateEquipment(Long equipmentId, String currentUserId);
}
