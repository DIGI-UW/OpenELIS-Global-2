package org.openelisglobal.testsupport.dbunit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.dbunit.dataset.datatype.AbstractDataType;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.dataset.datatype.TypeCastException;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.postgresql.util.PGobject;

/**
 * PostgreSQL DBUnit factory with the JSONB support missing from DBUnit 2.7.3.
 */
public class PostgresqlJsonbDataTypeFactory extends PostgresqlDataTypeFactory {

    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        if (sqlType == Types.OTHER && "jsonb".equalsIgnoreCase(sqlTypeName)) {
            return new JsonbDataType();
        }
        return super.createDataType(sqlType, sqlTypeName);
    }

    private static final class JsonbDataType extends AbstractDataType {

        private JsonbDataType() {
            super("jsonb", Types.OTHER, String.class, false);
        }

        @Override
        public Object typeCast(Object value) throws TypeCastException {
            return value == null ? null : value.toString();
        }

        @Override
        public Object getSqlValue(int column, ResultSet resultSet) throws SQLException {
            return resultSet.getString(column);
        }

        @Override
        public void setSqlValue(Object value, int column, PreparedStatement statement)
                throws SQLException, TypeCastException {
            if (value == null) {
                statement.setNull(column, Types.OTHER);
                return;
            }
            PGobject jsonb = new PGobject();
            jsonb.setType("jsonb");
            jsonb.setValue((String) typeCast(value));
            statement.setObject(column, jsonb);
        }
    }
}
