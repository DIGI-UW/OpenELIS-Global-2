package org.openelisglobal.program.service;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.program.util.DiagnosisCodeStarts;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PathologyCriticalResultServiceImpl implements PathologyCriticalResultService {

    @Autowired
    private DictionaryService dictionaryService;

    @Override
    public CriticalResult of(PathologySample pathologySample) {
        List<String> ids = pathologySample.getConclusions().stream()
                .filter(e -> e.getType() == ConclusionType.DICTIONARY).map(PathologyConclusion::getValue)
                .filter(StringUtils::isNotBlank).filter(this::isCriticalEntry).toList();
        boolean flagged = pathologySample.isCriticalResultFlagged();
        return new CriticalResult(flagged || !ids.isEmpty(), flagged, ids);
    }

    private boolean isCriticalEntry(String dictionaryId) {
        Dictionary entry = dictionaryService.getDataForId(dictionaryId);
        return entry != null && DiagnosisCodeStarts.isCritical(StringUtils.trimToEmpty(entry.getLocalAbbreviation()));
    }
}
