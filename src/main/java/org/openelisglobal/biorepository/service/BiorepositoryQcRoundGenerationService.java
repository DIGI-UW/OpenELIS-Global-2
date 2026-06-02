package org.openelisglobal.biorepository.service;

import java.util.Map;

/**
 * Random QC round generation from a storage-overview eligible pool.
 */
public interface BiorepositoryQcRoundGenerationService {

    int MAX_BOXES_PER_ROUND = 200;

    int MAX_SAMPLES_PER_BOX = 200;

    int MIN_BOXES_PER_ROUND = 1;

    int MIN_SAMPLES_PER_BOX = 1;

    /**
     * Validates round parameters and selects unique samples across distributed boxes.
     *
     * @param storageOverview   output of storage overview (counts, filters, eligibleSamples)
     * @param boxesPerRound     requested box count
     * @param samplesPerBox     requested samples per box
     * @param seed              optional random seed
     * @param freezerFilter     normalized freezer/device filter (null = all devices)
     * @param allowAllDevices   when true, multi-device sites may generate without a device filter
     * @return round payload including qcBatchId and samples
     * @throws IllegalArgumentException when validation fails; use {@link #toErrorBody} for API mapping
     */
    Map<String, Object> generateRound(Map<String, Object> storageOverview, int boxesPerRound, int samplesPerBox,
            Long seed, String freezerFilter, boolean allowAllDevices);

    /**
     * Build a structured error body for HTTP 400 responses.
     */
    Map<String, Object> toErrorBody(IllegalArgumentException exception);
}
