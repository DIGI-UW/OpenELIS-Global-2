package org.openelisglobal.addons.assistant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Suggested questions for a page. The service returns only an {@code id} and
 * values; the panel has the wording in each language. Dynamic suggestions come
 * from facts of the context (no model call).
 */
public final class Suggestions {

    private Suggestions() {
    }

    public record Suggestion(String id, Map<String, String> values) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("values", values);
            return m;
        }
    }

    private static Suggestion s(String id) {
        return new Suggestion(id, Map.of());
    }

    public static List<Suggestion> forContext(ContextText.Built ctx) {
        List<Suggestion> out = new ArrayList<>();
        switch (ctx.page()) {
        case ContextText.LOOKUP -> {
            if (!ctx.testKeys().isEmpty()) {
                out.add(s("abnormalSummary"));
                out.add(s("clusterThinking"));
                // dynamic: the first abnormal result (eGFR first)
                ctx.abnormal().stream().filter(a -> "1".equals(a.get("egfr"))).findFirst()
                        .ifPresent(a -> out.add(new Suggestion("egfrStage", Map.of("value", a.get("value")))));
                ctx.abnormal().stream().filter(a -> !"1".equals(a.get("egfr"))).findFirst()
                        .ifPresent(a -> out.add(new Suggestion("abnormalOne",
                                Map.of("test", a.get("test"), "value", a.get("value"), "unit", a.get("unit"),
                                        "direction", "H".equals(a.get("flag")) ? "high" : "low"))));
                out.add(s("moreTests"));
                out.add(s("patientAdvice"));
            }
        }
        case ContextText.VALIDATION -> {
            out.add(s("validationPriority"));
            out.add(s("validationDelta"));
        }
        default -> {
        }
        }
        out.add(s("whatIsScreen"));
        return out;
    }
}
