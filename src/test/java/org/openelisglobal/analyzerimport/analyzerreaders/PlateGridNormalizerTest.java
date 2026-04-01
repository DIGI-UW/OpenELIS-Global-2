package org.openelisglobal.analyzerimport.analyzerreaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PlateGridNormalizerTest {

    @Test
    public void testIsPlateGridFormat_WithTecanGrid_ReturnsTrue() {
        List<String> lines = Arrays.asList("Application\tMagellan", "Instrument\tInfinite F50", "Method\tHIV_ELISA_450",
                "", "<>\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12",
                "A\t2.345\t0.048\t1.234\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "B\t2.401\t0.047\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "C\t0.051\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "D\t1.567\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "E\t0.098\t0.101\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "F\t0.099\t0.103\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "G\t0.050\t0.049\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "H\t0.048\t0.051\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0");
        assertTrue(PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testIsPlateGridFormat_WithWellPerRow_ReturnsFalse() {
        List<String> lines = Arrays.asList("WellPosition\tSampleID\tOD_450", "A01\tTCN-001\t2.345",
                "A02\tTCN-002\t0.048");
        assertFalse(PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testNormalizeToWellPerRow_ProducesCorrectRowCount() {
        List<String> lines = Arrays.asList("Application\tMagellan", "", "<>\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12",
                "A\t2.345\t0.048\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "B\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "C\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "D\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "E\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "F\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "G\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "H\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0");
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, "\t");
        assertEquals(97, result.size());
        assertEquals("WellPosition\tSampleID\tOD_450", result.get(0));
        assertEquals("A1\t\t2.345", result.get(1));
        assertEquals("A2\t\t0.048", result.get(2));
    }
}
