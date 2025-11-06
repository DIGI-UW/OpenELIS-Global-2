package org.openelisglobal.renamemethod.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.renamemethod.valueholder.RenameMethod;

public interface RenameMethodService
        extends BaseObjectService<org.openelisglobal.renamemethod.valueholder.RenameMethod, String> {
    List<RenameMethod> getMethods(String filter);

    List<RenameMethod> getAllInActiveMethods();

    void refreshNames();

    Localization getLocalizationForRenameMethod(String id);
}
