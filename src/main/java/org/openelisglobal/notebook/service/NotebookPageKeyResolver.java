package org.openelisglobal.notebook.service;

import org.openelisglobal.notebook.valueholder.NoteBookPage;

/**
 * Resolves stable workflow page keys (never page titles).
 */
public final class NotebookPageKeyResolver {

    private NotebookPageKeyResolver() {
    }

    public static String resolvePageKey(NoteBookPage page) {
        if (page == null) {
            return "";
        }
        if (page.getPageId() != null && !page.getPageId().isBlank()) {
            return page.getPageId().trim();
        }
        int order = page.getOrder() != null ? page.getOrder() : 0;
        return order > 0 ? "stage-" + order : "";
    }
}
