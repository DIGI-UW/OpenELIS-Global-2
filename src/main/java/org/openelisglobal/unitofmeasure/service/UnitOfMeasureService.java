package org.openelisglobal.unitofmeasure.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UnitOfMeasureService extends BaseObjectService<UnitOfMeasure, String> {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    UnitOfMeasure getUnitOfMeasureById(String uomId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    UnitOfMeasure getUnitOfMeasureByName(UnitOfMeasure unitOfMeasure);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<UnitOfMeasure> getUnitOfMeasuresByType(String uomType);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void refreshNames();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Localization getLocalizationForUnitOfMeasure(String id);
}
