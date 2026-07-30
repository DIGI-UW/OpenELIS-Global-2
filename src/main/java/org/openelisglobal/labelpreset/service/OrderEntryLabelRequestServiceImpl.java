package org.openelisglobal.labelpreset.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dao.TestLabelConfigDAO;
import org.openelisglobal.labelpreset.dao.TestLabelPresetLinkDAO;
import org.openelisglobal.labelpreset.dto.LabelCell;
import org.openelisglobal.labelpreset.dto.LabelColumn;
import org.openelisglobal.labelpreset.dto.OrderEntryLabelRequestPayload;
import org.openelisglobal.labelpreset.dto.OrderEntryLabelRequestResponse;
import org.openelisglobal.labelpreset.dto.SourceType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.TestLabelConfig;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregation implementation for the Order Entry Labels section (OGC-285 M5).
 * Implements the FRS §4.4.1 conflict-resolution rules exactly as specified in
 * data-model.md §6.1:
 *
 * <ol>
 * <li>Per-order columns = active presets with {@code prints_per_order}.</li>
 * <li>Per-sample columns = distinct active {@code prints_per_sample} presets
 * that are either universal ({@code is_universal} — always shown per sample,
 * FR-014a) or linked by any test in {@code test_ids}.</li>
 * <li>Per (sample, preset) cell: {@code default = MAX(link.default_qty)} (or
 * {@code preset.default_per_sample} if unlinked); {@code max =
 * MAX(link.max_qty)} (or {@code preset.max_per_sample}); {@code locked = NOT
 * (AND(link.allow_override) AND config.allow_order_entry_override)} —
 * most-restrictive; {@code source = test} (with id+name of the link that drove
 * the default) when any link applies, else {@code preset_default}.</li>
 * <li>Per-order row: {@code default = preset.default_per_order}, {@code max =
 * preset.max_per_order}, {@code source = preset_default} (per-order is
 * lab-wide, not test-driven).</li>
 * <li>Columns sorted system-first (by id), then custom alphabetically.</li>
 * </ol>
 *
 * <p>
 * {@code @Transactional(readOnly = true)} — deterministic, no writes. Lazy
 * {@code Test} / {@code LabelPreset} associations on the link rows are resolved
 * within this transaction, so no LazyInitializationException escapes.
 */
@Service
public class OrderEntryLabelRequestServiceImpl implements OrderEntryLabelRequestService {

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestLabelPresetLinkDAO testLabelPresetLinkDAO;

    @Autowired
    private TestLabelConfigDAO testLabelConfigDAO;

    @Autowired
    private TestService testService;

    /**
     * Orders columns: system presets first (by id asc), then custom by name asc.
     */
    private static final Comparator<LabelPreset> COLUMN_ORDER = Comparator
            .comparing((LabelPreset p) -> Boolean.TRUE.equals(p.getIsSystem()) ? 0 : 1)
            .thenComparing(p -> Boolean.TRUE.equals(p.getIsSystem()) ? p.getId() : Integer.MAX_VALUE)
            .thenComparing(p -> p.getName() == null ? "" : p.getName(), String.CASE_INSENSITIVE_ORDER);

    @Override
    @Transactional(readOnly = true)
    public OrderEntryLabelRequestResponse computeLabelRequest(OrderEntryLabelRequestPayload payload) {
        OrderEntryLabelRequestResponse response = new OrderEntryLabelRequestResponse();
        if (payload == null) {
            return response;
        }

        List<String> testIds = normalizeTestIds(payload.getTestIds());

        // Active presets drive both the per-order columns (step 1) and the
        // always-on universal per-sample columns (step 2, FR-014a).
        List<LabelPreset> activePresets = labelPresetDAO.listActive();

        // --- Step 1: per-order columns = active presets with prints_per_order.
        List<LabelPreset> orderPresets = new ArrayList<>();
        for (LabelPreset preset : activePresets) {
            if (Boolean.TRUE.equals(preset.getPrintsPerOrder())) {
                orderPresets.add(preset);
            }
        }
        orderPresets.sort(COLUMN_ORDER);

        // --- Step 2: per-sample columns. Two sources, deduped by preset id:
        // (a) FR-014a universal per-sample presets — always a column for every
        // sample, independent of test links (links only OVERRIDE the qty in
        // step 3); (b) per-sample presets linked by any test in the order.
        // Contextual per-sample presets (Block / Slide / Freezer) are neither
        // universal nor test-linked here, so they stay out — they attach via the
        // M7 sample_type->preset model (FR-014b). Preserve first-seen, then sort.
        Map<Integer, LabelPreset> samplePresetsById = new LinkedHashMap<>();
        for (LabelPreset preset : activePresets) {
            if (Boolean.TRUE.equals(preset.getPrintsPerSample()) && Boolean.TRUE.equals(preset.getIsUniversal())) {
                samplePresetsById.putIfAbsent(preset.getId(), preset);
            }
        }
        // testId -> (presetId -> link) for the cell-level lookups in step 3.
        Map<String, List<TestLabelPresetLink>> linksByTest = new LinkedHashMap<>();
        for (String testId : testIds) {
            List<TestLabelPresetLink> links = testLabelPresetLinkDAO.listByTestId(testId);
            linksByTest.put(testId, links);
            for (TestLabelPresetLink link : links) {
                LabelPreset preset = link.getPreset();
                if (preset != null && Boolean.TRUE.equals(preset.getIsActive())
                        && Boolean.TRUE.equals(preset.getPrintsPerSample())) {
                    samplePresetsById.putIfAbsent(preset.getId(), preset);
                }
            }
        }
        List<LabelPreset> samplePresets = new ArrayList<>(samplePresetsById.values());
        samplePresets.sort(COLUMN_ORDER);

        // --- Build columns.
        for (LabelPreset preset : orderPresets) {
            response.getOrderColumns().add(toColumn(preset, preset.getMaxPerOrder()));
        }
        for (LabelPreset preset : samplePresets) {
            response.getSampleColumns().add(toColumn(preset, preset.getMaxPerSample()));
        }

        // --- Step 4: per-order row cells (one per per-order preset).
        for (LabelPreset preset : orderPresets) {
            response.getOrderRow().getCells().add(buildOrderCell(preset));
        }

        // --- Step 3: per-sample rows. Resolve allow_override config once per test.
        Map<String, Boolean> configOverrideByTest = resolveConfigOverrides(testIds);

        if (payload.getSamples() != null) {
            for (OrderEntryLabelRequestPayload.SampleRef sampleRef : payload.getSamples()) {
                OrderEntryLabelRequestResponse.SampleRow row = new OrderEntryLabelRequestResponse.SampleRow(
                        sampleRef.getSampleIdLocal());
                for (LabelPreset preset : samplePresets) {
                    row.getCells().add(buildSampleCell(preset, testIds, linksByTest, configOverrideByTest));
                }
                response.getSampleRows().add(row);
            }
        }

        return response;
    }

    private List<String> normalizeTestIds(List<Long> rawTestIds) {
        List<String> testIds = new ArrayList<>();
        if (rawTestIds != null) {
            for (Long id : rawTestIds) {
                if (id != null) {
                    testIds.add(String.valueOf(id));
                }
            }
        }
        return testIds;
    }

    private LabelColumn toColumn(LabelPreset preset, Integer max) {
        return new LabelColumn(preset.getId(), preset.getName(), Boolean.TRUE.equals(preset.getIsSystem()),
                max == null ? 0 : max);
    }

    /** Step 4: per-order cell — preset-driven, no test source. */
    private LabelCell buildOrderCell(LabelPreset preset) {
        LabelCell cell = new LabelCell();
        cell.setPresetId(preset.getId());
        cell.setDefaultQty(preset.getDefaultPerOrder() == null ? 0 : preset.getDefaultPerOrder());
        cell.setMax(preset.getMaxPerOrder() == null ? 0 : preset.getMaxPerOrder());
        // Per-order is lab-wide, never test-driven, and never order-entry-locked.
        cell.setLocked(false);
        cell.setSource(SourceType.PRESET_DEFAULT);
        return cell;
    }

    /** Step 3: per (sample, preset) cell, resolved across all linking tests. */
    private LabelCell buildSampleCell(LabelPreset preset, List<String> testIds,
            Map<String, List<TestLabelPresetLink>> linksByTest, Map<String, Boolean> configOverrideByTest) {
        LabelCell cell = new LabelCell();
        cell.setPresetId(preset.getId());

        Integer maxDefaultQty = null;
        Integer maxMaxQty = null;
        boolean allowOverride = true;
        boolean anyLink = false;
        Test sourceTest = null;
        int sourceDefaultQty = Integer.MIN_VALUE;

        for (String testId : testIds) {
            for (TestLabelPresetLink link : linksByTest.getOrDefault(testId, List.of())) {
                if (link.getPreset() == null || !preset.getId().equals(link.getPreset().getId())) {
                    continue;
                }
                anyLink = true;
                int linkDefault = link.getDefaultQty() == null ? 0 : link.getDefaultQty();
                int linkMax = link.getMaxQty() == null ? 0 : link.getMaxQty();
                maxDefaultQty = (maxDefaultQty == null) ? linkDefault : Math.max(maxDefaultQty, linkDefault);
                maxMaxQty = (maxMaxQty == null) ? linkMax : Math.max(maxMaxQty, linkMax);
                // Most-restrictive: any false link override OR a test whose config
                // disables order-entry override locks the cell.
                allowOverride = allowOverride && Boolean.TRUE.equals(link.getAllowOverride())
                        && Boolean.TRUE.equals(configOverrideByTest.getOrDefault(testId, Boolean.TRUE));
                // The link that drove the (highest) default is the cell's source test.
                if (linkDefault > sourceDefaultQty) {
                    sourceDefaultQty = linkDefault;
                    sourceTest = resolveTest(link, testId);
                }
            }
        }

        if (anyLink) {
            cell.setDefaultQty(maxDefaultQty == null ? 0 : maxDefaultQty);
            cell.setMax(maxMaxQty == null ? 0 : maxMaxQty);
            cell.setLocked(!allowOverride);
            cell.setSource(SourceType.TEST);
            if (sourceTest != null) {
                cell.setSourceTestId(parseTestId(sourceTest.getId()));
                cell.setSourceTestName(sourceTest.getName());
            }
        } else {
            // FR-014 system-default fallback: no linking test → preset defaults.
            cell.setDefaultQty(preset.getDefaultPerSample() == null ? 0 : preset.getDefaultPerSample());
            cell.setMax(preset.getMaxPerSample() == null ? 0 : preset.getMaxPerSample());
            cell.setLocked(false);
            cell.setSource(SourceType.PRESET_DEFAULT);
        }
        return cell;
    }

    /**
     * The {@code allow_order_entry_override} for each test (default true if no
     * config row exists yet — lazily-created config defaults to true).
     */
    private Map<String, Boolean> resolveConfigOverrides(List<String> testIds) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String testId : testIds) {
            Optional<TestLabelConfig> config = testLabelConfigDAO.getByTestId(testId);
            result.put(testId, config.map(TestLabelConfig::getAllowOrderEntryOverride).orElse(Boolean.TRUE));
        }
        return result;
    }

    /**
     * Resolve the {@link Test} for a link's source-test tag. Prefer the link's lazy
     * association; fall back to the {@link TestService} lookup by the iterating
     * test id (which is always non-null within the read-only transaction).
     */
    private Test resolveTest(TestLabelPresetLink link, String testId) {
        if (link.getTest() != null) {
            return link.getTest();
        }
        return testService.getTestById(testId);
    }

    private Long parseTestId(String testId) {
        if (testId == null) {
            return null;
        }
        try {
            return Long.valueOf(testId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
