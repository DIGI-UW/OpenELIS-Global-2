package org.openelisglobal.program.valueholder.pathology;

/**
 * A coded conclusion as the screen names it: the dictionary entry's id, its
 * name in the user's language, and its code (the entry's local abbreviation,
 * empty when it has none).
 */
public record ConclusionEntry(String id, String value, String code) {
}
