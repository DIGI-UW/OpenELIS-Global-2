package org.openelisglobal.internationalization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * The interface's translation catalogue, for documents the server renders that
 * must print the same words as the screens (the patient report, FRS FR-A53).
 *
 * <p>
 * The catalogue is {@code frontend/src/languages/*.json}, packaged into the WAR
 * under {@value #CATALOGUE}, so every language the screens ship is available to
 * the report and the backend's English and French bundle is not involved. A
 * locale resolves exactly as it does in the browser: English, then the base
 * language, then the regional file, then, when the site has switched overriding
 * on, the deployment's override files for the base language and the locale from
 * {@link #OVERRIDE_DIRECTORY}, the directory the frontend serves as
 * {@code /translation}. A key no layer holds prints as the key, as on screen.
 *
 * <p>
 * Shipped files are read once; override files are read on every lookup of a
 * locale, so a reworded label prints on the next report without a restart.
 */
public final class UiMessages {

    static final String CATALOGUE = "ui-languages/";
    public static final Path OVERRIDE_DIRECTORY = Path.of("/var/lib/openelis-global/translation");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, Map<String, String>> SHIPPED = new ConcurrentHashMap<>();
    private static final long CACHE_MILLIS = 30_000;
    private static final Map<Locale, CachedCatalogue> CACHE = new ConcurrentHashMap<>();

    private final Locale locale;
    private final Map<String, String> messages;

    private UiMessages(Locale locale, Map<String, String> messages) {
        this.locale = locale;
        this.messages = messages;
    }

    /** The catalogue for the current user's language. */
    public static UiMessages current() {
        return forLocale(LocaleContextHolder.getLocale());
    }

    /**
     * The catalogue for a locale, reused for a few seconds: a pick list asks for
     * one label per value, and building the catalogue reads the override files.
     */
    public static UiMessages cached(Locale locale) {
        Locale resolved = locale == null ? Locale.ENGLISH : locale;
        long now = System.currentTimeMillis();
        CachedCatalogue cachedCatalogue = CACHE.get(resolved);
        if (cachedCatalogue == null || now - cachedCatalogue.builtAt > CACHE_MILLIS) {
            cachedCatalogue = new CachedCatalogue(forLocale(resolved), now);
            CACHE.put(resolved, cachedCatalogue);
        }
        return cachedCatalogue.messages;
    }

    /** Whether the shipped English catalogue holds the key. */
    public static boolean defines(String key) {
        return key != null && !key.isBlank() && shipped("en").containsKey(key.trim());
    }

    /** The shipped English catalogue's keys under a prefix, with their English. */
    public static Map<String, String> shippedEnglish(String prefix) {
        Map<String, String> matching = new HashMap<>();
        shipped("en").forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                matching.put(key, value);
            }
        });
        return matching;
    }

    /** Whether this catalogue holds the key in any layer. */
    public boolean has(String key) {
        return key != null && messages.containsKey(key);
    }

    private static final class CachedCatalogue {
        private final UiMessages messages;
        private final long builtAt;

        private CachedCatalogue(UiMessages messages, long builtAt) {
            this.messages = messages;
            this.builtAt = builtAt;
        }
    }

    public static UiMessages forLocale(Locale locale) {
        boolean overrides = "true".equalsIgnoreCase(
                ConfigurationProperties.getInstance().getPropertyValue(Property.OVERRIDE_DEFAULT_TRANSLATION));
        return forLocale(locale, overrides ? OVERRIDE_DIRECTORY : null);
    }

    /**
     * @param overrideDirectory the deployment's override files, or null when
     *                          overriding is off
     */
    public static UiMessages forLocale(Locale locale, Path overrideDirectory) {
        Locale resolved = locale == null ? Locale.ENGLISH : locale;
        Map<String, String> messages = new HashMap<>(shipped("en"));
        List<String> files = filesFor(resolved);
        for (String file : files) {
            if (!"en".equals(file)) {
                messages.putAll(shipped(file));
            }
        }
        if (overrideDirectory != null) {
            for (String file : files) {
                messages.putAll(read(overrideDirectory.resolve(file + ".json")));
            }
        }
        return new UiMessages(resolved, messages);
    }

    /**
     * The file names for a locale, least specific first: {@code fr}, {@code fr_MG}.
     */
    static List<String> filesFor(Locale locale) {
        String language = locale.getLanguage();
        if (language.isEmpty()) {
            return List.of("en");
        }
        String country = locale.getCountry();
        return country.isEmpty() ? List.of(language) : List.of(language, language + "_" + country);
    }

    public Locale getLocale() {
        return locale;
    }

    /** The label for a key in this language, or the key when no layer holds it. */
    public String get(String key) {
        String message = messages.get(key);
        return message == null ? key : message;
    }

    /**
     * The label with its numbered placeholders ({@code {0}}, {@code {1}}) filled.
     * An apostrophe is text, as it is in the screens' message syntax.
     */
    public String format(String key, Object... arguments) {
        String pattern = get(key);
        if (arguments == null || arguments.length == 0) {
            return pattern;
        }
        try {
            return new MessageFormat(pattern.replace("'", "''"), locale).format(arguments);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(UiMessages.class.getSimpleName(), "format",
                    "Message " + key + " has an unreadable pattern: " + e.getMessage());
            return pattern;
        }
    }

    private static Map<String, String> shipped(String file) {
        return SHIPPED.computeIfAbsent(file, UiMessages::readShipped);
    }

    private static Map<String, String> readShipped(String file) {
        try (InputStream in = UiMessages.class.getClassLoader().getResourceAsStream(CATALOGUE + file + ".json")) {
            if (in == null) {
                if ("en".equals(file)) {
                    LogEvent.logError(UiMessages.class.getSimpleName(), "readShipped", "The interface catalogue "
                            + CATALOGUE + "en.json is not on the classpath: reports print message keys");
                }
                return Collections.emptyMap();
            }
            return strings(MAPPER.readValue(in, new TypeReference<Map<String, Object>>() {
            }));
        } catch (IOException e) {
            LogEvent.logError(UiMessages.class.getSimpleName(), "readShipped",
                    "The interface catalogue " + file + ".json could not be read: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * A deployment's override file. A missing file is the usual case; a broken one
     * costs only itself, as in the browser.
     */
    private static Map<String, String> read(Path file) {
        if (!Files.isRegularFile(file)) {
            return Collections.emptyMap();
        }
        try (InputStream in = Files.newInputStream(file)) {
            return strings(MAPPER.readValue(in, new TypeReference<Map<String, Object>>() {
            }));
        } catch (IOException | RuntimeException e) {
            LogEvent.logError(UiMessages.class.getSimpleName(), "read", "Translation override " + file
                    + " is not a JSON object of message ids; ignoring it: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    private static Map<String, String> strings(Map<String, Object> parsed) {
        Map<String, String> messages = new HashMap<>();
        if (parsed != null) {
            parsed.forEach((key, value) -> {
                if (value instanceof String) {
                    messages.put(key, (String) value);
                }
            });
        }
        return messages;
    }
}
