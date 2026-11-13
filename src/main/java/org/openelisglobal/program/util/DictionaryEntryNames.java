package org.openelisglobal.program.util;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;

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
}
