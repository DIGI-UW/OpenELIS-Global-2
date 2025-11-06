/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.unitofmeasure.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

public interface UnitOfMeasureDAO extends BaseDAO<UnitOfMeasure, String> {

    // public boolean insertData(UnitOfMeasure unitOfMeasure) throws
    // LIMSRuntimeException;

    // public void deleteData(List unitOfMeasures) throws LIMSRuntimeException;

    // public List getAllUnitOfMeasures() throws LIMSRuntimeException;

    // public List getPageOfUnitOfMeasures(int startingRecNo) throws
    // LIMSRuntimeException;

    // public void getData(UnitOfMeasure unitOfMeasure) throws LIMSRuntimeException;

    // public void updateData(UnitOfMeasure unitOfMeasure) throws
    // LIMSRuntimeException;
    //

    //

    // public UnitOfMeasure getUnitOfMeasureByName(UnitOfMeasure unitOfMeasure)
    // throws
    // LIMSRuntimeException;

    // public Integer getTotalUnitOfMeasureCount() throws LIMSRuntimeException;

    public UnitOfMeasure getUnitOfMeasureById(String uomId) throws LIMSRuntimeException;

    boolean duplicateUnitOfMeasureExists(UnitOfMeasure unitOfMeasure) throws LIMSRuntimeException;

    // public List<UnitOfMeasure> getAllActiveUnitOfMeasures();

}
