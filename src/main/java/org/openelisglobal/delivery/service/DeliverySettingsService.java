package org.openelisglobal.delivery.service;

import java.util.List;

public interface DeliverySettingsService {

    DeliverySettings get();

    /** Validates and stores the settings; returns what is now stored. */
    DeliverySettings save(DeliverySettings settings, String sysUserId);

    /** The retry delays in minutes, in order; never empty. */
    List<Integer> retryDelays();
}
