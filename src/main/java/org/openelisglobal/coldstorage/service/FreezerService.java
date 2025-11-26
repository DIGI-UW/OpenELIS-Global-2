package org.openelisglobal.coldstorage.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.valueholder.Freezer;

public interface FreezerService {

    List<Freezer> getActiveFreezers();

    List<Freezer> getAllFreezers(String search);

    Optional<Freezer> findByName(String name);

    Optional<Freezer> findById(Long id);

    Freezer requireFreezer(Long id);

    Freezer createFreezer(Freezer freezer, Long roomId, String sysUserId);

    Freezer updateFreezer(Long id, Freezer updatedFreezer, Long roomId, String sysUserId);

    void setDeviceStatus(Long id, Boolean active);

    void deleteFreezer(Long id);
}
