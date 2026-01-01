package org.openelisglobal.equipmentusage.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.equipmentusage.valueholder.Equipment;

public interface EquipmentDAO extends BaseDAO<Equipment, Long> {

    /**
     * Get all equipment (active and inactive)
     */
    List<Equipment> getAll() throws LIMSRuntimeException;

    /**
     * Get all active equipment
     */
    List<Equipment> getAllActive() throws LIMSRuntimeException;

    /**
     * Get equipment by serial number
     */
    Optional<Equipment> getBySerialNumber(String serialNumber) throws LIMSRuntimeException;

    /**
     * Search equipment by name (partial match)
     */
    List<Equipment> searchByName(String name) throws LIMSRuntimeException;

    /**
     * Get equipment by department
     */
    List<Equipment> getByDepartment(String department) throws LIMSRuntimeException;

    /**
     * Get all equipment for dropdowns (active only)
     */
    List<Equipment> getEquipmentForDropdown() throws LIMSRuntimeException;
}
