package org.openelisglobal.program.valueholder.pathology;

/**
 * A coded conclusion as the screen names it: the dictionary entry's id, its
 * name in the user's language, its code (the entry's local abbreviation, empty
 * when it has none), and whether the code starts with one of the starts the
 * laboratory marked critical.
 */
public record ConclusionEntry(String id, String value, String code, boolean critical) {
}
