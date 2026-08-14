package org.openelisglobal.testsupport.dbunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.sql.PreparedStatement;
import java.sql.Types;
import org.dbunit.dataset.datatype.DataType;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.postgresql.util.PGobject;

public class PostgresqlJsonbDataTypeFactoryTest {

    @Test
    public void jsonbValuesAreBoundAsPostgresqlJsonbObjects() throws Exception {
        DataType dataType = new PostgresqlJsonbDataTypeFactory().createDataType(Types.OTHER, "jsonb");
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);

        dataType.setSqlValue("{\"marker\":\"copied\"}", 2, statement);

        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(statement).setObject(eq(2), value.capture());
        assertTrue(value.getValue() instanceof PGobject);
        PGobject jsonb = (PGobject) value.getValue();
        assertEquals("jsonb", jsonb.getType());
        assertEquals("{\"marker\":\"copied\"}", jsonb.getValue());
    }
}
