package org.openelisglobal.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

/**
 * Custom Hibernate UserType for mapping PostgreSQL JSONB columns to String
 * fields. This handles the type conversion between Java String and PostgreSQL
 * JSONB.
 */
public class JsonBinaryType implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.OTHER };
    }

    @Override
    public Class returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == null) {
            return y == null;
        }
        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        final String cellContent = rs.getString(names[0]);
        if (cellContent == null) {
            return null;
        }
        try {
            return cellContent;
        } catch (final Exception ex) {
            throw new HibernateException("Failed to convert String to JSONB: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            try {
                PGobject pgo = new PGobject();
                pgo.setType("jsonb");
                pgo.setValue(value.toString());
                st.setObject(index, pgo);
            } catch (final Exception ex) {
                throw new HibernateException("Failed to convert JSONB to String: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) this.deepCopy(value);
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return this.deepCopy(cached);
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return this.deepCopy(original);
    }
}
