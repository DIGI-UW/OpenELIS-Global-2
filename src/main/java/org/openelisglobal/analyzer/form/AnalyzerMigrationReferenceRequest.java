package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AnalyzerMigrationReferenceRequest {

    @NotBlank
    private String profileId;

    @Min(1)
    private int profileRevision;

    @NotBlank
    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    private String profileFingerprint;

    @NotBlank
    private String bridgeConnectionId;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public int getProfileRevision() {
        return profileRevision;
    }

    public void setProfileRevision(int profileRevision) {
        this.profileRevision = profileRevision;
    }

    public String getProfileFingerprint() {
        return profileFingerprint;
    }

    public void setProfileFingerprint(String profileFingerprint) {
        this.profileFingerprint = profileFingerprint;
    }

    public String getBridgeConnectionId() {
        return bridgeConnectionId;
    }

    public void setBridgeConnectionId(String bridgeConnectionId) {
        this.bridgeConnectionId = bridgeConnectionId;
    }
}
