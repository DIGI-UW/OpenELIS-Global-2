package org.openelisglobal.analyzer.valueholder;

/**
 * The Bridge profile revision an analyzer's mapping is pinned to. A pin is a
 * profile and a revision; its fingerprint is the content last seen, a change
 * detector that never refuses.
 */
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

    /**
     * Whether this pin names the given revision, whatever content either side last
     * saw.
     */
    public boolean names(String otherProfileId, int otherRevision) {
        return profileId != null && profileId.equals(otherProfileId) && profileRevision == otherRevision;
    }
}
