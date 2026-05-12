package org.openelisglobal.notebook.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PathologyWorkflowTypeConfig {

    public static final String HISTOPATHOLOGY_BIOPSY = "histopathology_biopsy_tissue";
    public static final String PERIPHERAL_SMEAR_BONE_MARROW = "peripheral_smear_bone_marrow_morphology";
    public static final String FNAC = "fnac";
    public static final String CYTOLOGY_LIQUID_PAP = "cytology_liquid_based_pap_smear";

    public static final String WORKFLOW_DATA_KEY = "pathologyWorkflowTypes";
    public static final String ENABLED_STAGES_KEY = "enabledStages";

    private static final Set<Integer> ALL_STAGES = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);

    private static final Map<String, Set<Integer>> WORKFLOW_STAGE_MAP = buildStageMap();
    private static final Map<String, Integer> CANONICAL_ORDER_BY_NORMALIZED_TITLE = buildCanonicalOrderByTitleMap();

    private PathologyWorkflowTypeConfig() {
    }

    public static boolean isPathologyWorkflowType(String workflowType) {
        return normalizeWorkflowType(workflowType) != null;
    }

    public static String normalizeWorkflowType(String workflowType) {
        if (workflowType == null) {
            return null;
        }
        String normalized = workflowType.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }

        if (WORKFLOW_STAGE_MAP.containsKey(normalized)) {
            return normalized;
        }

        switch (normalized) {
            case "histopathology":
            case "biopsy":
            case "histopathology/biopsy":
            case "histopathology_biopsy":
                return HISTOPATHOLOGY_BIOPSY;
            case "peripheral_smear":
            case "bone_marrow":
            case "peripheral_smear_bone_marrow":
                return PERIPHERAL_SMEAR_BONE_MARROW;
            case "cytology":
            case "liquid_based_pap_smear":
            case "pap_smear":
                return CYTOLOGY_LIQUID_PAP;
            default:
                return null;
        }
    }

    public static String normalizeOrDefault(String workflowType) {
        String normalized = normalizeWorkflowType(workflowType);
        return normalized != null ? normalized : HISTOPATHOLOGY_BIOPSY;
    }

    public static Set<Integer> enabledStagesFor(String workflowType) {
        String normalized = normalizeOrDefault(workflowType);
        return WORKFLOW_STAGE_MAP.getOrDefault(normalized, ALL_STAGES);
    }

    public static boolean isStageEnabled(String workflowType, Integer pageOrder) {
        if (pageOrder == null) {
            return true;
        }
        return enabledStagesFor(workflowType).contains(pageOrder);
    }

    public static Integer canonicalStageOrder(String pageTitle, Integer pageOrder) {
        String normalizedTitle = normalizeStageTitle(pageTitle);
        if (normalizedTitle != null) {
            Integer canonicalOrder = CANONICAL_ORDER_BY_NORMALIZED_TITLE.get(normalizedTitle);
            if (canonicalOrder != null) {
                return canonicalOrder;
            }
        }
        return pageOrder;
    }

    public static boolean isStageEnabledForPage(String workflowType, String pageTitle, Integer pageOrder) {
        Integer canonicalOrder = canonicalStageOrder(pageTitle, pageOrder);
        if (canonicalOrder == null) {
            return true;
        }
        return enabledStagesFor(workflowType).contains(canonicalOrder);
    }

    public static Map<String, Set<Integer>> workflowStageMap() {
        return WORKFLOW_STAGE_MAP;
    }

    private static Map<String, Set<Integer>> buildStageMap() {
        Map<String, Set<Integer>> mutable = new LinkedHashMap<>();
        mutable.put(HISTOPATHOLOGY_BIOPSY, ALL_STAGES);
        mutable.put(PERIPHERAL_SMEAR_BONE_MARROW, Set.of(1, 2, 7, 8, 9, 10, 11, 12, 13));
        mutable.put(FNAC, Set.of(1, 2, 7, 8, 9, 10, 11, 12, 13));
        mutable.put(CYTOLOGY_LIQUID_PAP, Set.of(1, 2, 5, 7, 8, 9, 10, 11, 12, 13));
        return Collections.unmodifiableMap(mutable);
    }

    private static Map<String, Integer> buildCanonicalOrderByTitleMap() {
        Map<String, Integer> mutable = new LinkedHashMap<>();
        mutable.put("sample creation and full metadata capture", 1);
        mutable.put("sample creation and metadata capture", 1);
        mutable.put("sample creation metadata capture", 1);
        mutable.put("sample quality control", 2);
        mutable.put("gross examination", 3);
        mutable.put("cassette setup", 4);
        mutable.put("sample processing", 5);
        mutable.put("block creation", 6);
        mutable.put("slide preparation", 7);
        mutable.put("slide staining", 8);
        mutable.put("microscopy and diagnosis", 9);
        mutable.put("microscopy diagnosis", 9);
        mutable.put("individual patient report preview and print", 10);
        mutable.put("storage and inventory management", 11);
        mutable.put("reporting and performance monitoring", 12);
        mutable.put("disposal and archiving", 13);
        return Collections.unmodifiableMap(mutable);
    }

    private static String normalizeStageTitle(String pageTitle) {
        if (pageTitle == null) {
            return null;
        }

        String normalized = pageTitle.toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        return normalized.isEmpty() ? null : normalized;
    }
}
