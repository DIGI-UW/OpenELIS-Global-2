package org.openelisglobal.unitofmeasure.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.security.access.prepost.PreAuthorize;

@CrudPrivileges(write = "PRIV_TEST_CONFIGURE")
public interface UnitOfMeasureService extends BaseObjectService<UnitOfMeasure, String> {

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    UnitOfMeasure getUnitOfMeasureById(String uomId);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    UnitOfMeasure getUnitOfMeasureByName(UnitOfMeasure unitOfMeasure);

    // Units offered on the collection step are catalogue data, not results.
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<UnitOfMeasure> getUnitOfMeasuresByType(String uomType);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void refreshNames();

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Localization getLocalizationForUnitOfMeasure(String id);
}
