package org.openelisglobal.storage.service;

import java.util.List;
import org.openelisglobal.storage.valueholder.StoragePosition;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.openelisglobal.storage.valueholder.StorageRack;

public interface StorageLocationService {
    // Room methods
    List<StorageRoom> getRooms();
    StorageRoom getRoom(String id);
    StorageRoom createRoom(StorageRoom room);
    StorageRoom updateRoom(String id, StorageRoom room);
    void deleteRoom(String id);

    // Device methods
    List<StorageDevice> getDevicesByRoom(String roomId);
    List<StorageDevice> getAllDevices();

    // Shelf methods
    List<StorageShelf> getShelvesByDevice(String deviceId);
    List<StorageShelf> getAllShelves();

    // Rack methods
    List<StorageRack> getRacksByShelf(String shelfId);
    List<StorageRack> getAllRacks();

    // Position methods
    List<StoragePosition> getPositionsByRack(String rackId);
    List<StoragePosition> getAllPositions();

    // Count methods
    int countOccupiedInDevice(String deviceId);
    int countOccupied(String rackId);

    // Generic CRUD methods
    String insert(Object entity);
    String update(Object entity);
    void delete(Object entity);
    Object get(String id, Class<?> entityClass);
    
    // Validation methods
    boolean validateLocationActive(StoragePosition position);
    String buildHierarchicalPath(StoragePosition position);
}

