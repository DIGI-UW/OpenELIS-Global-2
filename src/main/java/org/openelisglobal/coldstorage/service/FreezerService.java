package org.openelisglobal.coldstorage.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FreezerService {

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<Freezer> getActiveFreezers();

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<Freezer> getAllFreezers(String search);

    /**
     * Every device that has ever existed, soft-deleted ones included. For the
     * reporting and audit-trail paths only - a device list cleanup must not rewrite
     * what a cold-chain inspector can pull.
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<Freezer> getAllFreezersForReporting();

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    Optional<Freezer> findByName(String name);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    Optional<Freezer> findById(Long id);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    Freezer requireFreezer(Long id);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    Freezer createFreezer(Freezer freezer, Long roomId, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    Freezer updateFreezer(Long id, Freezer updatedFreezer, Long roomId, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    Freezer updateThresholds(Long id, BigDecimal targetTemperature, BigDecimal warningThreshold,
            BigDecimal criticalThreshold, Integer pollingIntervalSeconds, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    void setDeviceStatus(Long id, Boolean active);

    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    void deleteFreezer(Long id);
}
