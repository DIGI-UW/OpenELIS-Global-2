package org.openelisglobal.result.action.util;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;

/**
 * OGC-1234: the select-list options a results screen offers are the test's
 * active options. A result entered before its option was removed in the Test
 * Catalog editor (by hand, or by "Copy from test" replacing the configuration)
 * still holds that dictionary value; without it in the list the screen showed
 * the raw dictionary id, or nothing. The stored value is appended, by name, so
 * the result reads as entered and can be kept or changed.
 */
public final class StoredDictionaryResult {

    private StoredDictionaryResult() {
    }

    public static List<IdValuePair> withStoredValue(List<IdValuePair> options, Result result,
            DictionaryService dictionaryService) {
        if (options == null || result == null || dictionaryService == null) {
            return options;
        }
        String value = result.getValue();
        if (GenericValidator.isBlankOrNull(value) || !value.trim().matches("\\d+")
                || !TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            return options;
        }
        String id = value.trim();
        for (IdValuePair option : options) {
            if (id.equals(option.getId())) {
                return options;
            }
        }
        Dictionary dictionary = dictionaryService.getDataForId(id);
        if (dictionary == null) {
            return options;
        }
        String displayValue = dictionary.getLocalizedName();
        if (displayValue == null || "unknown".equals(displayValue)) {
            displayValue = GenericValidator.isBlankOrNull(dictionary.getLocalAbbreviation()) ? dictionary.getDictEntry()
                    : dictionary.getLocalAbbreviation();
        }
        options.add(new IdValuePair(id, displayValue));
        return options;
    }
}
