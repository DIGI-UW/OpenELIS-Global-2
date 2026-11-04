package org.openelisglobal.addons.assistant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The answer rules and reference tables of one language: {@code rules/<lang>.md}
 * in the jar, or the same file in {@code ASSISTANT_RULES_DIR} when that is set
 * (read again every {@value #RELOAD_MS} ms, so a laboratory can change its rules
 * without a rebuild). Blocks: {@code <!-- assistant:system -->} (the system
 * prompt) and {@code <!-- assistant:table <key> -->} (reference tables).
 * {@code {labName}} and {@code {productName}} are filled from the configuration.
 */
public final class Rules {

    private static final long RELOAD_MS = 60_000;

    private static final Pattern SYSTEM = Pattern
            .compile("<!-- assistant:system -->\\s*(.*?)\\s*<!-- /assistant:system -->", Pattern.DOTALL);
    private static final Pattern TABLE = Pattern.compile(
            "<!-- assistant:table ([a-z0-9_-]+) -->\\s*(.*?)\\s*<!-- /assistant:table -->", Pattern.DOTALL);

    public final String system;
    public final Map<String, String> tables;

    public Rules(String system, Map<String, String> tables) {
        this.system = system;
        this.tables = tables;
    }

    /** Used when no rules file can be read: minimal, but still safe. */
    static String fallbackSystem(Lang lang) {
        return lang.vi()
                ? "Bạn là Trợ lý AI của phần mềm xét nghiệm. Trả lời ngắn, tiếng Việt, chỉ dựa vào ngữ cảnh; không chẩn"
                        + " đoán xác định, không kê đơn; dữ liệu đã ẩn danh; bạn chỉ đọc, không sửa dữ liệu. Kết thúc bằng: \""
                        + lang.disclaimer + "\""
                : "You are the AI assistant of a laboratory information system. Answer briefly, in English, only from the"
                        + " context; no definitive diagnosis, no prescription; the data are de-identified; you only read,"
                        + " you never change data. End with: \"" + lang.disclaimer + "\"";
    }

    public static Rules parse(String markdown, Lang lang) {
        String md = markdown == null ? "" : markdown;
        Matcher s = SYSTEM.matcher(md);
        String system = s.find() ? s.group(1).strip() : fallbackSystem(lang);
        Map<String, String> tables = new LinkedHashMap<>();
        Matcher t = TABLE.matcher(md);
        while (t.find()) {
            tables.put(t.group(1), t.group(2).strip());
        }
        return new Rules(system, tables);
    }

    /** Vietnamese (the original rule format). */
    public static Rules parse(String markdown) {
        return parse(markdown, Lang.VI);
    }

    public static Rules fromFile(Path file, Lang lang) throws IOException {
        return parse(Files.readString(file, StandardCharsets.UTF_8), lang);
    }

    /** The rules with {@code {labName}} and {@code {productName}} filled in. */
    public Rules filled(String labName, String productName) {
        String lab = labName == null || labName.isBlank() ? "the laboratory" : labName.strip();
        String product = productName == null || productName.isBlank() ? "OpenELIS" : productName.strip();
        Map<String, String> t = new LinkedHashMap<>();
        tables.forEach((k, v) -> t.put(k, v.replace("{labName}", lab).replace("{productName}", product)));
        return new Rules(system.replace("{labName}", lab).replace("{productName}", product), t);
    }

    // ------------------------------------------------------------------ cached loading

    private record Loaded(Rules rules, long at) {
    }

    private static final Map<Lang, Loaded> CACHE = new EnumMap<>(Lang.class);
    private static volatile Path dir = envDir();

    private static Path envDir() {
        String d = System.getenv("ASSISTANT_RULES_DIR");
        return d == null || d.isBlank() ? null : Path.of(d);
    }

    /** Tests: read rules from this directory instead of the jar. */
    public static synchronized void useDirectory(Path directory) {
        dir = directory;
        CACHE.clear();
    }

    public static synchronized Rules current(Lang lang) {
        long now = System.currentTimeMillis();
        Loaded l = CACHE.get(lang);
        if (l != null && now - l.at() < RELOAD_MS) {
            return l.rules();
        }
        Rules r = null;
        try {
            Path f = dir == null ? null : dir.resolve(lang.code + ".md");
            if (f != null && Files.isReadable(f)) {
                r = fromFile(f, lang);
            } else {
                try (InputStream in = Rules.class.getResourceAsStream("/rules/" + lang.code + ".md")) {
                    r = in == null ? null : parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), lang);
                }
            }
        } catch (IOException e) {
            r = null;
        }
        if (r == null) {
            r = l != null ? l.rules() : new Rules(fallbackSystem(lang), Map.of());
        }
        CACHE.put(lang, new Loaded(r, now));
        return r;
    }
}
