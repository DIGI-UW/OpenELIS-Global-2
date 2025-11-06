package org.openelisglobal.internationalization;

import java.util.Locale;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceResourceBundle;

public class MessageUtil {

    private static MessageUtil instance;
    private static String STRING_KEY_SUFFIX = null;

    MessageSource messageSource;

    private MessageUtil() {
    }

    public static void setMessageSource(MessageSource messageSource) {
        instance = new MessageUtil();
        instance.messageSource = messageSource;
    }

    /**
     * @param key - message key
     * @return - message for key in current locale
     */
    public static String getMessage(String key) {
        return getMessage(key, null, key, LocaleContextHolder.getLocale());
    }

    /**
     * @param key    - message key
     * @param locale - specific locale to check
     * @return - message for key in locale
     */
    public static String getMessage(String key, Locale locale) {
        return getMessage(key, null, key, locale);
    }

    /**
     * @param key  - message key
     * @param args - args to pass into message
     * @return - message for key in current locale
     */
    public static String getMessage(String key, Object[] args) {
        return getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    /**
     * @param key - message key
     * @param arg - single arg to pass in
     * @return - message for key in current locale
     */
    public static String getMessage(String key, String arg) {
        return getMessage(key, new String[] { arg }, key, LocaleContextHolder.getLocale());
    }

    public static String getMessageOrDefault(String key, Object[] args, String defaultMsg) {
        return getMessage(key, args, defaultMsg, LocaleContextHolder.getLocale());
    }

    public static String getMessage(String key, Object[] args, Locale locale) {
        return getMessage(key, args, key, locale);
    }

    /**
     * @param key    - message key
     * @param args   - args to pass into message
     * @param locale - specific locale to check
     * @return - message for key in locale
     */
    public static String getMessage(String key, Object[] args, String defaultMessage, Locale locale) {
        return instance.messageSource.getMessage(key, args, defaultMessage, locale);
    }

    /**
     * @param key - message key
     * @return - message for contextual key in current locale
     */
    public static String getContextualMessage(String messageKey) {
        return getMessage(getContextualKey(messageKey));
    }

    public static String getContextualKey(String key) {
        if (null == key) {
            return null;
        }

        // Note that if there is no suffix then the suffix key will be the same
        // as the message key
        // and the first search will be successful, there is no reason to test
        // for the suffix
        String contextualKey = key + getSuffix();
        String suffixedValue = getMessage(contextualKey);

        if (GenericValidator.isBlankOrNull(suffixedValue) || contextualKey.equals(suffixedValue)) {
            LogEvent.logTrace("MessageUtil", "getContextualKey()", "contextual key not found, using non-contextul key");
            return key;
        }
        return contextualKey;
    }

    private static String getSuffix() {
        if (STRING_KEY_SUFFIX == null) {
            STRING_KEY_SUFFIX = ConfigurationProperties.getInstance().getPropertyValue(Property.StringContext);
            if (!GenericValidator.isBlankOrNull(STRING_KEY_SUFFIX)) {
                STRING_KEY_SUFFIX = "." + STRING_KEY_SUFFIX.trim();
            }
        }

        return STRING_KEY_SUFFIX;
    }

    public static MessageSourceResourceBundle getMessageSourceAsResourceBundle() {
        return new MessageSourceResourceBundle(instance.messageSource, LocaleContextHolder.getLocale());
    }

    public static boolean messageNotFound(String message, String key) {
        return key.equals(message);
    }
}
