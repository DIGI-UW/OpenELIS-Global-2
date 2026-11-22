package org.openelisglobal.configuration.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * The import page's two steps: preview what a set of catalog files would do,
 * then apply them. Applying keeps the files in the configuration tree, so the
 * next start-up loads exactly what was applied and the GUI and file paths stay
 * one mechanism.
 */
public interface CatalogImportService {

    /** What one file would do, row by row. */
    record FilePlan(String domain, String fileName, int created, int updated, int skipped, List<RowPlan> rows,
            String error) {
    }

    /**
     * One row's fate in a plan: its line number, outcome and, when skipped, why.
     */
    record RowPlan(int lineNumber, String outcome, String reason) {
    }

    /**
     * A preview or an apply: the files, and what still needs a decision afterwards.
     */
    record ImportPlan(String importRunId, List<FilePlan> files, int unresolvedCount) {
    }

    /**
     * Evaluates every file against the current database and keeps nothing.
     * {@code domains} may name each file's domain in order; a blank or missing
     * entry falls back to the file name's domain prefix.
     */
    ImportPlan preview(List<MultipartFile> files, List<String> domains, String sysUserId);

    /**
     * Saves the files into the configuration tree and loads their domains, in
     * dependency order. Only the uploaded files are read again, and they are read
     * whatever their checksum says, since the person uploading them asked for that
     * explicitly; the rest of the catalog already in the tree is left alone.
     */
    ImportPlan apply(List<MultipartFile> files, List<String> domains, String sysUserId);

    /** The catalog domains that can be imported, in load order. */
    List<String> getImportableDomains();
}
