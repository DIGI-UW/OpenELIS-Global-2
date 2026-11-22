package org.openelisglobal.resultrun.form;

import java.util.Set;

/**
 * The run view's filter chips (OGC-1200 FR-E1a). Needs review gathers every row
 * a reviewer must look at before accepting.
 */
public enum ResultRunChip {
    ALL(Set.of(ResultRunRowCategory.values())),
    NEEDS_REVIEW(Set.of(ResultRunRowCategory.EXCEPTION, ResultRunRowCategory.HELD, ResultRunRowCategory.CRITICAL,
            ResultRunRowCategory.ABNORMAL)),
    CRITICAL(Set.of(ResultRunRowCategory.CRITICAL)), ABNORMAL(Set.of(ResultRunRowCategory.ABNORMAL)),
    EXCEPTIONS(Set.of(ResultRunRowCategory.EXCEPTION)), HELD(Set.of(ResultRunRowCategory.HELD)),
    NORMAL(Set.of(ResultRunRowCategory.NORMAL));

    private final Set<ResultRunRowCategory> categories;

    ResultRunChip(Set<ResultRunRowCategory> categories) {
        this.categories = categories;
    }

    public boolean matches(ResultRunRowCategory category) {
        return categories.contains(category);
    }

    /** The chip named in a request; All when it names none or an unknown one. */
    public static ResultRunChip fromRequest(String name) {
        if (name == null || name.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(name.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException unknown) {
            return ALL;
        }
    }
}
