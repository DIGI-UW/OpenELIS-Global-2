package org.openelisglobal.program.valueholder.pathology;

/**
 * What staining found for a slide. A slide not yet stained has no status: the
 * bench record cannot tell a slide in the stainer from one still waiting.
 */
public enum StainStatus {

    /** The stain on the slide was applied and is fit to read. */
    STAINED,

    /**
     * The stain did not take well enough to read; the slide is restained, or a new
     * slide is cut and this one retired.
     */
    UNSATISFACTORY
}
