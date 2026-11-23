package org.openelisglobal.internationalization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;
import org.junit.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * Spring only runs a message through MessageFormat when it is given arguments,
 * so a doubled apostrophe in a message without placeholders reaches the user as
 * two apostrophes. Only messages with placeholders should double them.
 */
public class MessageApostropheTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d");

    @Test
    public void messagesWithoutPlaceholdersUseASingleApostrophe() throws Exception {
        List<String> doubled = new ArrayList<>();
        for (String language : new String[] { "en", "fr" }) {
            Properties messages = load("languages/message_" + language + ".properties");
            for (String key : messages.stringPropertyNames()) {
                String value = messages.getProperty(key);
                if (value.contains("''") && !PLACEHOLDER.matcher(value).find()) {
                    doubled.add(language + ": " + key);
                }
            }
        }
        assertTrue("doubled apostrophes in messages without placeholders: " + doubled, doubled.isEmpty());
    }

    @Test
    public void frenchMessagesReadWithOneApostropheWithOrWithoutArguments() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:/languages/message");
        source.setDefaultEncoding("UTF-8");

        assertEquals("Le destinataire n'a pas d'adresse pour ce canal",
                source.getMessage("delivery.error.noAddress", null, Locale.FRENCH));
        assertTrue(source.getMessage("run.refusal.waiting", new Object[] { "S1" }, Locale.FRENCH)
                .startsWith("Une valeur d'analyseur pour ce test attend dans S1."));
    }

    private static Properties load(String resource) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = MessageApostropheTest.class.getClassLoader().getResourceAsStream(resource)) {
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
