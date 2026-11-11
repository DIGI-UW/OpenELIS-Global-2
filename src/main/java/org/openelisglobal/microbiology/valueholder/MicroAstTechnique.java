package org.openelisglobal.microbiology.valueholder;

/** Laboratory technique used for an AST attempt. */
public enum MicroAstTechnique {
    VITEK_2(MicroAstMethod.MIC), PHOENIX(MicroAstMethod.MIC), ETEST(MicroAstMethod.MIC),
    BROTH_MICRODILUTION(MicroAstMethod.MIC), DISK_DIFFUSION(MicroAstMethod.ZONE),
    LEGACY_UNSPECIFIED_MIC(MicroAstMethod.MIC), LEGACY_UNSPECIFIED_ZONE(MicroAstMethod.ZONE);

    private final MicroAstMethod measurementType;

    MicroAstTechnique(MicroAstMethod measurementType) {
        this.measurementType = measurementType;
    }

    public MicroAstMethod measurementType() {
        return measurementType;
    }

    public boolean isLegacyUnspecified() {
        return this == LEGACY_UNSPECIFIED_MIC || this == LEGACY_UNSPECIFIED_ZONE;
    }

    public static MicroAstTechnique legacyFor(MicroAstMethod method) {
        return MicroAstMethod.ZONE.equals(method) ? LEGACY_UNSPECIFIED_ZONE : LEGACY_UNSPECIFIED_MIC;
    }
}
