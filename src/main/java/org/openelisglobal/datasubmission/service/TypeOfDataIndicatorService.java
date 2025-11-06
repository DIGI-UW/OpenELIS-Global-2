package org.openelisglobal.datasubmission.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;

public interface TypeOfDataIndicatorService extends BaseObjectService<TypeOfDataIndicator, String> {
    void getData(TypeOfDataIndicator typeOfIndicator);

    TypeOfDataIndicator getTypeOfDataIndicator(String id);

    List<TypeOfDataIndicator> getAllTypeOfDataIndicator();
}
