package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

public class MicroIsolateWorkspaceForm {
    public List<Isolate> isolates;
    public List<Referral> referrals;
    public boolean referred;

    public record Isolate(String id, String label, String cultureId, String cultureLabel, String sourceSampleItemId,
            String sampleItemId, String sampleItemLabel, String gramStain, String colonyMorphology, String significance,
            String organismId, String organismName, String status, String method, BigDecimal confidence,
            Timestamp identifiedAt, String createdBy, Timestamp createdAt, boolean received,
            String sendingOrganizationId, String sendingOrganizationName, String reportedOrganismId,
            String reportedOrganismText, boolean mismatch, String mismatchAcknowledgedBy,
            Timestamp mismatchAcknowledgedAt, boolean canIdentify, boolean canReidentify, boolean canAcknowledge,
            String version, List<Identification> history) {
    }

    public record Identification(String id, String organismId, String organismName, String status, String method,
            BigDecimal confidence, Timestamp identifiedAt, String by, Timestamp at, String reason, String amendmentId) {
    }

    public record Referral(String id, String sampleItemId, String testName, String organizationName, String status,
            Timestamp sentAt, String transportDetails) {
    }

    public static class Options {
        public List<IdValuePair> organisms, sampleTypes, organizations, reasons, methods;
        public List<Test> tests;
        public List<Source> sources;
    }

    public record Source(String id, String value, String sampleTypeId) {
    }

    public record Test(String id, String name, List<String> sampleTypeIds) {
    }
}
