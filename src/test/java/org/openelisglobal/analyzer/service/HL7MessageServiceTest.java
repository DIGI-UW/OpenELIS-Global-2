/**
 * Unit tests for HL7MessageService (ORU^R01 parsing, ORM^O01 generation, MSH extraction).
 *
 */
package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class HL7MessageServiceTest {

    private HL7MessageService service;

    @Before
    public void setUp() {
        service = new HL7MessageServiceImpl();
    }

    @Test
    public void parseOruR01_mindrayCbc_extractsPatientAndResults() throws IOException {
        String raw = loadFixture("testdata/hl7/mindray-cbc-result.hl7");
        HL7MessageService.OruR01ParseResult result = service.parseOruR01(raw);

        assertNotNull(result);
        assertTrue("Patient ID", result.getPatientId().contains("PAT001"));
        assertEquals("PLACER123", result.getPlacerOrderNumber());
        assertEquals("FILLER456", result.getFillerOrderNumber());
        assertTrue("Service ID non-empty", result.getServiceId() != null && !result.getServiceId().isEmpty());
        assertTrue("Results count", result.getResults().size() >= 4);

        List<HL7MessageService.ObxResult> results = result.getResults();
        assertTrue("WBC", results.stream().anyMatch(
                r -> "WBC".equals(r.getTestCode()) || (r.getTestCode() != null && r.getTestCode().contains("WBC"))));
        assertTrue("RBC", results.stream().anyMatch(
                r -> "RBC".equals(r.getTestCode()) || (r.getTestCode() != null && r.getTestCode().contains("RBC"))));
    }

    @Test
    public void parseOruR01_sysmex_extractsPatientAndResults() throws IOException {
        String raw = loadFixture("testdata/hl7/sysmex-result.hl7");
        HL7MessageService.OruR01ParseResult result = service.parseOruR01(raw);

        assertNotNull(result);
        assertTrue("Patient ID", result.getPatientId().contains("PAT002"));
        assertEquals("PLACER456", result.getPlacerOrderNumber());
        assertEquals("FILLER789", result.getFillerOrderNumber());
        assertTrue("Results count", result.getResults().size() >= 3);
    }

    @Test(expected = HL7MessageService.HL7ParseException.class)
    public void parseOruR01_empty_throws() {
        service.parseOruR01("");
    }

    @Test(expected = HL7MessageService.HL7ParseException.class)
    public void parseOruR01_null_throws() {
        service.parseOruR01(null);
    }

    @Test
    public void extractMshInfo_mindray_returnsSendingAppAndFacility() throws IOException {
        String raw = loadFixture("testdata/hl7/mindray-cbc-result.hl7");
        HL7MessageService.MshInfo msh = service.extractMshInfo(raw);

        assertNotNull(msh);
        assertEquals("MINDRAY", msh.getSendingApplication());
        assertEquals("LAB", msh.getSendingFacility());
    }

    @Test
    public void extractMshInfo_sysmex_returnsSendingAppAndFacility() throws IOException {
        String raw = loadFixture("testdata/hl7/sysmex-result.hl7");
        HL7MessageService.MshInfo msh = service.extractMshInfo(raw);

        assertNotNull(msh);
        assertEquals("SYSMEX", msh.getSendingApplication());
        assertEquals("LAB", msh.getSendingFacility());
    }

    @Test
    public void toSegmentLines_returnsOneLinePerSegment() throws IOException {
        String raw = loadFixture("testdata/hl7/mindray-cbc-result.hl7");
        List<String> lines = service.toSegmentLines(raw);

        assertNotNull(lines);
        assertTrue("Has MSH", lines.stream().anyMatch(l -> l.startsWith("MSH|")));
        assertTrue("Has PID", lines.stream().anyMatch(l -> l.startsWith("PID|")));
        assertTrue("Has OBX", lines.stream().anyMatch(l -> l.startsWith("OBX|")));
    }

    @Test
    public void generateOrmO01_producesValidOrm() {
        HL7MessageService.OrmO01Request request = new TestOrmRequest();
        String encoded = service.generateOrmO01(request);

        assertNotNull(encoded);
        assertTrue("ORM^O01", encoded.contains("ORM") && encoded.contains("O01"));
        assertTrue("MSH", encoded.startsWith("MSH|"));
        assertTrue("OpenELIS sender", encoded.contains("OpenELIS"));
        assertTrue("MINDRAY receiver", encoded.contains("MINDRAY"));
        assertTrue("PAT001", encoded.contains("PAT001"));
        assertTrue("WBC", encoded.contains("WBC"));
    }

    @Test(expected = HL7MessageService.HL7GenerationException.class)
    public void generateOrmO01_nullRequest_throws() {
        service.generateOrmO01(null);
    }

    private static String loadFixture(String path) throws IOException {
        ClassPathResource r = new ClassPathResource(path);
        try (InputStream in = r.getInputStream()) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    private static final class TestOrmRequest implements HL7MessageService.OrmO01Request {
        @Override
        public String getPatientId() {
            return "PAT001";
        }

        @Override
        public String getPatientLastName() {
            return "DOE";
        }

        @Override
        public String getPatientFirstName() {
            return "JOHN";
        }

        @Override
        public String getPatientDob() {
            return "19800115";
        }

        @Override
        public String getPatientGender() {
            return "M";
        }

        @Override
        public String getPlacerOrderNumber() {
            return "ORD123";
        }

        @Override
        public String getFillerOrderNumber() {
            return "FILL001";
        }

        @Override
        public String getReceivingApplication() {
            return "MINDRAY";
        }

        @Override
        public String getReceivingFacility() {
            return "LAB";
        }

        @Override
        public List<HL7MessageService.OrmOrderItem> getOrders() {
            List<HL7MessageService.OrmOrderItem> items = new ArrayList<>();
            items.add(new HL7MessageService.OrmOrderItem() {
                @Override
                public String getTestCode() {
                    return "WBC";
                }

                @Override
                public String getTestName() {
                    return "WHITE BLOOD CELL";
                }
            });
            return items;
        }
    }
}
