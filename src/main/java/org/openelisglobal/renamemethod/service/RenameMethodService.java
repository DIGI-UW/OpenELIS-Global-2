package org.openelisglobal.renamemethod.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.renamemethod.valueholder.RenameMethod;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RenameMethodService
        extends BaseObjectService<org.openelisglobal.renamemethod.valueholder.RenameMethod, String> {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<RenameMethod> getMethods(String filter);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<RenameMethod> getAllInActiveMethods();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void refreshNames();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Localization getLocalizationForRenameMethod(String id);
}
