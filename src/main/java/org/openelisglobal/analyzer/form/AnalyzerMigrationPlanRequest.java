package org.openelisglobal.analyzer.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;

public class AnalyzerMigrationPlanRequest {

    @NotBlank
    private String runId;

    @Valid
    @NotEmpty
    private List<Decision> decisions = new ArrayList<>();

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public List<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<Decision> decisions) {
        this.decisions = decisions;
    }

    public static class Decision {

        public enum Action {
            MIGRATE, EXCLUDE
        }

        @NotBlank
        private String sourceAnalyzerId;

        @NotNull
        private Action action;

        private String profileId;
        private int profileRevision;

        @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
        private String profileFingerprint;

        private String bridgeConnectionId;
        private String reasonCode;

        public String getSourceAnalyzerId() {
            return sourceAnalyzerId;
        }

        public void setSourceAnalyzerId(String sourceAnalyzerId) {
            this.sourceAnalyzerId = sourceAnalyzerId;
        }

        public Action getAction() {
            return action;
        }

        public void setAction(Action action) {
            this.action = action;
        }

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

        public String getReasonCode() {
            return reasonCode;
        }

        public void setReasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
        }
    }
}
