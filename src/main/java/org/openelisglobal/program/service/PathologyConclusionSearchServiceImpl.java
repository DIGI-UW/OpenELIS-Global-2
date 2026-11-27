package org.openelisglobal.program.service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.program.util.DictionaryEntryNames;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntries;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PathologyConclusionSearchServiceImpl implements PathologyConclusionSearchService {

    /** How many matches are answered; the count says how many there are. */
    static final int PAGE_SIZE = 50;

    private final DictionaryService dictionaryService;

    public PathologyConclusionSearchServiceImpl(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @Override
    @Transactional(readOnly = true)
    public ConclusionEntries search(String query) {
        String wanted = fold(StringUtils.trimToEmpty(query));
        List<ConclusionEntry> matches = dictionaryService
                .getDictionaryEntrysByCategoryAbbreviation("categoryName", PathologySampleService.CONCLUSION_CATEGORY,
                        false)
                .stream().map(DictionaryEntryNames::conclusionEntryOf)
                .filter(entry -> fold(entry.value()).contains(wanted) || fold(entry.code()).contains(wanted))
                .sorted(Comparator.comparing((ConclusionEntry entry) -> entry.value().toLowerCase(Locale.ROOT))
                        .thenComparing(ConclusionEntry::id))
                .toList();
        return new ConclusionEntries(matches.stream().limit(PAGE_SIZE).toList(), matches.size());
    }

    /** Lower case and without accents; punctuation is kept. */
    private static String fold(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
