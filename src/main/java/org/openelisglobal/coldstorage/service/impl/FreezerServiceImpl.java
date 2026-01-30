package org.openelisglobal.coldstorage.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.dao.FreezerDAO;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("unused")
public class FreezerServiceImpl implements FreezerService {

    private final FreezerDAO freezerDAO;
    private final StorageLocationService storageLocationService;

    public FreezerServiceImpl(FreezerDAO freezerDAO, StorageLocationService storageLocationService) {
        this.freezerDAO = freezerDAO;
        this.storageLocationService = storageLocationService;
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
        return freezer;
    }

    @Override
    @Transactional
    public Freezer updateFreezer(Long id, Freezer updatedFreezer, Long roomId, String sysUserId) {
        Freezer existing = requireFreezer(id);

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
        existing.setTargetTemperature(updatedFreezer.getTargetTemperature());
        existing.setWarningThreshold(updatedFreezer.getWarningThreshold());
        existing.setCriticalThreshold(updatedFreezer.getCriticalThreshold());
        existing.setPollingIntervalSeconds(updatedFreezer.getPollingIntervalSeconds());

        return freezerDAO.update(existing);
    }

    @Override
    @Transactional
    public Freezer updateThresholds(Long id, BigDecimal targetTemperature, BigDecimal warningThreshold,
            BigDecimal criticalThreshold, Integer pollingIntervalSeconds, String sysUserId) {
        Freezer freezer = requireFreezer(id);
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

        return freezerDAO.update(freezer);
    }

    @Override
    @Transactional
    public void setDeviceStatus(Long id, Boolean active) {
        Freezer freezer = requireFreezer(id);
        freezer.setActive(active);
        freezerDAO.update(freezer);
    }

    @Override
    @Transactional
    public void deleteFreezer(Long id) {
        Freezer freezer = requireFreezer(id);
        // Soft delete by setting inactive
        freezer.setActive(false);
        freezerDAO.update(freezer);
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
        device.setCode(generateDeviceCode(freezer.getName()));
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

    /**
     * Generates a unique code for a StorageDevice based on the device name.
     * Converts name to uppercase, removes non-alphanumeric chars, and truncates to
     * 50 chars.
     */
    private String generateDeviceCode(String name) {
        if (name == null || name.isBlank()) {
            return "DEV";
        }
        String cleanedName = name.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return cleanedName.substring(0, Math.min(50, cleanedName.length()));
    }
}
