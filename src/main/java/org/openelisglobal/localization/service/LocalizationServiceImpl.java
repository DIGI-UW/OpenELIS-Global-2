package org.openelisglobal.localization.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.localization.dao.LocalizationDAO;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext", "localeResolver" })
public class LocalizationServiceImpl extends AuditableBaseObjectServiceImpl<Localization, String>
        implements LocalizationService {

    public enum LocalizationType {
        TEST_NAME("test name"), REPORTING_TEST_NAME("test report name"), BANNER_LABEL("Site information banner test"),
        TEST_UNIT_NAME("test unit name"), PANEL_NAME("panel name"), BILL_REF_LABEL("Billing reference_label");

        String dbLabel;

        LocalizationType(String dbLabel) {
            this.dbLabel = dbLabel;
        }

        @Transactional(readOnly = true)
        public String getDBDescription() {
            return dbLabel;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Localization get(String id) {
        return getBaseObjectDAO().get(id).get();
    }

    @Autowired
    private LocalizationDAO baseObjectDAO;

    LocalizationServiceImpl() {
        super(Localization.class);
    }

    @Override
    protected LocalizationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public String getCurrentLocaleLanguage() {
        return LocaleContextHolder.getLocale().getLanguage();
    }

    @Override
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    @Override
    public boolean languageChanged(Localization localization, Localization newLocalization) {
        if (localization == null || newLocalization.getLocalesWithValue().size() == 0) {
            return false;
        }
        boolean languageChanged = false;
        for (Locale locale : newLocalization.getLocalesWithValue()) {
            if (!newLocalization.getLocalizedValue(locale).equals(localization.getLocalizedValue(locale))) {
                languageChanged = true;
            }
        }
        return languageChanged;
    }

    @Override
    public String getLocalizedValueById(String id) {
        return baseObjectDAO.get(id).get().getLocalizedValue();
    }

    /**
     * Checks to see if localization is needed, if so it does the update.
     *
     * @param french  The french localization
     * @param english The english localization
     * @return true if the object can be found and an update is needed. False if the
     *         id the service was created with is not valid or the french or english
     *         is empty or null or the french and english match what is already in
     *         the object
     */
    // public boolean updateLocalizationIfNeeded(String english, String french) {
    // if (localization == null || GenericValidator.isBlankOrNull(french) ||
    // GenericValidator.isBlankOrNull(english)) {
    // return false;
    // }
    //
    // if (localization == null) {
    // return false;
    // }
    //
    // if (english.equals(localization.getEnglish()) &&
    // french.equals(localization.getFrench())) {
    // return false;
    // }
    //
    // localization.setEnglish(english);
    // localization.setFrench(french);
    // return true;
    // }

    // @Transactional(readOnly = true)
    // public Localization getLocalization() {
    // return localization;
    // }

    // public void setCurrentUserId(String currentUserId) {
    // if (localization != null) {
    // localization.setSysUserId(currentUserId);
    // }
    // }

    public static Localization createNewLocalization(String english, String french, LocalizationType type) {
        Localization localization = new Localization();
        localization.setDescription(type.getDBDescription());
        localization.setEnglish(english);
        localization.setFrench(french);
        return localization;
    }

    @Override
    public String insert(Localization localization) {
        return super.insert(localization);
    }

    @Override
    @Transactional
    public void updateTestNames(Localization name, Localization reportingName) {
        update(name);
        update(reportingName);
    }

    @Override
    public List<Locale> getAllActiveLocales() {
        return Arrays.asList(Locale.ENGLISH, Locale.FRENCH);
    }
}
