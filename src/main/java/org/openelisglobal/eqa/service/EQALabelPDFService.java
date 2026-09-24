package org.openelisglobal.eqa.service;

/**
 * Blind-code label sheets for in-house panels (OGC-612): Avery 5160-equivalent
 * stock (30 per letter sheet), each label carrying the blind code, cycle
 * identifier and analyte name — and nothing else, so a dropped label cannot
 * leak a target. Regeneration is byte-identical.
 */
public interface EQALabelPDFService {

    byte[] generateLabelSheet(Long panelId);

    /** Labels the sheet carries, for the print audit. */
    int countLabels(Long panelId);
}
