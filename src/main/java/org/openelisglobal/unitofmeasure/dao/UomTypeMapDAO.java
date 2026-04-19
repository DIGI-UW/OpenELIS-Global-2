package org.openelisglobal.unitofmeasure.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.openelisglobal.unitofmeasure.valueholder.UomTypeMap;

public interface UomTypeMapDAO extends BaseDAO<UomTypeMap, Integer> {

    List<UnitOfMeasure> getUnitOfMeasuresByType(String uomType);

    List<UomTypeMap> getMappingsForUom(String uomId);

    List<String> getTypesForUom(String uomId);
}
