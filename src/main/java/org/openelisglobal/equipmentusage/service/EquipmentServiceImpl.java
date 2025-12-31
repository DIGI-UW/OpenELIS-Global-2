package org.openelisglobal.equipmentusage.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.equipmentusage.dao.EquipmentDAO;
import org.openelisglobal.equipmentusage.valueholder.Equipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentServiceImpl extends AuditableBaseObjectServiceImpl<Equipment, Long> implements EquipmentService {

    @Autowired
    private EquipmentDAO equipmentDAO;

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
            Long id = (Long) super.insert(equipment);
            return super.get(id);
        }
        return update(equipment);
    }

    @Override
    @Transactional
    public void deactivateEquipment(Long equipmentId) {
        Equipment equipment = get(equipmentId);
        equipment.setIsActive("N");
        update(equipment);
    }

    @Override
    @Transactional
    public void activateEquipment(Long equipmentId) {
        Equipment equipment = get(equipmentId);
        equipment.setIsActive("Y");
        update(equipment);
    }
}
