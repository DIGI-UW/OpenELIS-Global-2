package org.openelisglobal.barcode.dao;

import org.openelisglobal.barcode.valueholder.BarcodeLabelInfo;
import org.openelisglobal.common.dao.BaseDAO;

public interface BarcodeLabelInfoDAO extends BaseDAO<BarcodeLabelInfo, String> {

    /**
     * Persists the object in the database. updates object with PK from insert
     *
     * @param barcodeLabelInfo The object to persist
     * @return If insertion was success
     * @throws LIMSRuntimeException
     */
    // public boolean insertData(BarcodeLabelInfo barcodeLabelInfo) throws
    // LIMSRuntimeException;

    /**
     * Persists changes on the object based on PK
     *
     * @param barcodeLabelInfo The object to update
     * @throws LIMSRuntimeException
     */
    // public void updateData(BarcodeLabelInfo barcodeLabelInfo) throws
    // LIMSRuntimeException;

    /**
     * Get object by code instead of PK
     *
     * @param code Code of the object
     * @return the corresponding object
     * @throws LIMSRuntimeException
     */
    // public BarcodeLabelInfo getDataByCode(String code) throws
    // LIMSRuntimeException;
}
