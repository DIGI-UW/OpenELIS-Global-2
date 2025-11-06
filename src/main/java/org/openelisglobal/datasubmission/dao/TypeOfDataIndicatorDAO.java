package org.openelisglobal.datasubmission.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;

public interface TypeOfDataIndicatorDAO extends BaseDAO<TypeOfDataIndicator, String> {

    public void getData(TypeOfDataIndicator typeOfIndicator) throws LIMSRuntimeException;

    public TypeOfDataIndicator getTypeOfDataIndicator(String id) throws LIMSRuntimeException;

    public List<TypeOfDataIndicator> getAllTypeOfDataIndicator() throws LIMSRuntimeException;

    // public boolean insertData(TypeOfDataIndicator typeOfIndicator) throws
    // LIMSRuntimeException;

    // public void updateData(TypeOfDataIndicator typeOfIndicator) throws
    // LIMSRuntimeException;
}
