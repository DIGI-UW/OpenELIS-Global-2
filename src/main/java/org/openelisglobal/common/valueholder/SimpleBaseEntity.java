package org.openelisglobal.common.valueholder;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * This interface provides the most rudimentary interface to entities (DB Rows)
 * in OpenElis. If an object implements this interface then we can use a generic
 * DAO implementation to read/write/update and properly track changes. The
 * changes are tracked as appropriate in the application, DB logs and the entity
 * itself.
 *
 * @author pahill
 * @param <T> the class of the index
 */
public interface SimpleBaseEntity<T> extends Serializable {

    T getId();

    void setId(T key);

    Timestamp getLastupdated();

    void setLastupdated(Timestamp lastupdated);

    void setSysUserId(String sysUserId);

    public String getSysUserId();
}
