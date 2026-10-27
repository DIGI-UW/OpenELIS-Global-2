package org.openelisglobal.addons.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The prompt from {@link Rules}: the system prompt, and a user message with the
 * de-identified context, the related reference tables and the question.
 */
public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static List<String> tableKeys(ContextText.Built ctx) {
        return new ArrayList<>(ReferenceTables.select(ctx.testKeys()));
    }

    public static String userMessage(Rules rules, ContextText.Built ctx, String question) {
        return userMessage(rules, ctx, question, Lang.VI);
    }

    public static String userMessage(Rules rules, ContextText.Built ctx, String question, Lang lang) {
        StringBuilder user = new StringBuilder();
        user.append(lang.vi() ? "NGỮ CẢNH (đã ẩn danh):\n" : "CONTEXT (de-identified):\n").append(ctx.text().strip())
                .append("\n\n");
        List<String> tables = new ArrayList<>();
        for (String k : tableKeys(ctx)) {
            String t = rules.tables.get(k);
            if (t != null) {
                tables.add(t);
            }
        }
        if (!tables.isEmpty()) {
            user.append(lang.vi() ? "BẢNG THAM CHIẾU (tra bảng, không dựa vào trí nhớ):\n"
                    : "REFERENCE TABLES (look them up, do not rely on memory):\n");
            for (String t : tables) {
                user.append(t.strip()).append("\n\n");
            }
        }
        user.append(lang.vi() ? "CÂU HỎI: " : "QUESTION: ").append(question.strip());
        return user.toString();
    }

    /** OpenAI-style messages: [system, user]. */
    public static List<Map<String, String>> messages(Rules rules, ContextText.Built ctx, String question, Lang lang) {
        return List.of(Map.of("role", "system", "content", rules.system),
                Map.of("role", "user", "content", userMessage(rules, ctx, question, lang)));
    }
}
