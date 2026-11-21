package org.openelisglobal.program.valueholder.pathology;

/**
 * What bench work a pathologist's request asks for (FR-10.3). A request on file
 * from before kinds were recorded has none: it was picked from a list of stain
 * names and nobody can say now which of these it meant.
 */
public enum RequestKind {

    /** Another block cut from the specimen; it names no block or slide. */
    BLOCK,

    /** Another slide cut from a block of the case, which it names. */
    SLIDE,

    /** A stain applied to a slide of the case, which it names with the stain. */
    STAIN
}
