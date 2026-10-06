package org.openelisglobal.analyzer.valueholder;

/** The exact Bridge profile revision an analyzer's mapping is pinned to. */
public final class AnalyzerProfilePin {

    private final String profileId;
    private final int profileRevision;
    private final String profileFingerprint;

    public AnalyzerProfilePin(String profileId, int profileRevision, String profileFingerprint) {
        this.profileId = profileId;
        this.profileRevision = profileRevision;
        this.profileFingerprint = profileFingerprint;
    }

    public String getProfileId() {
        return profileId;
    }

    public int getProfileRevision() {
        return profileRevision;
    }

    public String getProfileFingerprint() {
        return profileFingerprint;
    }
}
