package org.openelisglobal.notebook.valueholder;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * SRS notebook stage actions (authorization is never inferred from page titles).
 */
public enum NotebookStageAction {
    VIEW,
    EDIT,
    COMPLETE;

    public static final Set<NotebookStageAction> DEFAULT_STAGE_ACTIONS = Collections
            .unmodifiableSet(EnumSet.of(VIEW, EDIT, COMPLETE));

    public static NotebookStageAction fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return NotebookStageAction.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Set<NotebookStageAction> parseActions(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_STAGE_ACTIONS;
        }
        EnumSet<NotebookStageAction> actions = EnumSet.noneOf(NotebookStageAction.class);
        Arrays.stream(raw.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).forEach(part -> {
            NotebookStageAction action = fromString(part);
            if (action != null) {
                actions.add(action);
            }
        });
        return actions.isEmpty() ? DEFAULT_STAGE_ACTIONS : Collections.unmodifiableSet(actions);
    }
}
