package org.openelisglobal.storage.service;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.dao.*;
import org.openelisglobal.storage.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StorageLocationServiceImpl implements StorageLocationService {
    
    @Autowired
    private StorageRoomDAO storageRoomDAO;
    
    @Autowired
    private StorageDeviceDAO storageDeviceDAO;
    
    @Autowired
    private StorageShelfDAO storageShelfDAO;
    
    @Autowired
    private StorageRackDAO storageRackDAO;
    
    @Autowired
    private StoragePositionDAO storagePositionDAO;

    @Override
    public String insert(Object entity) {
        if (entity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) entity;
            // Check for duplicate code
            StorageRoom existing = storageRoomDAO.findByCode(room.getCode());
            if (existing != null) {
                throw new LIMSRuntimeException("Room with code " + room.getCode() + " already exists");
            }
            return storageRoomDAO.insert(room);
        } else if (entity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) entity;
            // Check for duplicate code in same room
            StorageDevice existing = storageDeviceDAO.findByParentRoomIdAndCode(
                device.getParentRoom().getId(), device.getCode());
            if (existing != null) {
                throw new LIMSRuntimeException("Device with code " + device.getCode() + 
                    " already exists in this room");
            }
            return storageDeviceDAO.insert(device);
        } else if (entity instanceof StorageShelf) {
            return storageShelfDAO.insert((StorageShelf) entity);
        } else if (entity instanceof StorageRack) {
            StorageRack rack = (StorageRack) entity;
            // Validate grid dimensions
            if (rack.getRows() < 0 || rack.getColumns() < 0) {
                throw new IllegalArgumentException("Grid dimensions cannot be negative");
            }
            return storageRackDAO.insert(rack);
        } else if (entity instanceof StoragePosition) {
            return storagePositionDAO.insert((StoragePosition) entity);
        }
        throw new LIMSRuntimeException("Unsupported entity type for insert");
    }

    @Override
    public String update(Object entity) {
        if (entity instanceof StorageRoom) {
            storageRoomDAO.update((StorageRoom) entity);
            return null;
        } else if (entity instanceof StorageDevice) {
            StorageDevice device = (StorageDevice) entity;
            // Check for active samples when deactivating
            if (!device.getActive()) {
                int occupiedCount = storagePositionDAO.countOccupiedInDevice(device.getId());
                if (occupiedCount > 0) {
                    return "Warning: Device has " + occupiedCount + " active samples. " +
                           "Please move or dispose samples before deactivating.";
                }
            }
            storageDeviceDAO.update(device);
            return null;
        } else if (entity instanceof StorageShelf) {
            storageShelfDAO.update((StorageShelf) entity);
            return null;
        } else if (entity instanceof StorageRack) {
            storageRackDAO.update((StorageRack) entity);
            return null;
        } else if (entity instanceof StoragePosition) {
            storagePositionDAO.update((StoragePosition) entity);
            return null;
        }
        throw new LIMSRuntimeException("Unsupported entity type for update");
    }

    @Override
    public void delete(Object entity) {
        if (entity instanceof StorageRoom) {
            StorageRoom room = (StorageRoom) entity;
            // Check for active child devices
            var devices = storageDeviceDAO.findByParentRoomId(room.getId());
            boolean hasActiveDevices = devices.stream()
                .anyMatch(d -> d.getActive() != null && d.getActive());
            if (hasActiveDevices) {
                throw new LIMSRuntimeException("Cannot delete room with active child devices");
            }
            storageRoomDAO.delete(room);
        } else if (entity instanceof StorageDevice) {
            storageDeviceDAO.delete((StorageDevice) entity);
        } else if (entity instanceof StorageShelf) {
            storageShelfDAO.delete((StorageShelf) entity);
        } else if (entity instanceof StorageRack) {
            storageRackDAO.delete((StorageRack) entity);
        } else if (entity instanceof StoragePosition) {
            storagePositionDAO.delete((StoragePosition) entity);
        } else {
            throw new LIMSRuntimeException("Unsupported entity type for delete");
        }
    }

    @Override
    public Object get(String id, Class<?> entityClass) {
        if (entityClass == StorageRoom.class) {
            return storageRoomDAO.get(id);
        } else if (entityClass == StorageDevice.class) {
            return storageDeviceDAO.get(id);
        } else if (entityClass == StorageShelf.class) {
            return storageShelfDAO.get(id);
        } else if (entityClass == StorageRack.class) {
            return storageRackDAO.get(id);
        } else if (entityClass == StoragePosition.class) {
            return storagePositionDAO.get(id);
        }
        throw new LIMSRuntimeException("Unsupported entity class for get");
    }

    @Override
    public boolean validateLocationActive(StoragePosition position) {
        if (position == null || position.getParentRack() == null) {
            return false;
        }
        
        StorageRack rack = position.getParentRack();
        if (rack.getParentShelf() == null) {
            return false;
        }
        
        StorageShelf shelf = rack.getParentShelf();
        if (shelf.getParentDevice() == null) {
            return false;
        }
        
        StorageDevice device = shelf.getParentDevice();
        if (device.getParentRoom() == null) {
            return false;
        }
        
        StorageRoom room = device.getParentRoom();
        
        // Check entire hierarchy is active
        return room.getActive() != null && room.getActive() &&
               device.getActive() != null && device.getActive() &&
               shelf.getActive() != null && shelf.getActive() &&
               rack.getActive() != null && rack.getActive();
    }

    @Override
    public String buildHierarchicalPath(StoragePosition position) {
        if (position == null) {
            return "Unknown Location";
        }
        
        if (position.getParentRack() == null) {
            return "Unknown";
        }
        
        StorageRack rack = position.getParentRack();
        if (rack.getParentShelf() == null) {
            return rack.getLabel() + " > Position " + position.getCoordinate();
        }
        
        StorageShelf shelf = rack.getParentShelf();
        if (shelf.getParentDevice() == null) {
            return shelf.getLabel() + " > " + rack.getLabel() + " > Position " + position.getCoordinate();
        }
        
        StorageDevice device = shelf.getParentDevice();
        if (device.getParentRoom() == null) {
            return device.getName() + " > " + shelf.getLabel() + " > " + 
                   rack.getLabel() + " > Position " + position.getCoordinate();
        }
        
        StorageRoom room = device.getParentRoom();
        
        return room.getName() + " > " + device.getName() + " > " + 
               shelf.getLabel() + " > " + rack.getLabel() + " > Position " + position.getCoordinate();
    }
}

