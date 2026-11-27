package org.openelisglobal.program.valueholder.pathology;

import java.util.List;

/**
 * The answer of the diagnosis search: the first entries that match, and how
 * many entries match in all.
 */
public record ConclusionEntries(List<ConclusionEntry> items, int total) {
}
