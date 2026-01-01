package org.openelisglobal.equipmentusage.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.equipmentusage.dao.EquipmentDAO;
import org.openelisglobal.equipmentusage.valueholder.Equipment;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentServiceImpl extends AuditableBaseObjectServiceImpl<Equipment, Long> implements EquipmentService {

    @Autowired
    private EquipmentDAO equipmentDAO;

    @Autowired
    private SystemUserService systemUserService;

    public EquipmentServiceImpl() {
        super(Equipment.class);
        this.auditTrailLog = true;
    }

    @Override
    protected EquipmentDAO getBaseObjectDAO() {
        return equipmentDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getAllActive() {
        return equipmentDAO.getAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Equipment> getBySerialNumber(String serialNumber) {
        return equipmentDAO.getBySerialNumber(serialNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> searchByName(String searchTerm) {
        return equipmentDAO.searchByName(searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getByDepartment(String department) {
        return equipmentDAO.getByDepartment(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentForDropdown() {
        return equipmentDAO.getEquipmentForDropdown();
    }

    @Override
    @Transactional
    public Equipment save(Equipment equipment) {
        if (equipment.getId() == null) {
            if (equipment.getCreatedDate() == null) {
                equipment.setCreatedDate(java.time.LocalDateTime.now());
            }
            if (equipment.getModifiedDate() == null) {
                equipment.setModifiedDate(java.time.LocalDateTime.now());
            }
            equipment.setLastupdatedFields();
            Long id = (Long) super.insert(equipment);
            return super.get(id);
        }
        equipment.setModifiedDate(java.time.LocalDateTime.now());
        equipment.setLastupdatedFields();
        return update(equipment);
    }

    @Override
    @Transactional
    public Equipment save(Equipment equipment, String currentUserId) {
        if (equipment.getId() == null) {
            if (equipment.getCreatedDate() == null) {
                equipment.setCreatedDate(java.time.LocalDateTime.now());
            }
            if (equipment.getModifiedDate() == null) {
                equipment.setModifiedDate(java.time.LocalDateTime.now());
            }

            if (currentUserId != null && !currentUserId.trim().isEmpty()) {
                equipment.setSysUserId(currentUserId);
                SystemUser createdBy = systemUserService.get(currentUserId);
                if (createdBy != null) {
                    equipment.setCreatedBy(createdBy);
                    equipment.setModifiedBy(createdBy);
                }
            }

            equipment.setLastupdatedFields();
            Long id = (Long) super.insert(equipment);
            return super.get(id);
        }

        // For updates: get fresh copy from DB, apply changes, then evict and update
        Equipment dbEquipment = get(equipment.getId());
        if (dbEquipment == null) {
            throw new IllegalArgumentException("Equipment not found: " + equipment.getId());
        }

        // Apply changes from the incoming equipment object to the fresh copy
        dbEquipment.setName(equipment.getName());
        dbEquipment.setSerialNumber(equipment.getSerialNumber());
        dbEquipment.setDepartment(equipment.getDepartment());
        dbEquipment.setManufacturer(equipment.getManufacturer());
        dbEquipment.setModelNumber(equipment.getModelNumber());
        dbEquipment.setPurchaseDate(equipment.getPurchaseDate());
        dbEquipment.setLastCalibrationDate(equipment.getLastCalibrationDate());
        dbEquipment.setNextCalibrationDue(equipment.getNextCalibrationDue());

        dbEquipment.setModifiedDate(java.time.LocalDateTime.now());
        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            dbEquipment.setSysUserId(currentUserId);
            SystemUser modifiedBy = systemUserService.get(currentUserId);
            if (modifiedBy != null) {
                dbEquipment.setModifiedBy(modifiedBy);
            }
        }

        dbEquipment.setLastupdatedFields();

        // Detach from session so audit can compare properly
        equipmentDAO.evict(dbEquipment);

        return update(dbEquipment);
    }

    @Override
    @Transactional
    public void deactivateEquipment(Long equipmentId) {
        deactivateEquipment(equipmentId, null);
    }

    @Override
    @Transactional
    public void deactivateEquipment(Long equipmentId, String currentUserId) {
        Equipment equipment = get(equipmentId);

        // Detach from session so audit can compare properly
        equipmentDAO.evict(equipment);

        equipment.setIsActive("N");
        equipment.setModifiedDate(java.time.LocalDateTime.now());
        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            equipment.setSysUserId(currentUserId);
            SystemUser modifiedBy = systemUserService.get(currentUserId);
            if (modifiedBy != null) {
                equipment.setModifiedBy(modifiedBy);
            }
        }
        equipment.setLastupdatedFields();
        update(equipment);
    }

    @Override
    @Transactional
    public void activateEquipment(Long equipmentId) {
        activateEquipment(equipmentId, null);
    }

    @Override
    @Transactional
    public void activateEquipment(Long equipmentId, String currentUserId) {
        Equipment equipment = get(equipmentId);

        // Detach from session so audit can compare properly
        equipmentDAO.evict(equipment);

        equipment.setIsActive("Y");
        equipment.setModifiedDate(java.time.LocalDateTime.now());
        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            equipment.setSysUserId(currentUserId);
            SystemUser modifiedBy = systemUserService.get(currentUserId);
            if (modifiedBy != null) {
                equipment.setModifiedBy(modifiedBy);
            }
        }
        equipment.setLastupdatedFields();
        update(equipment);
    }
}
