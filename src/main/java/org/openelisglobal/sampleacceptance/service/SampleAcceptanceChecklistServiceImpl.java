package org.openelisglobal.sampleacceptance.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl.LocalizationType;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleAcceptanceChecklistServiceImpl implements SampleAcceptanceChecklistService {

    /** The only enforcement modes the gate recognizes. */
    private static final Set<String> VALID_ENFORCEMENT = Set.of("MANDATORY", "OPTIONAL", "OFF");

    /** The three domains that carry a configurable enforcement mode. */
    private static final Set<String> ENFORCEABLE_DOMAINS = Set.of("CLINICAL", "ENVIRONMENTAL", "VECTOR");

    /** Valid navigation targets for the admin view / item editing. */
    private static final Set<String> VALID_VIEW_DOMAINS = Set.of(ALL_DOMAINS, "CLINICAL", "ENVIRONMENTAL", "VECTOR");

    /**
     * Maximum label length. Bounded by {@code dictionary.local_abbrev}
     * ({@code varchar(60)}), where the label is stored alongside the localized
     * name; we reject longer labels rather than silently truncating.
     */
    private static final int MAX_LABEL_LENGTH = 60;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> listLabWide() {
        return dictionaryService.getActiveSortedEntriesByCategoryName(LAB_WIDE_CATEGORY);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> listForDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return listLabWide();
        }
        String category = DOMAIN_CATEGORY_PREFIX + domain.trim().toUpperCase();
        List<Dictionary> domainItems = dictionaryService.getActiveSortedEntriesByCategoryName(category);
        // Precedence: a domain with its own active items uses that list; otherwise the
        // lab-wide list is the fallback. The lists are never merged.
        if (domainItems != null && !domainItems.isEmpty()) {
            return domainItems;
        }
        return listLabWide();
    }

    @Override
    public String getEnforcement(String domain) {
        Property property = enforcementPropertyFor(domain);
        if (property == null) {
            return DEFAULT_ENFORCEMENT;
        }
        String value = ConfigurationProperties.getInstance().getPropertyValue(property);
        return (value == null || value.isBlank()) ? DEFAULT_ENFORCEMENT : value;
    }

    private Property enforcementPropertyFor(String domain) {
        if (domain == null) {
            return null;
        }
        switch (domain.trim().toUpperCase()) {
        case "CLINICAL":
            return Property.SAMPLE_ACCEPTANCE_CHECKLIST_ENFORCEMENT_CLINICAL;
        case "ENVIRONMENTAL":
            return Property.SAMPLE_ACCEPTANCE_CHECKLIST_ENFORCEMENT_ENVIRONMENTAL;
        case "VECTOR":
            return Property.SAMPLE_ACCEPTANCE_CHECKLIST_ENFORCEMENT_VECTOR;
        default:
            return null;
        }
    }

    // ---- admin (write) side -------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AdminChecklistView getAdminView(String domain) {
        String normalized = normalizeDomain(domain);
        validateViewDomain(normalized);
        AdminChecklistView view = new AdminChecklistView();
        view.setDomain(normalized);

        List<Dictionary> ownItems = sortedAllByCategoryName(categoryNameFor(normalized));
        view.setOwnItems(ownItems);

        if (ALL_DOMAINS.equals(normalized)) {
            view.setLabWideItems(new ArrayList<>());
            view.setDomainOverrides(false);
            view.setEnforcement(null);
        } else {
            view.setLabWideItems(listLabWide());
            view.setDomainOverrides(hasActive(ownItems));
            view.setEnforcement(getEnforcement(normalized));
        }
        return view;
    }

    @Override
    @Transactional
    public Dictionary createItem(String domain, String label, String sysUserId) {
        String normalized = normalizeDomain(domain);
        validateViewDomain(normalized);
        String trimmed = validateLabel(label);
        DictionaryCategory category = requireCategory(categoryNameFor(normalized));
        List<Dictionary> existing = dictionaryService.getDictionaryEntriesByCategoryId(category.getId());

        Localization localization = new Localization();
        localization.setDescription(LocalizationType.DICTIONARY_NAME.getDBDescription());
        localization.setLocalizedValue("en", trimmed);
        localization.setSysUserId(sysUserId);
        localizationService.insert(localization);

        Dictionary dictionary = new Dictionary();
        dictionary.setDictionaryCategory(category);
        dictionary.setDictEntry(generateUniqueDictEntry(trimmed, existing));
        dictionary.setLocalAbbreviation(trimmed);
        dictionary.setIsActive(IActionConstants.YES);
        dictionary.setSortOrder(nextSortOrder(existing));
        dictionary.setLocalizedDictionaryName(localization);
        dictionary.setSysUserId(sysUserId);
        dictionaryService.insert(dictionary);
        return dictionary;
    }

    @Override
    @Transactional
    public Dictionary updateItem(String id, String label, boolean active, String sysUserId) {
        String trimmed = validateLabel(label);
        Dictionary dictionary = dictionaryService.getDictionaryById(id);
        if (dictionary == null) {
            throw new IllegalArgumentException("No checklist item with id " + id);
        }
        dictionary.setLocalAbbreviation(trimmed);
        dictionary.setIsActive(active ? IActionConstants.YES : IActionConstants.NO);

        // Keep the localized display name (what the gate renders) in step with the
        // edited label; dictEntry stays the stable key.
        Localization localization = dictionary.getLocalizedDictionaryName();
        if (localization == null) {
            localization = new Localization();
            localization.setDescription(LocalizationType.DICTIONARY_NAME.getDBDescription());
            localization.setLocalizedValue("en", trimmed);
            localization.setSysUserId(sysUserId);
            localizationService.insert(localization);
            dictionary.setLocalizedDictionaryName(localization);
        } else {
            localization.setLocalizedValue("en", trimmed);
            localization.setSysUserId(sysUserId);
            localizationService.update(localization);
        }

        dictionary.setSysUserId(sysUserId);
        dictionaryService.update(dictionary);
        return dictionary;
    }

    @Override
    @Transactional
    public void reorder(String domain, List<String> orderedIds, String sysUserId) {
        validateViewDomain(normalizeDomain(domain));
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        int order = 1;
        for (String id : orderedIds) {
            Dictionary dictionary = dictionaryService.getDictionaryById(id);
            if (dictionary == null) {
                throw new IllegalArgumentException("No checklist item with id " + id);
            }
            dictionary.setSortOrder(order++);
            dictionary.setSysUserId(sysUserId);
            dictionaryService.update(dictionary);
        }
    }

    @Override
    @Transactional
    public void setEnforcement(String domain, String mode, String sysUserId) {
        String normalizedDomain = domain == null ? "" : domain.trim().toUpperCase();
        if (!ENFORCEABLE_DOMAINS.contains(normalizedDomain)) {
            throw new IllegalArgumentException("Enforcement is not configurable for domain: " + domain);
        }
        String normalizedMode = mode == null ? "" : mode.trim().toUpperCase();
        if (!VALID_ENFORCEMENT.contains(normalizedMode)) {
            throw new IllegalArgumentException("Invalid enforcement mode: " + mode);
        }
        String name = "sampleAcceptCheck." + normalizedDomain.toLowerCase();
        SiteInformation siteInformation = siteInformationService.getSiteInformationByName(name);
        if (siteInformation == null) {
            throw new IllegalArgumentException("Enforcement setting not found: " + name);
        }
        siteInformation.setValue(normalizedMode);
        siteInformation.setSysUserId(sysUserId);
        siteInformationService.persistData(siteInformation, false);
        // getEnforcement() reads the in-memory ConfigurationProperties map, which is
        // loaded at startup and not refreshed by persistData for these keys. Reload
        // it (as the generic site-information admin does) so the new mode is live.
        ConfigurationProperties.loadDBValuesIntoConfiguration();
    }

    // ---- helpers ------------------------------------------------------------

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank() || ALL_DOMAINS.equalsIgnoreCase(domain.trim())) {
            return ALL_DOMAINS;
        }
        return domain.trim().toUpperCase();
    }

    private String categoryNameFor(String normalizedDomain) {
        return ALL_DOMAINS.equals(normalizedDomain) ? LAB_WIDE_CATEGORY : DOMAIN_CATEGORY_PREFIX + normalizedDomain;
    }

    private DictionaryCategory requireCategory(String categoryName) {
        DictionaryCategory category = dictionaryCategoryService.getDictionaryCategoryByName(categoryName);
        if (category == null || category.getId() == null) {
            throw new IllegalStateException("Missing dictionary category: " + categoryName);
        }
        return category;
    }

    private List<Dictionary> sortedAllByCategoryName(String categoryName) {
        DictionaryCategory category = dictionaryCategoryService.getDictionaryCategoryByName(categoryName);
        if (category == null || category.getId() == null) {
            return new ArrayList<>();
        }
        List<Dictionary> entries = dictionaryService.getDictionaryEntriesByCategoryId(category.getId());
        if (entries == null) {
            return new ArrayList<>();
        }
        entries.sort(Comparator
                .comparingInt((Dictionary d) -> d.getSortOrder() != null ? d.getSortOrder() : Integer.MAX_VALUE)
                .thenComparing(Dictionary::getId));
        return entries;
    }

    private boolean hasActive(List<Dictionary> items) {
        for (Dictionary item : items) {
            if (IActionConstants.YES.equals(item.getIsActive())) {
                return true;
            }
        }
        return false;
    }

    private int nextSortOrder(List<Dictionary> existing) {
        int max = 0;
        for (Dictionary item : existing) {
            if (item.getSortOrder() != null && item.getSortOrder() > max) {
                max = item.getSortOrder();
            }
        }
        return max + 1;
    }

    /**
     * Build a stable {@code dictEntry} key from the label (camelCase, alphanumeric)
     * and disambiguate against the category's existing keys with a numeric suffix.
     */
    private String generateUniqueDictEntry(String label, List<Dictionary> existing) {
        String base = slugify(label);
        if (base.isEmpty()) {
            base = "item";
        }
        Set<String> taken = new HashSet<>();
        for (Dictionary item : existing) {
            if (item.getDictEntry() != null) {
                taken.add(item.getDictEntry().toLowerCase());
            }
        }
        String candidate = base;
        int suffix = 2;
        while (taken.contains(candidate.toLowerCase())) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    /** lowerCamelCase, alphanumeric-only key from the label (matches the seeds). */
    private String slugify(String label) {
        StringBuilder out = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < label.length() && out.length() < 100; i++) {
            char c = label.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            } else {
                upperNext = out.length() > 0;
            }
        }
        if (out.length() > 0) {
            out.setCharAt(0, Character.toLowerCase(out.charAt(0)));
        }
        return out.toString();
    }

    /**
     * A navigation target must be one of ALL / CLINICAL / ENVIRONMENTAL / VECTOR.
     */
    private void validateViewDomain(String normalizedDomain) {
        if (!VALID_VIEW_DOMAINS.contains(normalizedDomain)) {
            throw new IllegalArgumentException("Unknown domain: " + normalizedDomain);
        }
    }

    /**
     * A label is required and must fit the backing column. Returns the trimmed
     * label; throws {@link IllegalArgumentException} (→ HTTP 400) otherwise.
     */
    private String validateLabel(String label) {
        String trimmed = label == null ? "" : label.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("A label is required");
        }
        if (trimmed.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException("Label must be " + MAX_LABEL_LENGTH + " characters or fewer");
        }
        return trimmed;
    }
}
