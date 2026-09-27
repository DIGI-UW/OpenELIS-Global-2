package org.openelisglobal.testconfiguration.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.testconfiguration.form.ResultSelectListForm;
import org.openelisglobal.testconfiguration.form.ResultSelectListRenameForm;

public interface ResultSelectListService {

    boolean addResultSelectList(ResultSelectListForm form, String currentUserId);

    List<Dictionary> getAllSelectListOptions();

    Map<String, List<IdValuePair>> getTestSelectDictionary();

    boolean renameOption(ResultSelectListRenameForm form, String currentUserId);

    /**
     * The stored translations of one option's displayed name, for the rename screen
     * to prefill. Counterpart to
     * {@link #renameOption(ResultSelectListRenameForm, String)}: the option list
     * carries only the name of the locale it is being read in, so without this the
     * screen cannot show what the other locales already say — and cannot send them
     * back unchanged.
     *
     * @param id the dictionary id of the option
     * @return its localization, or null if the option or its localization is absent
     */
    Localization getLocalizationForResultSelectOption(String id);
}
