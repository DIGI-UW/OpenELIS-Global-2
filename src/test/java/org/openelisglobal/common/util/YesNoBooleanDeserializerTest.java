package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.Test;
import org.openelisglobal.test.beanItems.TestResultItem;

/**
 * A results row serves reportable as "Y"/"N"; posting the row back unchanged
 * must not be refused because the field only accepted a JSON boolean.
 */
public class YesNoBooleanDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private String reportableOf(String json) throws Exception {
        return mapper.readerFor(TestResultItem.class)
                .without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .<TestResultItem>readValue(json).getReportable();
    }

    @Test
    public void theYesNoValueTheRowIsServedWithIsAccepted() throws Exception {
        assertEquals("Y", reportableOf("{\"reportable\":\"Y\"}"));
        assertEquals("N", reportableOf("{\"reportable\":\"N\"}"));
    }

    @Test
    public void jsonBooleansAndTheirTextStillWork() throws Exception {
        assertEquals("Y", reportableOf("{\"reportable\":true}"));
        assertEquals("N", reportableOf("{\"reportable\":false}"));
        assertEquals("Y", reportableOf("{\"reportable\":\"true\"}"));
        assertEquals("N", reportableOf("{\"reportable\":\"false\"}"));
    }

    @Test
    public void anythingElseIsStillRejected() {
        assertThrows(InvalidFormatException.class, () -> reportableOf("{\"reportable\":\"maybe\"}"));
    }
}
