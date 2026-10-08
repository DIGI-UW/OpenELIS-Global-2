package org.openelisglobal.orderentry.service;

import java.util.List;
import java.util.Map;

/**
 * What the test catalog says a sample needs (FRS clinical order entry v4,
 * FR-C9a, the Required line of the Handling group): per test, the storage
 * condition from the Storage section and the holding time in minutes.
 */
public interface HandlingRequirementService {

    /**
     * One entry per known test id, with {@code testId}, {@code storageCondition},
     * {@code storageConditionCustom} and {@code holdingMinutes} (null when the
     * catalog sets none). Unknown or malformed ids are skipped.
     */
    List<Map<String, Object>> requirements(List<String> testIds);
}
