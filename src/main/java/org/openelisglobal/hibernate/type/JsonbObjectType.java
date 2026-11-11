package org.openelisglobal.hibernate.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

/**
 * Reusable Hibernate {@link UserType} that maps a PostgreSQL {@code jsonb}
 * column to a strongly-typed Java object via Jackson (de)serialization.
 *
 * <p>
 * Unlike {@link JsonBinaryType} — a String-only passthrough that writes
 * {@code value.toString()} and reads the raw cell back as a {@code String} —
 * this type binds a typed POJO. Register it with the target class as a
 * parameter:
 *
 * <pre>
 * &#64;TypeDef(name = "jsonb-object", typeClass = JsonbObjectType.class)
 * public class Foo {
 *     &#64;Type(type = "jsonb-object", parameters = &#64;Parameter(name = "class", value = "com.example.FooSnapshotDto"))
 *     &#64;Column(columnDefinition = "jsonb")
 *     private FooSnapshotDto snapshot;
 * }
 * </pre>
 *
 * <p>
 * Forward path: this is the Hibernate-5.6 bridge to typed JSON binding. When
 * the codebase moves to Hibernate 6 it can be replaced by the native
 * {@code @JdbcTypeCode(SqlTypes.JSON)} and retired. The legacy String-based
 * {@link JsonBinaryType} users can migrate onto this type incrementally.
 *
 * <p>
 * Dirty-checking compares serialized JSON (the target POJO need not implement
 * {@code equals()}); {@link #deepCopy(Object)} round-trips through Jackson for
 * a true copy.
 */
public class JsonbObjectType implements UserType, DynamicParameterizedType {

    // findAndRegisterModules picks up the repo's jackson-datatype-jdk8 / jsr310 /
    // hibernate5-jakarta modules so this type stays correct for any reusable DTO
    // (Optional, java.time, lazy proxies), not just plain-scalar POJOs.
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private Class<?> returnedClass;

    @Override
    public void setParameterValues(Properties parameters) {
        String className = parameters != null ? (String) parameters.get("class") : null;
        if (className == null && parameters != null) {
            // Fall back to the Hibernate-injected declared property type.
            className = parameters.getProperty(DynamicParameterizedType.RETURNED_CLASS);
        }
        if (className == null) {
            throw new HibernateException("JsonbObjectType requires a 'class' parameter naming the target type, e.g."
                    + " @Type(type = \"jsonb-object\", parameters = @Parameter(name = \"class\","
                    + " value = \"com.example.MyDto\"))");
        }
        try {
            this.returnedClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new HibernateException("JsonbObjectType cannot load target class " + className, e);
        }
    }

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.OTHER };
    }

    @Override
    public Class<?> returnedClass() {
        return returnedClass;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        try {
            return MAPPER.writeValueAsString(x).equals(MAPPER.writeValueAsString(y));
        } catch (Exception ex) {
            return x.equals(y);
        }
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        if (x == null) {
            return 0;
        }
        try {
            return MAPPER.writeValueAsString(x).hashCode();
        } catch (Exception ex) {
            return x.hashCode();
        }
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        final String content = rs.getString(names[0]);
        if (content == null) {
            return null;
        }
        try {
            return MAPPER.readValue(content, returnedClass);
        } catch (Exception ex) {
            throw new HibernateException("Failed to deserialize jsonb to " + returnedClass.getName(), ex);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        try {
            PGobject pgo = new PGobject();
            pgo.setType("jsonb");
            pgo.setValue(MAPPER.writeValueAsString(value));
            st.setObject(index, pgo);
        } catch (Exception ex) {
            throw new HibernateException("Failed to serialize " + value.getClass().getName() + " to jsonb", ex);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.readValue(MAPPER.writeValueAsString(value), value.getClass());
        } catch (Exception ex) {
            throw new HibernateException("Failed to deep-copy jsonb value", ex);
        }
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
            // Cache a portable JSON string rather than the live object graph.
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new HibernateException("Failed to disassemble jsonb value", ex);
        }
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        if (cached == null) {
            return null;
        }
        try {
            return MAPPER.readValue((String) cached, returnedClass);
        } catch (Exception ex) {
            throw new HibernateException("Failed to assemble jsonb value", ex);
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }
}
