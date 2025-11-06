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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.hibernate.resources.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Accesses property values via a get/set pair, which may be nonpublic. The
 * default (and recommended strategy).
 *
 * @author Diane Benz added for bugzilla 1908: Vietnam postgres does not take
 *         java String type and allow inserts/updates to SQL NUMBER (Oracle
 *         does) Have created this UserType to convert java String to Integer
 *         Updated all hbm.xml files that use java.lang.String on a NUMBER
 *         column to use this custom type
 */
public class LIMSStringNumberUserType implements UserType {

    private static final int[] SQL_TYPES = { Types.NUMERIC };

    public LIMSStringNumberUserType() {
        super();
    }

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public Class returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return (x == y) || (x != null && y != null && (x.equals(y)));
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        if (value == null) {
            return null;
        }
        return new String((String) value);
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object assemble(Serializable arg0, Object arg1) throws HibernateException {
        return deepCopy(arg0);
    }

    @Override
    public Serializable disassemble(Object value) {
        return (Serializable) deepCopy(value);
    }

    /*
     * (non-Javadoc) (at) see org (dot)
     * hibernate.usertype.UserType#hashCode(java.lang.Object)
     */
    @Override
    public int hashCode(Object arg0) throws HibernateException {
        return arg0.hashCode();
    }

    /*
     * (non-Javadoc) (at) see org (dot)
     * hibernate.usertype.UserType#replace(java.lang.Object, java.lang.Object,
     * java.lang.Object)
     */
    @Override
    public Object replace(Object arg0, Object arg1, Object arg2) throws HibernateException {
        return deepCopy(arg0);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        int value = rs.getInt(names[0]);
        return rs.wasNull() ? null : String.valueOf(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.NUMERIC);
        } else {
            if (value instanceof String) {
                if (value.equals("")) {
                    st.setNull(index, Types.NUMERIC);
                } else {
                    st.setInt(index, Integer.parseInt((String) value));
                }
            } else {
                throw new LIMSRuntimeException("Incorrect Mapping using this UserType LIMSStringNumberUserType");
            }
        }
    }
}
