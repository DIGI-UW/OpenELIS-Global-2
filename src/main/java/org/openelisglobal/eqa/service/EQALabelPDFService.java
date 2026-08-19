package org.openelisglobal.eqa.service;

/**
 * Blind-code label sheets for in-house panels (OGC-612, FR-V2.4-13): Avery
 * 5160-equivalent stock (30 per letter sheet), each label carrying the blind
 * code, cycle identifier and analyte name — and nothing else, so a dropped
 * label cannot leak a target (AC-V2.4-14). Regeneration is byte-identical
 * (AC-V2.4-15).
 */
public interface EQALabelPDFService {

    byte[] generateLabelSheet(Long panelId);

    /** Labels the sheet carries, for the print audit FR-V2.4-13 asks for. */
    int countLabels(Long panelId);
}
