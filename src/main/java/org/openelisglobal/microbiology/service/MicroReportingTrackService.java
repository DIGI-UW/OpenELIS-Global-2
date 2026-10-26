package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroReportingTrackService {
    private final DictionaryService dictionary;

    public MicroReportingTrackService(DictionaryService dictionary) {
        this.dictionary = dictionary;
    }

    public List<IdValuePair> options() {
        return dictionary.getActiveSortedEntriesByCategoryName("Reporting track").stream()
                .map(d -> new IdValuePair(d.getId(), d.getDictEntry())).toList();
    }

    public String validate(String id) {
        if (id == null || id.isBlank())
            return null;
        if (options().stream().noneMatch(option -> option.getId().equals(id)))
            throw new IllegalArgumentException("MICROBIOLOGY_REPORTING_TRACK_INVALID");
        return id;
    }
}
