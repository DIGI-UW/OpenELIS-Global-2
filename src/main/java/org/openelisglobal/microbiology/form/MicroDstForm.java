package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;
import java.util.List;

public class MicroDstForm {
    public String classification;
    public boolean unresolvedDiscordance;
    public List<Drug> drugs = List.of();

    public record Evidence(String source, String sourceId, String interpretation, boolean validated) {
    }

    public record Drug(String antibioticId, String label, String role, String interpretation, boolean discordant,
            String evidenceFingerprint, String resolutionReason, String resolvedBy, Timestamp resolvedAt,
            List<Evidence> evidence) {
    }

    public static class ResolutionRequest {
        @jakarta.validation.constraints.NotBlank
        public String antibioticId;
        @jakarta.validation.constraints.NotBlank
        public String evidenceFingerprint;
        @jakarta.validation.constraints.Pattern(regexp = "SUSCEPTIBLE|RESISTANT")
        @jakarta.validation.constraints.NotBlank
        public String interpretation;
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 4000)
        public String reason;
    }

    public record Option(String id, String label) {
    }

    public record ComponentOption(String id, String label, List<Option> options) {
    }

    public record TestOption(String id, String label, List<ComponentOption> components) {
    }

    public static class Catalog {
        public List<Binding> bindings;
        public List<Option> antibiotics;
        public List<TestOption> tests;
    }

    public static class Binding {
        public String id, testId, componentId, antibioticId, susceptibleValue, resistantValue;
    }
}
