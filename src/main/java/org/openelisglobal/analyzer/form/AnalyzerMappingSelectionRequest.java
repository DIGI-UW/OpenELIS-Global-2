package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AnalyzerMappingSelectionRequest {

    @NotBlank
    private String mappingId;

    @Min(1)
    private int revision;

    @NotBlank
    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    private String mappingFingerprint;

    public String getMappingId() {
        return mappingId;
    }

    public void setMappingId(String mappingId) {
        this.mappingId = mappingId;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public String getMappingFingerprint() {
        return mappingFingerprint;
    }

    public void setMappingFingerprint(String mappingFingerprint) {
        this.mappingFingerprint = mappingFingerprint;
    }
}
