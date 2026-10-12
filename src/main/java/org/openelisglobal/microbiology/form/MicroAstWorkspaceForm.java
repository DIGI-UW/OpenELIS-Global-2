package org.openelisglobal.microbiology.form;

import java.util.List;

public class MicroAstWorkspaceForm {
    public boolean canEnter, canValidate;
    public List<Isolate> isolates;
    public List<Option> standards;
    public MicroDstForm dst;

    public record Option(String id, String label) {
    }

    public record Assignment(String id, String panelId, String label, String provenance) {
    }

    public record Choice(String antibioticId, String readingId) {
    }

    public record Isolate(String id, String label, String organism, String protocol, List<Option> panels,
            List<Assignment> assignments, List<MicroAstRunForm> runs, List<Choice> choices) {
    }

    public static class Action {
        @jakarta.validation.constraints.NotBlank
        public String isolateId;
        @jakarta.validation.constraints.NotBlank
        public String action;
        public String runId, readingId, panelId, technique, breakpointStandardId, antibioticId, interpretation, reason,
                analyzerCardId;
        public java.math.BigDecimal rawValue;
    }
}
