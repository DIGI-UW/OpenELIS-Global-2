package org.openelisglobal.program.util;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;

/**
 * What a pathology case must hold before a report is generated or the case is
 * signed out, in one place so the two never disagree.
 */
public final class PathologyCaseRules {

    private PathologyCaseRules() {
    }

    /** Both the macroscopic and the microscopic description are written. */
    public static boolean hasDescriptions(PathologySample pathologySample) {
        return StringUtils.isNotBlank(pathologySample.getGrossExam())
                && StringUtils.isNotBlank(pathologySample.getMicroscopyExam());
    }

    /** A coded diagnosis, or a narrative conclusion that is not blank. */
    public static boolean hasConclusion(PathologySample pathologySample) {
        List<PathologyConclusion> conclusions = pathologySample.getConclusions();
        return conclusions != null && conclusions.stream()
                .anyMatch(conclusion -> conclusion.getType() == ConclusionType.DICTIONARY
                        || (conclusion.getType() == ConclusionType.TEXT
                                && StringUtils.isNotBlank(conclusion.getValue())));
    }
}
