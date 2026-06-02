package org.openelisglobal.testconfiguration.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.testconfiguration.form.ResultSelectListForm;
import org.openelisglobal.testconfiguration.form.ResultSelectListRenameForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResultSelectListService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    boolean addResultSelectList(ResultSelectListForm form, String currentUserId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Dictionary> getAllSelectListOptions();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Map<String, List<IdValuePair>> getTestSelectDictionary();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    boolean renameOption(ResultSelectListRenameForm form, String currentUserId);
}
