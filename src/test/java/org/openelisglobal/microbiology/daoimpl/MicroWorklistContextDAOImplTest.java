package org.openelisglobal.microbiology.daoimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Date;
import java.sql.Timestamp;
import org.junit.Test;

public class MicroWorklistContextDAOImplTest {

    private static final long FIXTURE_TIME = 1_786_259_145_000L;

    @Test
    public void temporalValuesAcceptJdbcAndGenericDateRepresentations() {
        Timestamp timestamp = new Timestamp(FIXTURE_TIME);
        Date date = new Date(FIXTURE_TIME);
        java.util.Date genericDate = new java.util.Date(FIXTURE_TIME);

        assertEquals(timestamp, MicroWorklistContextDAOImpl.timestampValue(timestamp));
        assertEquals(timestamp, MicroWorklistContextDAOImpl.timestampValue(genericDate));
        assertEquals(date, MicroWorklistContextDAOImpl.dateValue(date));
        assertEquals(date, MicroWorklistContextDAOImpl.dateValue(genericDate));
        assertNull(MicroWorklistContextDAOImpl.timestampValue(null));
        assertNull(MicroWorklistContextDAOImpl.dateValue(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void timestampValueRejectsUnsupportedTypes() {
        MicroWorklistContextDAOImpl.timestampValue("2026-08-08");
    }
}
