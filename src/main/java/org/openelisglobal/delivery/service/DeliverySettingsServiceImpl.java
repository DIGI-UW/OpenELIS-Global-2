package org.openelisglobal.delivery.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliverySettingsServiceImpl implements DeliverySettingsService {

    static final String RETRY_DELAYS = "delivery.retryDelays";
    static final String DEFAULT_FORMAT = "delivery.defaultFormat";
    static final String HOLD_MINUTES = "delivery.holdMinutes";
    static final String SEND_PARTIALS = "delivery.sendPartials";
    static final String LAB_CONTACT = "delivery.labContact";
    static final String LOCATION = "delivery.location";
    static final String CENTRAL_ADDRESS = "delivery.centralAddress";
    static final String SECURE_LINKS = "delivery.secureLinks";
    static final String LINK_DAYS = "delivery.linkDays";
    static final String COUNTRY_CODE = "delivery.countryCode";

    private static final Set<String> FORMATS = Set.of("MASKED", "PROTECTED");
    private static final Set<String> LOCATIONS = Set.of("LOCAL", "CENTRAL");

    @Autowired
    private SiteInformationService siteInformationService;

    @Override
    @Transactional(readOnly = true)
    public DeliverySettings get() {
        DeliverySettings settings = new DeliverySettings();
        List<Integer> delays = parseDelays(value(RETRY_DELAYS));
        settings.setRetryDelays(delays.isEmpty() ? new ArrayList<>(DeliverySettings.DEFAULT_RETRY_DELAYS) : delays);
        String format = value(DEFAULT_FORMAT);
        if (format != null && FORMATS.contains(format)) {
            settings.setDefaultFormat(format);
        }
        settings.setHoldMinutes(parseInt(value(HOLD_MINUTES), settings.getHoldMinutes()));
        settings.setSendPartials(!"false".equalsIgnoreCase(value(SEND_PARTIALS)));
        settings.setLabContact(value(LAB_CONTACT));
        String location = value(LOCATION);
        if (location != null && LOCATIONS.contains(location)) {
            settings.setLocation(location);
        }
        settings.setCentralAddress(value(CENTRAL_ADDRESS));
        settings.setSecureLinks("true".equalsIgnoreCase(value(SECURE_LINKS)));
        settings.setLinkDays(parseInt(value(LINK_DAYS), settings.getLinkDays()));
        settings.setCountryCode(value(COUNTRY_CODE));
        return settings;
    }

    @Override
    @Transactional
    public DeliverySettings save(DeliverySettings wanted, String sysUserId) {
        if (wanted.getRetryDelays().isEmpty() || wanted.getRetryDelays().stream().anyMatch(d -> d == null || d < 1)) {
            throw new IllegalArgumentException("retryDelays");
        }
        if (wanted.getDefaultFormat() == null || !FORMATS.contains(wanted.getDefaultFormat())) {
            throw new IllegalArgumentException("defaultFormat");
        }
        if (wanted.getHoldMinutes() < 0 || wanted.getHoldMinutes() > 1440) {
            throw new IllegalArgumentException("holdMinutes");
        }
        if (wanted.getLocation() == null || !LOCATIONS.contains(wanted.getLocation())) {
            throw new IllegalArgumentException("location");
        }
        if ("CENTRAL".equals(wanted.getLocation()) && GenericValidator.isBlankOrNull(wanted.getCentralAddress())) {
            throw new IllegalArgumentException("centralAddress");
        }
        if (wanted.getLinkDays() < 1 || wanted.getLinkDays() > 365) {
            throw new IllegalArgumentException("linkDays");
        }
        if (!GenericValidator.isBlankOrNull(wanted.getCountryCode())
                && !wanted.getCountryCode().trim().matches("\\+?[0-9]{1,4}")) {
            throw new IllegalArgumentException("countryCode");
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put(RETRY_DELAYS, join(wanted.getRetryDelays()));
        values.put(DEFAULT_FORMAT, wanted.getDefaultFormat());
        values.put(HOLD_MINUTES, String.valueOf(wanted.getHoldMinutes()));
        values.put(SEND_PARTIALS, String.valueOf(wanted.isSendPartials()));
        values.put(LAB_CONTACT, blankToNull(wanted.getLabContact()));
        values.put(LOCATION, wanted.getLocation());
        values.put(CENTRAL_ADDRESS, blankToNull(wanted.getCentralAddress()));
        values.put(SECURE_LINKS, String.valueOf(wanted.isSecureLinks()));
        values.put(LINK_DAYS, String.valueOf(wanted.getLinkDays()));
        values.put(COUNTRY_CODE, blankToNull(wanted.getCountryCode()));
        for (Map.Entry<String, String> entry : values.entrySet()) {
            SiteInformation row = siteInformationService.getSiteInformationByName(entry.getKey());
            if (row == null) {
                continue;
            }
            row.setValue(entry.getValue());
            row.setSysUserId(sysUserId);
            siteInformationService.persistData(row, false);
        }
        return get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> retryDelays() {
        List<Integer> delays = parseDelays(value(RETRY_DELAYS));
        return delays.isEmpty() ? DeliverySettings.DEFAULT_RETRY_DELAYS : delays;
    }

    private String value(String name) {
        SiteInformation row = siteInformationService.getSiteInformationByName(name);
        return row == null || GenericValidator.isBlankOrNull(row.getValue()) ? null : row.getValue().trim();
    }

    static List<Integer> parseDelays(String raw) {
        List<Integer> delays = new ArrayList<>();
        if (GenericValidator.isBlankOrNull(raw)) {
            return delays;
        }
        for (String part : raw.split(",")) {
            try {
                int minutes = Integer.parseInt(part.trim());
                if (minutes > 0) {
                    delays.add(minutes);
                }
            } catch (NumberFormatException e) {
                // a malformed entry is skipped; the defaults cover an empty list
            }
        }
        return delays;
    }

    private static String join(List<Integer> delays) {
        StringBuilder joined = new StringBuilder();
        for (Integer delay : delays) {
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(delay);
        }
        return joined.toString();
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return raw == null ? fallback : Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String blankToNull(String value) {
        return GenericValidator.isBlankOrNull(value) ? null : value.trim();
    }
}
