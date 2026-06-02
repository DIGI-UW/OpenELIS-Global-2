package org.openelisglobal.biorepository.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BiorepositoryQcRoundGenerationServiceImpl implements BiorepositoryQcRoundGenerationService {

    @Override
    public Map<String, Object> generateRound(Map<String, Object> storageOverview, int boxesPerRound, int samplesPerBox,
            Long seed, String freezerFilter, boolean allowAllDevices) {
        validateRoundParameters(boxesPerRound, samplesPerBox);
        validateDeviceScope(storageOverview, freezerFilter, allowAllDevices);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eligible = storageOverview.get("eligibleSamples") instanceof List
                ? (List<Map<String, Object>>) storageOverview.get("eligibleSamples")
                : List.of();

        if (eligible.isEmpty()) {
            throw new IllegalArgumentException(
                    "No eligible samples match the current storage scope. Adjust filters before generating.");
        }

        Map<String, List<Map<String, Object>>> candidatesByBox = new HashMap<>();
        for (Map<String, Object> row : eligible) {
            String boxKey = asString(row.get("boxKey"));
            if (boxKey == null) {
                continue;
            }
            candidatesByBox.computeIfAbsent(boxKey, key -> new ArrayList<>()).add(row);
        }

        if (candidatesByBox.isEmpty()) {
            throw new IllegalArgumentException(
                    "No eligible boxes match the current storage scope. Adjust filters before generating.");
        }

        int eligibleBoxCount = candidatesByBox.size();
        int uniqueEligibleSamples = countUniqueBioSampleIds(eligible);
        int requestedSamples = boxesPerRound * samplesPerBox;

        validatePoolCapacity(boxesPerRound, samplesPerBox, eligibleBoxCount, uniqueEligibleSamples, requestedSamples);

        String qcBatchId = "QCBATCH-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8);
        Random random = seed != null ? new Random(seed) : new Random();

        int boxesToSelect = Math.min(boxesPerRound, eligibleBoxCount);
        List<String> selectedBoxKeys = selectDistributedBoxKeys(candidatesByBox, boxesToSelect, random);

        Set<Integer> selectedBioSampleIds = new HashSet<>();
        List<Map<String, Object>> selectedSamples = new ArrayList<>();
        int maxSamples = Math.min(requestedSamples, uniqueEligibleSamples);

        for (String boxKey : selectedBoxKeys) {
            if (selectedSamples.size() >= maxSamples) {
                break;
            }
            List<Map<String, Object>> inBox = new ArrayList<>(candidatesByBox.getOrDefault(boxKey, List.of()));
            Collections.shuffle(inBox, random);
            int takenFromBox = 0;
            for (Map<String, Object> candidate : inBox) {
                if (selectedSamples.size() >= maxSamples || takenFromBox >= samplesPerBox) {
                    break;
                }
                Integer bioSampleId = toInteger(candidate.get("bioSampleId"));
                if (bioSampleId == null || !selectedBioSampleIds.add(bioSampleId)) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>(candidate);
                row.put("qcBatchId", qcBatchId);
                selectedSamples.add(row);
                takenFromBox++;
            }
        }

        if (selectedSamples.isEmpty()) {
            throw new IllegalArgumentException(
                    "Could not select samples for this round. Reduce boxes per round or samples per box.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("qcBatchId", qcBatchId);
        result.put("boxesSelected", selectedBoxKeys.size());
        result.put("samplesSelected", selectedSamples.size());
        result.put("boxesPerRound", boxesPerRound);
        result.put("samplesPerBox", samplesPerBox);
        result.put("seed", seed);
        result.put("filteredSamplePool", eligible.size());
        result.put("uniqueEligibleSamples", uniqueEligibleSamples);
        result.put("eligibleBoxes", eligibleBoxCount);
        result.put("requestedSamples", requestedSamples);
        result.put("samples", selectedSamples);
        return result;
    }

    @Override
    public Map<String, Object> toErrorBody(IllegalArgumentException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", exception.getMessage());
        return body;
    }

    static void validateRoundParameters(int boxesPerRound, int samplesPerBox) {
        if (samplesPerBox < MIN_SAMPLES_PER_BOX || samplesPerBox > MAX_SAMPLES_PER_BOX) {
            throw new IllegalArgumentException(
                    "samplesPerBox must be between " + MIN_SAMPLES_PER_BOX + " and " + MAX_SAMPLES_PER_BOX);
        }
        if (boxesPerRound < MIN_BOXES_PER_ROUND || boxesPerRound > MAX_BOXES_PER_ROUND) {
            throw new IllegalArgumentException(
                    "boxesPerRound must be between " + MIN_BOXES_PER_ROUND + " and " + MAX_BOXES_PER_ROUND);
        }
    }

    static void validateDeviceScope(Map<String, Object> storageOverview, String freezerFilter,
            boolean allowAllDevices) {
        int deviceCount = resolveDeviceCount(storageOverview);
        if (deviceCount > 1 && freezerFilter == null && !allowAllDevices) {
            throw new IllegalArgumentException(
                    "Select a device/freezer before generating a QC round when multiple devices exist.");
        }
    }

    static void validatePoolCapacity(int boxesPerRound, int samplesPerBox, int eligibleBoxCount,
            int uniqueEligibleSamples, int requestedSamples) {
        if (eligibleBoxCount < boxesPerRound) {
            throw new IllegalArgumentException(String.format(
                    "Not enough eligible boxes for this round. Requested %d box(es) but only %d eligible box(es) in scope.",
                    boxesPerRound, eligibleBoxCount));
        }
        if (uniqueEligibleSamples < requestedSamples) {
            throw new IllegalArgumentException(String.format(
                    "Not enough eligible samples for this round. Requested %d sample(s) (%d box(es) x %d per box) but only %d unique eligible sample(s) in scope.",
                    requestedSamples, boxesPerRound, samplesPerBox, uniqueEligibleSamples));
        }
    }

    @SuppressWarnings("unchecked")
    static int resolveDeviceCount(Map<String, Object> storageOverview) {
        Object filtersObj = storageOverview.get("filters");
        if (!(filtersObj instanceof Map)) {
            return 0;
        }
        Object freezersObj = ((Map<String, Object>) filtersObj).get("freezers");
        if (!(freezersObj instanceof List)) {
            return 0;
        }
        return ((List<?>) freezersObj).size();
    }

    static int countUniqueBioSampleIds(List<Map<String, Object>> eligible) {
        Set<Integer> ids = new HashSet<>();
        for (Map<String, Object> row : eligible) {
            Integer id = toInteger(row.get("bioSampleId"));
            if (id != null) {
                ids.add(id);
            }
        }
        return ids.size();
    }

    static List<String> selectDistributedBoxKeys(Map<String, List<Map<String, Object>>> candidatesByBox,
            int boxesToSelect, Random random) {
        Map<String, List<String>> boxesByShelf = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : candidatesByBox.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String shelfKey = asString(entry.getValue().get(0).get("shelfKey"));
            String bucket = shelfKey != null ? shelfKey : "Unknown Shelf";
            boxesByShelf.computeIfAbsent(bucket, key -> new ArrayList<>()).add(entry.getKey());
        }
        for (List<String> boxList : boxesByShelf.values()) {
            Collections.shuffle(boxList, random);
        }
        List<String> shelfKeys = new ArrayList<>(boxesByShelf.keySet());
        Collections.shuffle(shelfKeys, random);
        List<String> selected = new ArrayList<>();
        while (selected.size() < boxesToSelect) {
            boolean addedInCycle = false;
            for (String shelfKey : shelfKeys) {
                List<String> boxes = boxesByShelf.get(shelfKey);
                if (boxes == null || boxes.isEmpty()) {
                    continue;
                }
                selected.add(boxes.remove(0));
                addedInCycle = true;
                if (selected.size() >= boxesToSelect) {
                    break;
                }
            }
            if (!addedInCycle) {
                break;
            }
        }
        return selected;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return s.isBlank() ? null : s;
    }
}
