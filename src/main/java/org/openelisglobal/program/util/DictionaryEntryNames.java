package org.openelisglobal.program.util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntry;

/**
 * How a pathology row's dictionary entry, the tissue a cassette holds or the
 * stain on a slide, is named wherever it is shown: the case history, the case
 * view and the slide label all read the same name. Each caller decides what
 * stands in when the id names no entry.
 */
public final class DictionaryEntryNames {

    private DictionaryEntryNames() {
    }

    /**
     * The entry's name in the user's language, retired entries included; empty for
     * a blank id or an id that names no entry.
     */
    public static Optional<String> localizedNameOf(DictionaryService dictionaryService, String dictionaryId) {
        if (StringUtils.isBlank(dictionaryId)) {
            return Optional.empty();
        }
        Dictionary entry = dictionaryService.getDataForId(dictionaryId);
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.getLocalizedName());
    }

    /**
     * Each id with its name in the user's language, in the order given; an id that
     * names no entry is left out.
     */
    public static List<IdValuePair> namedPairs(DictionaryService dictionaryService, Stream<String> dictionaryIds) {
        return dictionaryIds
                .flatMap(id -> localizedNameOf(dictionaryService, id).map(name -> new IdValuePair(id, name)).stream())
                .collect(Collectors.toList());
    }

    /**
     * The conclusion entry the id names, read once: its name in the user's language
     * and its code; empty for a blank id, an id that names no entry, or an entry
     * without a name.
     */
    public static Optional<ConclusionEntry> conclusionEntryOf(DictionaryService dictionaryService,
            String dictionaryId) {
        if (StringUtils.isBlank(dictionaryId)) {
            return Optional.empty();
        }
        Dictionary entry = dictionaryService.getDataForId(dictionaryId);
        return entry == null || entry.getLocalizedName() == null ? Optional.empty()
                : Optional.of(conclusionEntryOf(entry));
    }

    /**
     * The loaded entry as a conclusion entry; a missing name or code reads blank.
     */
    public static ConclusionEntry conclusionEntryOf(Dictionary entry) {
        return new ConclusionEntry(entry.getId(), StringUtils.defaultString(entry.getLocalizedName()),
                StringUtils.trimToEmpty(entry.getLocalAbbreviation()));
    }
}
