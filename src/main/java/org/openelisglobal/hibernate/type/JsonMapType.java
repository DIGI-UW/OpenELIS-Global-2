package org.openelisglobal.hibernate.type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

/**
 * Custom Hibernate UserType for mapping PostgreSQL JSONB columns to Map<String,
 * Object> fields. This handles the type conversion between Java Map and
 * PostgreSQL JSONB.
 */
public class JsonMapType implements UserType {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
    };

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.OTHER };
    }

    @Override
    public Class<?> returnedClass() {
        return Map.class;
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
        if (cellContent == null || cellContent.isEmpty()) {
            return new HashMap<String, Object>();
        }
        try {
            return OBJECT_MAPPER.readValue(cellContent, MAP_TYPE_REF);
        } catch (final JsonProcessingException ex) {
            throw new HibernateException("Failed to parse JSONB to Map: " + ex.getMessage(), ex);
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
                String jsonString = OBJECT_MAPPER.writeValueAsString(value);
                pgo.setValue(jsonString);
                st.setObject(index, pgo);
            } catch (final JsonProcessingException ex) {
                throw new HibernateException("Failed to convert Map to JSONB: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object deepCopy(Object value) throws HibernateException {
        if (value == null) {
            return null;
        }
        // Create a deep copy of the map
        Map<String, Object> original = (Map<String, Object>) value;
        return new HashMap<>(original);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new HibernateException("Failed to serialize Map: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        if (cached == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue((String) cached, MAP_TYPE_REF);
        } catch (JsonProcessingException ex) {
            throw new HibernateException("Failed to deserialize Map: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return this.deepCopy(original);
    }
}
