package org.openelisglobal.storage.service;

import org.openelisglobal.storage.valueholder.StoragePosition;

public interface StorageLocationService {
    // Generic CRUD methods
    String insert(Object entity);
    String update(Object entity);
    void delete(Object entity);
    Object get(String id, Class<?> entityClass);
    
    // Validation methods
    boolean validateLocationActive(StoragePosition position);
    String buildHierarchicalPath(StoragePosition position);
}

