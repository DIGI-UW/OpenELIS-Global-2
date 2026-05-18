package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link ASTMOrderBuilder}. The field-layout contract is
 * load-bearing: the mock's _process_order parses ASTM by pipe-splitting and
 * indexing parts[2] for the accession and parts[4] for the universal test ID,
 * so these tests pin exactly which field each datum lands in.
 */
public class ASTMOrderBuilderTest {

    private final ASTMOrderBuilder builder = new ASTMOrderBuilder();

    @Test
    public void singleTestOrder_headerPatientOrderTerminator_crSeparated() {
        String msg = builder.build("ACC-12345", "PAT-99", List.of("MTB-RIF"));
        String[] records = msg.split("\r");
        assertEquals("H|\\^&|||OpenELIS^Order^1.0|||||||LIS2-A2", records[0]);
        assertEquals("P|1|||PAT-99", records[1]);
        assertEquals("O|1|ACC-12345||^^^MTB-RIF|R", records[2]);
        assertEquals("L|1|N", records[3]);
    }

    @Test
    public void orderFieldLayoutMatchesMockParser() {
        String msg = builder.build("ACC-7", null, List.of("WBC"));
        String orderRecord = Arrays.stream(msg.split("\r")).filter(r -> r.startsWith("O|")).findFirst()
                .orElseThrow(() -> new AssertionError("missing O record"));
        String[] parts = orderRecord.split("\\|", -1);
        assertEquals("specimen ID must be at field index 2", "ACC-7", parts[2]);
        assertEquals("instrument specimen ID must be empty at field index 3", "", parts[3]);
        assertEquals("universal test ID must be at field index 4", "^^^WBC", parts[4]);
    }

    @Test
    public void multiTestOrder_oneORecordPerTest_sequenceIncrements() {
        String msg = builder.build("ACC-CBC", "PAT-1", List.of("WBC", "RBC", "HGB", "HCT"));
        long oCount = Arrays.stream(msg.split("\r")).filter(r -> r.startsWith("O|")).count();
        assertEquals(4, oCount);
        assertTrue(msg.contains("O|1|ACC-CBC||^^^WBC|R"));
        assertTrue(msg.contains("O|2|ACC-CBC||^^^RBC|R"));
        assertTrue(msg.contains("O|3|ACC-CBC||^^^HGB|R"));
        assertTrue(msg.contains("O|4|ACC-CBC||^^^HCT|R"));
    }

    @Test
    public void nullPatientId_emitsEmptyField_noNpe() {
        String msg = builder.build("ACC-1", null, List.of("X"));
        assertTrue("P record should have empty ID with no NPE", msg.contains("\rP|1|||\r"));
    }

    @Test
    public void blankTestCodes_skipped_seqNotIncremented() {
        String msg = builder.build("ACC-1", "PAT", Arrays.asList("WBC", "", null, "RBC"));
        long oCount = Arrays.stream(msg.split("\r")).filter(r -> r.startsWith("O|")).count();
        assertEquals("blank/null test codes must be skipped", 2, oCount);
        assertTrue(msg.contains("O|1|ACC-1||^^^WBC|R"));
        assertTrue(msg.contains("O|2|ACC-1||^^^RBC|R"));
    }

    @Test
    public void trailingCarriageReturn_lastRecordIsTerminated() {
        String msg = builder.build("ACC-1", "PAT", List.of("X"));
        assertTrue("message must end with CR", msg.endsWith("\r"));
    }

    @Test
    public void nullAccession_throwsNpe() {
        assertThrows(NullPointerException.class, () -> builder.build(null, "PAT", List.of("X")));
    }

    @Test
    public void blankAccession_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> builder.build("   ", "PAT", List.of("X")));
    }

    @Test
    public void emptyTestCodes_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> builder.build("ACC-1", "PAT", Collections.emptyList()));
    }

    @Test
    public void allBlankTestCodes_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> builder.build("ACC-1", "PAT", Arrays.asList("", null, "  ")));
    }
}
