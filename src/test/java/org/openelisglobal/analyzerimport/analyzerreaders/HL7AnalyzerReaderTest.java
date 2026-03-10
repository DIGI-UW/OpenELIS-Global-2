/**
 * Unit tests for HL7AnalyzerReader (ORU^R01 parse, readStream, MSH identification).
 *
 */
package org.openelisglobal.analyzerimport.analyzerreaders;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.core.io.ClassPathResource;

public class HL7AnalyzerReaderTest extends BaseWebContextSensitiveTest {

    private HL7AnalyzerReader reader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        reader = new HL7AnalyzerReader();
    }

    @Test
    public void readStream_validMindrayCbc_returnsTrue() throws Exception {
        String raw = loadFixture("testdata/hl7/mindray-cbc-result.hl7");
        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
        boolean ok = reader.readStream(in);
        assertTrue("readStream should succeed", ok);
        assertNull("error should be null", reader.getError());
    }

    @Test
    public void readStream_validSysmex_returnsTrue() throws Exception {
        String raw = loadFixture("testdata/hl7/sysmex-result.hl7");
        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
        boolean ok = reader.readStream(in);
        assertTrue("readStream should succeed", ok);
        assertNull("error should be null", reader.getError());
    }

    @Test
    public void readStream_empty_returnsFalse() {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        boolean ok = reader.readStream(in);
        assertFalse("readStream should fail", ok);
        assertNotNull("error should be set", reader.getError());
    }

    @Test
    public void readStream_invalidHL7_returnsFalse() {
        InputStream in = new ByteArrayInputStream("not valid hl7".getBytes(StandardCharsets.UTF_8));
        boolean ok = reader.readStream(in);
        assertFalse("readStream should fail", ok);
        assertNotNull("error should be set", reader.getError());
    }

    private static String loadFixture(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
