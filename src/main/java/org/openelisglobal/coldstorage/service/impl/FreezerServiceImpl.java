package org.openelisglobal.coldstorage.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.coldstorage.dao.FreezerDAO;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("unused")
public class FreezerServiceImpl implements FreezerService {

    private static final String AUDIT_TABLE = "freezer";

    private final FreezerDAO freezerDAO;
    private final StorageLocationService storageLocationService;
    private final AuditTrailService auditTrailService;

    public FreezerServiceImpl(FreezerDAO freezerDAO, StorageLocationService storageLocationService,
            AuditTrailService auditTrailService) {
        this.freezerDAO = freezerDAO;
        this.storageLocationService = storageLocationService;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Freezer> getActiveFreezers() {
        return freezerDAO.findActiveFreezers();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Freezer> findByName(String name) {
        return freezerDAO.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Freezer> findById(Long id) {
        return freezerDAO.get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Freezer requireFreezer(Long id) {
        return freezerDAO.get(id).orElseThrow(() -> new IllegalArgumentException("Freezer not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Freezer> getAllFreezers(String search) {
        if (search != null && !search.isBlank()) {
            return freezerDAO.searchFreezers(search);
        }
        return freezerDAO.getAllFreezers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Freezer> getAllFreezersForReporting() {
        return freezerDAO.getAllFreezersIncludingDeleted();
    }

    @Override
    @Transactional
    public Freezer createFreezer(Freezer freezer, Long roomId, String sysUserId) {
        // Validate unique name
        if (freezerDAO.findByName(freezer.getName()).isPresent()) {
            throw new IllegalArgumentException("Freezer with name '" + freezer.getName() + "' already exists");
        }

        // Fetch and set StorageDevice if provided, or auto-create one
        if (freezer.getStorageDevice() != null && freezer.getStorageDevice().getId() != null) {
            // Link to existing StorageDevice
            StorageDevice device = (StorageDevice) storageLocationService.get(freezer.getStorageDevice().getId(),
                    StorageDevice.class);
            if (device == null) {
                throw new IllegalArgumentException("StorageDevice not found: " + freezer.getStorageDevice().getId());
            }
            freezer.setStorageDevice(device);
        } else if (freezer.getStorageDevice() != null && freezer.getStorageDevice().getType() != null
                && roomId != null) {
            // Auto-create StorageDevice if device type and roomId are provided
            StorageDevice newDevice = createStorageDeviceFromFreezer(freezer, roomId, sysUserId);
            freezer.setStorageDevice(newDevice);
        }

        freezerDAO.insert(freezer);
        auditTrailService.saveNewHistory(freezer, sysUserId, AUDIT_TABLE);
        return freezer;
    }

    @Override
    @Transactional
    public Freezer updateFreezer(Long id, Freezer updatedFreezer, Long roomId, String sysUserId) {
        Freezer existing = requireFreezer(id);
        Freezer before = auditCopy(existing);

        if (!existing.getName().equals(updatedFreezer.getName())) {
            freezerDAO.findByName(updatedFreezer.getName()).ifPresent(f -> {
                if (!f.getId().equals(id)) {
                    throw new IllegalArgumentException(
                            "Freezer with name '" + updatedFreezer.getName() + "' already exists");
                }
            });
        }

        if (updatedFreezer.getStorageDevice() != null && updatedFreezer.getStorageDevice().getId() != null) {
            StorageDevice device = (StorageDevice) storageLocationService.get(updatedFreezer.getStorageDevice().getId(),
                    StorageDevice.class);
            if (device == null) {
                throw new IllegalArgumentException(
                        "StorageDevice not found: " + updatedFreezer.getStorageDevice().getId());
            }
            existing.setStorageDevice(device);
        } else if (updatedFreezer.getStorageDevice() != null && updatedFreezer.getStorageDevice().getType() != null
                && roomId != null) {
            if (existing.getStorageDevice() != null) {
                updateStorageDeviceFromFreezer(existing.getStorageDevice(), updatedFreezer, roomId, sysUserId);
            } else {
                StorageDevice newDevice = createStorageDeviceFromFreezer(updatedFreezer, roomId, sysUserId);
                existing.setStorageDevice(newDevice);
            }
        } else {
            existing.setStorageDevice(null);
        }

        existing.setName(updatedFreezer.getName());
        existing.setProtocol(updatedFreezer.getProtocol());
        existing.setHost(updatedFreezer.getHost());
        existing.setPort(updatedFreezer.getPort());
        existing.setSerialPort(updatedFreezer.getSerialPort());
        existing.setBaudRate(updatedFreezer.getBaudRate());
        existing.setDataBits(updatedFreezer.getDataBits());
        existing.setStopBits(updatedFreezer.getStopBits());
        existing.setParity(updatedFreezer.getParity());
        existing.setSlaveId(updatedFreezer.getSlaveId());
        existing.setTemperatureRegister(updatedFreezer.getTemperatureRegister());
        existing.setTemperatureScale(updatedFreezer.getTemperatureScale());
        existing.setTemperatureOffset(updatedFreezer.getTemperatureOffset());
        existing.setHumidityRegister(updatedFreezer.getHumidityRegister());
        existing.setHumidityScale(updatedFreezer.getHumidityScale());
        existing.setHumidityOffset(updatedFreezer.getHumidityOffset());
        existing.setTemperatureRegister2(updatedFreezer.getTemperatureRegister2());
        existing.setTemperatureScale2(updatedFreezer.getTemperatureScale2());
        existing.setTemperatureOffset2(updatedFreezer.getTemperatureOffset2());
        existing.setTargetTemperature(updatedFreezer.getTargetTemperature());
        existing.setWarningThreshold(updatedFreezer.getWarningThreshold());
        existing.setCriticalThreshold(updatedFreezer.getCriticalThreshold());
        existing.setPollingIntervalSeconds(updatedFreezer.getPollingIntervalSeconds());
        if (updatedFreezer.getRegisterCount() != null) {
            existing.setRegisterCount(updatedFreezer.getRegisterCount());
        }
        if (updatedFreezer.getWordOrder() != null) {
            existing.setWordOrder(updatedFreezer.getWordOrder());
        }
        if (updatedFreezer.getRs485Mode() != null) {
            existing.setRs485Mode(updatedFreezer.getRs485Mode());
        }
        if (updatedFreezer.getRs485RtsActiveHigh() != null) {
            existing.setRs485RtsActiveHigh(updatedFreezer.getRs485RtsActiveHigh());
        }
        if (updatedFreezer.getRs485Termination() != null) {
            existing.setRs485Termination(updatedFreezer.getRs485Termination());
        }
        if (updatedFreezer.getRs485RxDuringTx() != null) {
            existing.setRs485RxDuringTx(updatedFreezer.getRs485RxDuringTx());
        }
        if (updatedFreezer.getRs485DelayBeforeMs() != null) {
            existing.setRs485DelayBeforeMs(updatedFreezer.getRs485DelayBeforeMs());
        }
        if (updatedFreezer.getRs485DelayAfterMs() != null) {
            existing.setRs485DelayAfterMs(updatedFreezer.getRs485DelayAfterMs());
        }

        audit(existing, before, sysUserId, IActionConstants.AUDIT_TRAIL_UPDATE);
        return freezerDAO.update(existing);
    }

    @Override
    @Transactional
    public Freezer updateThresholds(Long id, BigDecimal targetTemperature, BigDecimal warningThreshold,
            BigDecimal criticalThreshold, Integer pollingIntervalSeconds, String sysUserId) {
        Freezer freezer = requireFreezer(id);
        Freezer before = auditCopy(freezer);
        freezer.setTargetTemperature(targetTemperature);
        freezer.setWarningThreshold(warningThreshold);
        freezer.setCriticalThreshold(criticalThreshold);

        if (pollingIntervalSeconds != null) {
            freezer.setPollingIntervalSeconds(pollingIntervalSeconds);
        }

        if (freezer.getStorageDevice() != null && targetTemperature != null) {
            freezer.getStorageDevice().setTemperatureSetting(targetTemperature);
            freezer.getStorageDevice().setSysUserId(sysUserId);
            storageLocationService.update(freezer.getStorageDevice());
        }

        audit(freezer, before, sysUserId, IActionConstants.AUDIT_TRAIL_UPDATE);
        return freezerDAO.update(freezer);
    }

    @Override
    @Transactional
    public void setDeviceStatus(Long id, Boolean active, String sysUserId) {
        Freezer freezer = requireFreezer(id);
        if (Boolean.TRUE.equals(freezer.getDeleted())) {
            throw new IllegalArgumentException("Cannot change status of a deleted freezer: " + id);
        }
        Freezer before = auditCopy(freezer);
        freezer.setActive(active);
        audit(freezer, before, sysUserId, IActionConstants.AUDIT_TRAIL_UPDATE);
        freezerDAO.update(freezer);
    }

    @Override
    @Transactional
    public void deleteFreezer(Long id, String sysUserId) {
        Freezer freezer = requireFreezer(id);
        Freezer before = auditCopy(freezer);
        // Soft delete via a dedicated flag, distinct from the active enable/disable
        // toggle, so a deleted device stays out of every list query and its toggle
        // can no longer resurrect it (issue #3743).
        freezer.setDeleted(true);
        audit(freezer, before, sysUserId, IActionConstants.AUDIT_TRAIL_DELETE);
        freezerDAO.update(freezer);
    }

    private void audit(Freezer after, Freezer before, String sysUserId, String activity) {
        auditTrailService.saveHistory(auditCopy(after), before, sysUserId, activity, AUDIT_TABLE);
    }

    /**
     * Excludes lazy collections and lastupdated and normalises decimal scale, or
     * the text diff logs phantom changes.
     */
    private Freezer auditCopy(Freezer freezer) {
        Freezer copy = new Freezer();
        BeanUtils.copyProperties(freezer, copy, "readings", "thresholdAssignments", "lastupdated");
        if (freezer.getStorageDevice() != null) {
            // Not the shared instance: updateFreezer moves that one in place.
            StorageDevice device = new StorageDevice();
            device.setName(freezer.getStorageDevice().getName());
            device.setParentRoom(freezer.getStorageDevice().getParentRoom());
            copy.setStorageDevice(device);
        }
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(copy);
        for (var property : wrapper.getPropertyDescriptors()) {
            String name = property.getName();
            if (wrapper.isWritableProperty(name) && wrapper.getPropertyValue(name) instanceof BigDecimal value) {
                BigDecimal plain = value.stripTrailingZeros();
                wrapper.setPropertyValue(name, plain.scale() < 0 ? plain.setScale(0) : plain);
            }
        }
        return copy;
    }

    /**
     * Creates a new StorageDevice from a Freezer's metadata. Used when creating a
     * new Freezer without linking to an existing StorageDevice.
     */
    private StorageDevice createStorageDeviceFromFreezer(Freezer freezer, Long roomId, String sysUserId) {
        org.openelisglobal.storage.valueholder.StorageRoom room = (org.openelisglobal.storage.valueholder.StorageRoom) storageLocationService
                .get(roomId.intValue(), org.openelisglobal.storage.valueholder.StorageRoom.class);

        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        StorageDevice device = new StorageDevice();
        device.setName(freezer.getName());
        // Code deliberately left unset: StorageLocationService.insert derives a
        // room-unique one from the name, and a code set here would instead be
        // validated against the 10-character cap and rejected (issue #3904).
        device.setType(freezer.getStorageDevice().getType());
        device.setActive(true);
        device.setParentRoom(room);
        device.setSysUserId(sysUserId);

        // Set temperature setting if available
        if (freezer.getTargetTemperature() != null) {
            device.setTemperatureSetting(freezer.getTargetTemperature());
        }

        // Save the device
        storageLocationService.insert(device);
        return device;
    }

    /**
     * Updates an existing StorageDevice with new metadata from a Freezer.
     */
    private void updateStorageDeviceFromFreezer(StorageDevice device, Freezer freezer, Long roomId, String sysUserId) {
        org.openelisglobal.storage.valueholder.StorageRoom room = (org.openelisglobal.storage.valueholder.StorageRoom) storageLocationService
                .get(roomId.intValue(), org.openelisglobal.storage.valueholder.StorageRoom.class);

        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        device.setName(freezer.getName());
        device.setType(freezer.getStorageDevice().getType());
        device.setParentRoom(room);
        device.setSysUserId(sysUserId);

        // Update temperature setting if available
        if (freezer.getTargetTemperature() != null) {
            device.setTemperatureSetting(freezer.getTargetTemperature());
        }

        storageLocationService.update(device);
    }

}
