package org.openelisglobal.sample.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.sampletyperequest.dto.SampleTypeRequestDTO;
import org.openelisglobal.test.valueholder.Test;

/**
 * The tests an order save lists for each specimen the order already has, and
 * the analyses that save takes off.
 *
 * <p>
 * A specimen in the sample manifest takes its tests from there; otherwise from
 * its requested-specimen row (the Enter Order step sends tests only there). A
 * specimen the save does not list, or lists with no tests, keeps its tests, and
 * an analysis another analysis or result produced (reflex, microbiology
 * work-up) is never taken off by an order save.
 */
public final class OrderTestEdits {

    private OrderTestEdits() {
    }

    public static Map<String, Set<String>> requestedTestsBySampleItem(List<SampleTestCollection> collections,
            List<SampleTypeRequestDTO> requestedSampleTypes, Function<String, List<String>> panelTests) {
        Map<String, Set<String>> bySampleItem = new HashMap<>();
        if (collections != null) {
            for (SampleTestCollection collection : collections) {
                if (collection == null || GenericValidator.isBlankOrNull(collection.existingSampleItemId)) {
                    continue;
                }
                Set<String> tests = testIds(collection.tests);
                if (!tests.isEmpty()) {
                    bySampleItem.computeIfAbsent(collection.existingSampleItemId, id -> new HashSet<>()).addAll(tests);
                }
            }
        }
        Set<String> inManifest = manifestSampleItemIds(collections);
        if (requestedSampleTypes != null) {
            for (SampleTypeRequestDTO requested : requestedSampleTypes) {
                if (requested == null || GenericValidator.isBlankOrNull(requested.getSampleItemId())
                        || inManifest.contains(requested.getSampleItemId())) {
                    continue;
                }
                Set<String> tests = requestedTests(requested.getRequestedTests(), requested.getRequestedPanels(),
                        panelTests);
                if (!tests.isEmpty()) {
                    bySampleItem.computeIfAbsent(requested.getSampleItemId(), id -> new HashSet<>()).addAll(tests);
                }
            }
        }
        return bySampleItem;
    }

    public static Set<String> manifestSampleItemIds(List<SampleTestCollection> collections) {
        Set<String> ids = new HashSet<>();
        if (collections != null) {
            for (SampleTestCollection collection : collections) {
                if (collection != null && !GenericValidator.isBlankOrNull(collection.existingSampleItemId)) {
                    ids.add(collection.existingSampleItemId);
                }
            }
        }
        return ids;
    }

    public static Set<String> requestedTests(String testIds, String panelIds,
            Function<String, List<String>> panelTests) {
        Set<String> tests = new LinkedHashSet<>(splitIds(testIds));
        for (String panelId : splitIds(panelIds)) {
            tests.addAll(panelTests.apply(panelId));
        }
        return tests;
    }

    public static boolean isTakenOff(Analysis analysis, Map<String, Set<String>> requestedBySampleItem,
            String cancelledStatusId) {
        if (analysis == null || analysis.getTest() == null || analysis.getSampleItem() == null
                || analysis.getParentAnalysis() != null || analysis.getParentResult() != null) {
            return false;
        }
        if (cancelledStatusId != null && cancelledStatusId.equals(analysis.getStatusId())) {
            return false;
        }
        Set<String> requested = requestedBySampleItem.get(analysis.getSampleItem().getId());
        return requested != null && !requested.contains(analysis.getTest().getId());
    }

    /**
     * True when the order screen lists a collected sample with no tests while the
     * sample still has ordered tests: that is a rejection or a cancelled order, not
     * a test removal.
     */
    public static boolean leavesACollectedSampleWithoutTests(List<SampleTestCollection> collections,
            List<SampleTypeRequestDTO> requestedSampleTypes, List<Analysis> existingAnalyses,
            String cancelledStatusId) {
        if (requestedSampleTypes == null || existingAnalyses == null) {
            return false;
        }
        Set<String> inManifest = manifestSampleItemIds(collections);
        for (SampleTypeRequestDTO requested : requestedSampleTypes) {
            if (requested == null || GenericValidator.isBlankOrNull(requested.getSampleItemId())
                    || inManifest.contains(requested.getSampleItemId())
                    || !GenericValidator.isBlankOrNull(requested.getRequestedTests())
                    || !GenericValidator.isBlankOrNull(requested.getRequestedPanels())) {
                continue;
            }
            for (Analysis analysis : existingAnalyses) {
                if (analysis.getSampleItem() != null
                        && requested.getSampleItemId().equals(analysis.getSampleItem().getId())
                        && analysis.getParentAnalysis() == null && analysis.getParentResult() == null
                        && !java.util.Objects.equals(cancelledStatusId, analysis.getStatusId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<String> testIds(List<Test> tests) {
        Set<String> ids = new HashSet<>();
        if (tests != null) {
            for (Test test : tests) {
                if (test != null && test.getId() != null) {
                    ids.add(test.getId());
                }
            }
        }
        return ids;
    }

    private static List<String> splitIds(String ids) {
        return ids == null ? List.of()
                : java.util.Arrays.stream(ids.split(",")).map(String::trim).filter(id -> !id.isEmpty()).toList();
    }
}
