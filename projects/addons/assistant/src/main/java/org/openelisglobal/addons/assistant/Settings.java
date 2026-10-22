package org.openelisglobal.addons.assistant;

import java.time.ZoneId;
import java.util.Map;

/**
 * The add-on's configuration, as set on the OpenELIS Extensions page (fields
 * declared in {@code addon.json}) and served to the service by
 * {@code GET /config}. Secrets ({@code apiKey}, {@code runnerToken}) come the
 * same way, from fields of type {@code secret}.
 */
public final class Settings {

    public static final String OPENAI = "openai-compatible";
    public static final String CLAUDE = "claude-runner";
    public static final String NONE = "none";
    public static final String DEFAULT_ENDPOINT = "https://openrouter.ai/api/v1";
    public static final String DEFAULT_MODEL = "deepseek/deepseek-chat";

    public final Lang lang;
    public final String labName;
    public final String productName;
    public final String provider;
    public final String fallbackProvider;
    public final String endpoint;
    public final String model;
    public final String apiKey;
    public final String runnerUrl;
    public final String runnerToken;
    public final int dailyQuota;
    public final ZoneId zone;
    public final int idleMinutes;
    public final int retentionDays;
    public final int historyTurns;
    public final int timeoutSeconds;
    public final int maxTokens;
    public final double temperature;

    public Settings(Lang lang, String labName, String productName, String provider, String fallbackProvider,
            String endpoint, String model, String apiKey, String runnerUrl, String runnerToken, int dailyQuota,
            ZoneId zone, int idleMinutes, int retentionDays, int historyTurns, int timeoutSeconds, int maxTokens,
            double temperature) {
        this.lang = lang;
        this.labName = labName;
        this.productName = productName;
        this.provider = provider;
        this.fallbackProvider = fallbackProvider;
        this.endpoint = stripSlash(endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim());
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.runnerUrl = runnerUrl == null ? "" : stripSlash(runnerUrl.trim());
        this.runnerToken = runnerToken == null ? "" : runnerToken.trim();
        this.dailyQuota = dailyQuota;
        this.zone = zone;
        this.idleMinutes = idleMinutes <= 0 ? 5 : idleMinutes;
        this.retentionDays = retentionDays <= 0 ? 90 : retentionDays;
        this.historyTurns = Math.max(0, historyTurns);
        this.timeoutSeconds = timeoutSeconds <= 0 ? 60 : timeoutSeconds;
        this.maxTokens = maxTokens <= 0 ? 900 : maxTokens;
        this.temperature = temperature;
    }

    /** Tests and defaults. */
    public static Settings defaults(Lang lang) {
        return from(Map.of("language", lang.code));
    }

    /** From the configuration map OpenELIS serves ({@code GET /config}). */
    public static Settings from(Map<String, ?> c) {
        return new Settings(Lang.of(str(c, "language", "en")), str(c, "labName", ""),
                str(c, "productName", "OpenELIS"), str(c, "provider", OPENAI), str(c, "fallbackProvider", NONE),
                str(c, "endpoint", DEFAULT_ENDPOINT), str(c, "model", DEFAULT_MODEL), str(c, "apiKey", ""),
                str(c, "runnerUrl", ""), str(c, "runnerToken", ""), num(c, "dailyQuestionLimit", 20),
                zone(str(c, "timeZone", "UTC")), num(c, "idleMinutes", 5), num(c, "retentionDays", 90),
                num(c, "historyTurns", 6), num(c, "timeoutSeconds", 60), num(c, "maxTokens", 900),
                dbl(c, "temperature", 0.2));
    }

    public boolean openAiConfigured() {
        return !apiKey.isEmpty();
    }

    public boolean runnerConfigured() {
        return !runnerToken.isEmpty() && !runnerUrl.isEmpty();
    }

    private static String str(Map<String, ?> c, String k, String fallback) {
        Object v = c == null ? null : c.get(k);
        return v == null || String.valueOf(v).isBlank() ? fallback : String.valueOf(v);
    }

    private static int num(Map<String, ?> c, String k, int fallback) {
        Object v = c == null ? null : c.get(k);
        try {
            return v instanceof Number n ? n.intValue() : v == null ? fallback : (int) Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double dbl(Map<String, ?> c, String k, double fallback) {
        Object v = c == null ? null : c.get(k);
        try {
            return v instanceof Number n ? n.doubleValue() : v == null ? fallback : Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static ZoneId zone(String s) {
        try {
            return ZoneId.of(s.trim());
        } catch (RuntimeException e) {
            return ZoneId.of("UTC");
        }
    }

    private static String stripSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
