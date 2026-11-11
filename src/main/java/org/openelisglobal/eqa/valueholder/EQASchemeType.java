package org.openelisglobal.eqa.valueholder;

/**
 * Arrangement-type axis of a scheme: who runs it and who participates. The
 * test-domain axis lives in the standard test catalog, not here. A provider is
 * required for every type except IN_HOUSE.
 */
public enum EQASchemeType {
    INTERNATIONAL_PT, REGIONAL_PT, INTER_LAB_SPLIT, IN_HOUSE
}
