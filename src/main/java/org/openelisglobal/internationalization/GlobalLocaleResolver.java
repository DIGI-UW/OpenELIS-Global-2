package org.openelisglobal.internationalization;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ConfigurationListener;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.common.util.LocaleChangeListener;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.i18n.AbstractLocaleContextResolver;

public class GlobalLocaleResolver extends AbstractLocaleContextResolver implements ConfigurationListener {

    private Locale defaultLocale;
    private Locale currentLocale;
    private TimeZone timeZone;

    private List<LocaleChangeListener> localChangeListeners = new ArrayList<>();

    public GlobalLocaleResolver() {
        defaultLocale = Locale.US;
    }

    public void addLocalChangeListener(LocaleChangeListener listener) {
        localChangeListeners.add(listener);
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        if (currentLocale == null) {
            return defaultLocale;
        }
        return currentLocale;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (!locale.equals(currentLocale)) {
            currentLocale = locale;
        }
        currentLocale = locale;
    }

    @Override
    public void setDefaultLocale(Locale locale) {
        if (!defaultLocale.equals(locale)) {
            this.defaultLocale = locale;
            for (LocaleChangeListener listener : localChangeListeners) {
                listener.localeChanged(locale.toLanguageTag());
            }
        }
    }

    @Override
    public LocaleContext resolveLocaleContext(HttpServletRequest request) {
        return new TimeZoneAwareLocaleContext() {
            @Override
            public Locale getLocale() {
                if (currentLocale == null) {
                    currentLocale = determineDefaultLocale();
                }
                return currentLocale;
            }

            @Override
            @Nullable
            public TimeZone getTimeZone() {
                if (timeZone == null) {
                    timeZone = determineDefaultTimeZone(request);
                }
                return timeZone;
            }
        };
    }

    @Override
    public void setLocaleContext(HttpServletRequest request, HttpServletResponse response,
            LocaleContext localeContext) {
        Locale locale = null;
        if (localeContext != null) {
            locale = localeContext.getLocale();
        }
        currentLocale = locale;
    }

    public Locale determineDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Determine the default time zone for the given request, Called if no TimeZone
     * session attribute has been found.
     *
     * <p>
     * The default implementation returns the specified default time zone, if any,
     * or {@code null} otherwise.
     *
     * @param request the request to resolve the time zone for
     * @return the default time zone (or {@code null} if none defined)
     * @see #setDefaultTimeZone
     */
    @Nullable
    protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
        return getDefaultTimeZone();
    }

    @Override
    public void refreshConfiguration() {
        String localeTag = SpringContext.getBean(DefaultConfigurationProperties.class)
                .getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        System.out.println("LOCALE IS: " + localeTag);
        Locale locale = GenericValidator.isBlankOrNull(localeTag) ? Locale.US : Locale.forLanguageTag(localeTag);
        setDefaultLocale(locale);
        LocaleContextHolder.setDefaultLocale(locale);
    }
}
