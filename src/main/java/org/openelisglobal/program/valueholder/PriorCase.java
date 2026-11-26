package org.openelisglobal.program.valueholder;

import org.openelisglobal.sample.valueholder.Sample;

/**
 * A case of one bench as the prior results query reads it: its id, the name of
 * its stage and its order, without the case entity itself.
 */
public record PriorCase(AnatomicPathologyBench bench, Integer caseId, String status, Sample sample) {
}
