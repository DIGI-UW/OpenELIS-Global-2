package org.openelisglobal.qaevent.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Links the NCE categories and types an instance already holds to the display
 * keys OpenELIS ships them under (OGC-1507 FR-9, section 8). A row whose key
 * the interface catalogue does not hold is linked by its English name, then by
 * the alias list of older spellings; a row that matches neither keeps its key
 * and stays the site's own. Nothing is deleted or merged, so every NCE keeps
 * its category and type. Runs at every start; once linked, a row is left alone.
 */
@Service
public class NceDisplayKeyLinker {

    /** Older spellings of shipped values, lower case, to their key. */
    static final Map<String, String> ALIASES = Map.of("haemolytic sample", "nce.type.haemolyticSample",
            "lost specimen - irreplacable", "nce.type.lostSpecimenIrreplacable", "rerun - qc failure",
            "nce.type.rerunQCFailure", "post analytical", "nce.category.postanalytical", "technology issue",
            "nce.type.technologyIssue");

    @Autowired
    private NceCategoryService nceCategoryService;
    @Autowired
    private NceTypeService nceTypeService;

    /** What one list's linking did, for the upgrade report. */
    public static final class Report {
        public int alreadyLinked;
        public int linkedByName;
        public int linkedByAlias;
        public final List<String> unmatched = new ArrayList<>();

        boolean changed() {
            return linkedByName + linkedByAlias > 0;
        }

        @Override
        public String toString() {
            return linkedByName + " linked by name, " + linkedByAlias + " linked by alias, " + alreadyLinked
                    + " already linked, " + unmatched.size() + " unmatched"
                    + (unmatched.isEmpty() ? "" : " " + unmatched);
        }
    }

    @Transactional
    public Map<String, Report> link() {
        Map<String, Report> reports = new HashMap<>();
        reports.put("NCE categories", link(nceCategoryService.getAllNceCategories(), "nce.category.",
                NceCategory::getName, NceCategory::getDisplayKey, (category, key) -> {
                    category.setDisplayKey(key);
                    nceCategoryService.update(category);
                }));
        reports.put("NCE types", link(nceTypeService.getAllNceTypes(), "nce.type.", NceType::getName,
                NceType::getDisplayKey, (type, key) -> {
                    type.setDisplayKey(key);
                    nceTypeService.update(type);
                }));
        reports.forEach((list, report) -> {
            if (report.changed()) {
                LogEvent.logInfo(getClass().getSimpleName(), "link", "Shipped display keys, " + list + ": " + report);
            }
        });
        return reports;
    }

    interface KeySetter<T> {
        void set(T row, String key);
    }

    private <T> Report link(List<T> rows, String prefix, Function<T, String> name, Function<T, String> displayKey,
            KeySetter<T> setKey) {
        Report report = new Report();
        Map<String, List<String>> keysByEnglish = new HashMap<>();
        UiMessages.shippedEnglish(prefix).forEach((key, english) -> keysByEnglish
                .computeIfAbsent(english.trim().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(key));
        Set<String> taken = new HashSet<>();
        for (T row : rows) {
            if (UiMessages.defines(displayKey.apply(row))) {
                taken.add(displayKey.apply(row).trim());
            }
        }
        for (T row : rows) {
            if (UiMessages.defines(displayKey.apply(row))) {
                report.alreadyLinked++;
                continue;
            }
            String lowerName = name.apply(row) == null ? "" : name.apply(row).trim().toLowerCase(Locale.ROOT);
            List<String> byName = keysByEnglish.getOrDefault(lowerName, List.of());
            String alias = ALIASES.get(lowerName);
            Consumer<String> apply = key -> {
                setKey.set(row, key);
                taken.add(key);
            };
            if (byName.size() == 1 && !taken.contains(byName.get(0))) {
                apply.accept(byName.get(0));
                report.linkedByName++;
            } else if (alias != null && alias.startsWith(prefix) && UiMessages.defines(alias)
                    && !taken.contains(alias)) {
                apply.accept(alias);
                report.linkedByAlias++;
            } else {
                report.unmatched.add(name.apply(row));
            }
        }
        return report;
    }
}
