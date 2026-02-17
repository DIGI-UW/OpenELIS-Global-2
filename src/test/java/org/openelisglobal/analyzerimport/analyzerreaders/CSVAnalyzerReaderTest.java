/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) I-TECH UW. All Rights Reserved.
 *
 * <p>Contributor(s): I-TECH, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzerimport.analyzerreaders;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for CSVAnalyzerReader.
 */
public class CSVAnalyzerReaderTest {

    private CSVAnalyzerReader reader;

    private static final String VALID_CSV = "SampleID,TestCode,Result,Units\n" + "12345,GLU,95,mg/dL\n"
            + "12346,HBA1C,6.5,%\n" + "12347,CHOL,180,mg/dL\n";

    private static final String VALID_CSV_WITH_EMPTY_LINES = "SampleID,TestCode,Result,Units\n" + "12345,GLU,95,mg/dL\n"
            + "\n" + "12346,HBA1C,6.5,%\n" + "\n";

    private static final String EMPTY_CSV = "";

    private static final String SINGLE_LINE_CSV = "SampleID,TestCode,Result,Units";

    private static final String MALFORMED_CSV = "SampleID\n" + "12345\n" + "12346\n";

    @Before
    public void setUp() {
        reader = new CSVAnalyzerReader();
    }

    @Test
    public void testReadStream_ValidCSV() {
        // Arrange
        InputStream stream = new ByteArrayInputStream(VALID_CSV.getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = reader.readStream(stream);

        // Assert
        assertTrue("Should successfully read valid CSV", result);
        assertNull("Should have no errors", reader.getError());

        List<String> lines = reader.getLines();
        assertNotNull(lines);
        assertEquals(4, lines.size()); // Header + 3 data rows
        assertEquals("SampleID,TestCode,Result,Units", lines.get(0));
        assertEquals("12345,GLU,95,mg/dL", lines.get(1));
    }

    @Test
    public void testReadStream_SkipsEmptyLines() {
        // Arrange
        InputStream stream = new ByteArrayInputStream(VALID_CSV_WITH_EMPTY_LINES.getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = reader.readStream(stream);

        // Assert
        assertTrue(result);
        List<String> lines = reader.getLines();
        assertEquals(3, lines.size()); // Should skip empty lines, only header + 2 data rows with content
    }

    @Test
    public void testReadStream_EmptyCSV() {
        // Arrange
        InputStream stream = new ByteArrayInputStream(EMPTY_CSV.getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = reader.readStream(stream);

        // Assert
        assertFalse("Should fail on empty CSV", result);
        assertNotNull(reader.getError());
        assertTrue(reader.getError().contains("Empty CSV message"));
    }

    @Test
    public void testReadStream_NullStream() {
        // Arrange
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        // Act
        boolean result = reader.readStream(stream);

        // Assert
        assertFalse(result);
        assertNotNull(reader.getError());
    }

    @Test
    public void testIsValidCSV_ValidFormat() {
        // Arrange
        List<String> validLines = Arrays.asList("SampleID,TestCode,Result,Units", "12345,GLU,95,mg/dL",
                "12346,HBA1C,6.5,%");

        // Act
        boolean result = CSVAnalyzerReader.isValidCSV(validLines);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testIsValidCSV_NullLines() {
        // Act
        boolean result = CSVAnalyzerReader.isValidCSV(null);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testIsValidCSV_EmptyLines() {
        // Act
        boolean result = CSVAnalyzerReader.isValidCSV(Collections.emptyList());

        // Assert
        assertFalse(result);
    }

    @Test
    public void testIsValidCSV_OnlyHeader() {
        // Arrange
        List<String> headerOnly = Arrays.asList("SampleID,TestCode,Result,Units");

        // Act
        boolean result = CSVAnalyzerReader.isValidCSV(headerOnly);

        // Assert
        assertFalse("Should require at least 2 lines (header + data)", result);
    }

    @Test
    public void testIsValidCSV_SingleColumn() {
        // Arrange
        List<String> singleColumn = Arrays.asList("SingleColumn", "Value1");

        // Act
        boolean result = CSVAnalyzerReader.isValidCSV(singleColumn);

        // Assert
        assertFalse("Should require at least 2 columns", result);
    }

    @Test
    public void testIsValidCSV_EmptyHeader() {
        // Arrange
        List<String> emptyHeader = Arrays.asList("", "12345,GLU,95");

        // Act
        boolean result = CSVAnalyzerReader.isValidCSV(emptyHeader);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testGetLines() {
        // Arrange
        InputStream stream = new ByteArrayInputStream(VALID_CSV.getBytes(StandardCharsets.UTF_8));
        reader.readStream(stream);

        // Act
        List<String> lines = reader.getLines();

        // Assert
        assertNotNull(lines);
        assertEquals(4, lines.size());
        assertEquals("SampleID,TestCode,Result,Units", lines.get(0));
    }

    @Test
    public void testReadStream_UTF8Encoding() {
        // Arrange - CSV with special characters
        String csvWithSpecialChars = "SampleID,TestCode,Result,Units\n" + "12345,GLU,95,mg/dL\n"
                + "12346,H\u00e9moglobine,6.5,%\n" + "12347,Prot\u00e9ine,180,g/L\n";
        InputStream stream = new ByteArrayInputStream(csvWithSpecialChars.getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = reader.readStream(stream);

        // Assert
        assertTrue(result);
        List<String> lines = reader.getLines();
        assertTrue(lines.get(2).contains("H\u00e9moglobine"));
        assertTrue(lines.get(3).contains("Prot\u00e9ine"));
    }

    @Test
    public void testReadStream_LargeCSV() {
        // Arrange - Generate large CSV
        StringBuilder largeCSV = new StringBuilder("SampleID,TestCode,Result,Units\n");
        for (int i = 0; i < 1000; i++) {
            largeCSV.append(String.format("%05d,TEST%d,%d,mg/dL\n", i, i, i * 10));
        }
        InputStream stream = new ByteArrayInputStream(largeCSV.toString().getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = reader.readStream(stream);

        // Assert
        assertTrue(result);
        List<String> lines = reader.getLines();
        assertEquals(1001, lines.size()); // Header + 1000 data rows
    }

    @Test
    public void testInsertAnalyzerData_NoInserterSet() {
        // Arrange
        InputStream stream = new ByteArrayInputStream(VALID_CSV.getBytes(StandardCharsets.UTF_8));
        reader.readStream(stream);

        // Act - No plugins registered, so inserter will be null
        boolean result = reader.insertAnalyzerData("testUser");

        // Assert
        assertFalse("Should fail when no inserter is available", result);
        assertNotNull(reader.getError());
        assertTrue(reader.getError().contains("Unable to understand which analyzer"));
    }

    @Test
    public void testGetError_NoError() {
        // Act
        String error = reader.getError();

        // Assert
        assertNull("Should have no error initially", error);
    }

    @Test
    public void testReadStream_HandlesQuotedFields() {
        // Arrange - CSV with quoted fields
        String csvWithQuotes = "SampleID,TestCode,Result,Units\n" + "12345,\"GLU, Glucose\",95,mg/dL\n"
                + "12346,\"Test with \\\"quotes\\\"\",6.5,%\n";
        InputStream stream = new ByteArrayInputStream(csvWithQuotes.getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = reader.readStream(stream);

        // Assert
        assertTrue(result);
        List<String> lines = reader.getLines();
        assertEquals(3, lines.size());
        assertTrue(lines.get(1).contains("GLU, Glucose"));
    }

    @Test
    public void testReadStream_HandlesWindowsLineEndings() {
        // Arrange - CSV with Windows line endings (\r\n)
        String csvWindows = "SampleID,TestCode,Result,Units\r\n12345,GLU,95,mg/dL\r\n12346,HBA1C,6.5,%\r\n";
        InputStream stream = new ByteArrayInputStream(csvWindows.getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = reader.readStream(stream);

        // Assert
        assertTrue(result);
        List<String> lines = reader.getLines();
        assertEquals(3, lines.size());
    }

    @Test
    public void testReadStream_HandlesTrailingCommas() {
        // Arrange - CSV with trailing commas
        String csvTrailingCommas = "SampleID,TestCode,Result,Units,\n" + "12345,GLU,95,mg/dL,\n"
                + "12346,HBA1C,6.5,%,\n";
        InputStream stream = new ByteArrayInputStream(csvTrailingCommas.getBytes(StandardCharsets.UTF_8));

        // Act
        boolean result = reader.readStream(stream);

        // Assert
        assertTrue(result);
        List<String> lines = reader.getLines();
        assertTrue(lines.size() >= 3);
    }
}
