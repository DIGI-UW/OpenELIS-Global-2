package org.openelisglobal.storage.service;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for CodeGenerationService
 * 
 * References: - Testing Roadmap: .specify/guides/testing-roadmap.md - Backend
 * Best Practices: .specify/guides/backend-testing-best-practices.md - Template:
 * JUnit 4 Service Test
 * 
 * TDD Workflow (MANDATORY for complex logic): - RED: Write failing test first
 * (defines expected behavior) - GREEN: Write minimal code to make test pass -
 * REFACTOR: Improve code quality while keeping tests green
 * 
 * Task Reference: T284
 * 
 * Test Naming: test{MethodName}_{Scenario}_{ExpectedResult}
 */
@RunWith(MockitoJUnitRunner.class)
public class CodeGenerationServiceTest {

    private CodeGenerationService codeGenerationService;

    @Before
    public void setUp() {
        codeGenerationService = new CodeGenerationServiceImpl();
    }

    /**
     * Test code generation algorithm T284: Code Generation Algorithm - Uppercase,
     * remove non-alphanumeric, keep hyphens/underscores, truncate to 10 chars
     */
    @Test
    public void testCodeGenerationAlgorithm() {
        // Test basic generation
        String code = codeGenerationService.generateCodeFromName("Main Lab", "room");
        assertEquals("Should generate uppercase code", "MAINLAB", code);

        // Test with hyphens (should be kept)
        code = codeGenerationService.generateCodeFromName("Main-Lab", "room");
        assertEquals("Should keep hyphens", "MAIN-LAB", code);

        // Test with underscores (should be kept)
        code = codeGenerationService.generateCodeFromName("Main_Lab", "room");
        assertEquals("Should keep underscores", "MAIN_LAB", code);

        // Test removal of non-alphanumeric (except hyphens/underscores)
        code = codeGenerationService.generateCodeFromName("Main Lab #1", "room");
        assertEquals("Should remove special chars", "MAINLAB1", code);

        // Test truncation to 10 chars
        code = codeGenerationService.generateCodeFromName("Very Long Laboratory Name", "room");
        assertEquals("Should truncate to 10 chars", 10, code.length());
        assertTrue("Should start with first chars", code.startsWith("VERYLONG"));
    }

    /**
     * Test uppercase conversion T284: Auto-Uppercase Conversion - All input
     * converted to uppercase
     */
    @Test
    public void testUppercaseConversion() {
        String code = codeGenerationService.generateCodeFromName("main lab", "room");
        assertEquals("Should convert to uppercase", "MAINLAB", code);

        code = codeGenerationService.generateCodeFromName("Main Lab", "room");
        assertEquals("Should convert to uppercase", "MAINLAB", code);

        code = codeGenerationService.generateCodeFromName("MAIN LAB", "room");
        assertEquals("Should keep uppercase", "MAINLAB", code);
    }

    /**
     * Test removal of non-alphanumeric characters T284: Remove Non-Alphanumeric -
     * Keep only A-Z, 0-9, hyphens, underscores
     */
    @Test
    public void testRemoveNonAlphanumeric() {
        // Test removal of spaces
        String code = codeGenerationService.generateCodeFromName("Main Lab", "room");
        assertEquals("Should remove spaces", "MAINLAB", code);

        // Test removal of special characters
        code = codeGenerationService.generateCodeFromName("Main@Lab#1", "room");
        assertEquals("Should remove special chars", "MAINLAB1", code);

        // Test keeping hyphens
        code = codeGenerationService.generateCodeFromName("Main-Lab", "room");
        assertEquals("Should keep hyphens", "MAIN-LAB", code);

        // Test keeping underscores
        code = codeGenerationService.generateCodeFromName("Main_Lab", "room");
        assertEquals("Should keep underscores", "MAIN_LAB", code);
    }

    /**
     * Test truncation to 10 characters T284: Truncate To 10 Chars - Code must be
     * ≤10 characters
     */
    @Test
    public void testTruncateTo10Chars() {
        // Test exact 10 chars
        String code = codeGenerationService.generateCodeFromName("1234567890", "room");
        assertEquals("Should handle exact 10 chars", 10, code.length());

        // Test longer than 10 chars
        code = codeGenerationService.generateCodeFromName("Very Long Laboratory Name", "room");
        assertEquals("Should truncate to 10 chars", 10, code.length());
        assertTrue("Should start with first chars", code.startsWith("VERYLONG"));

        // Test with hyphens (truncation should account for hyphens)
        code = codeGenerationService.generateCodeFromName("Very-Long-Name-That-Exceeds", "room");
        assertEquals("Should truncate to 10 chars", 10, code.length());
    }

    /**
     * Test conflict resolution with numeric suffix T284: Conflict Resolution -
     * Append numeric suffix if conflict (e.g., "MAINLAB-1")
     */
    @Test
    public void testConflictResolution() {
        Set<String> existingCodes = new HashSet<>();
        existingCodes.add("MAINLAB");

        // Test conflict resolution
        String code = codeGenerationService.generateCodeWithConflictResolution("Main Lab", "room", existingCodes);
        assertEquals("Should append numeric suffix", "MAINLAB-1", code);

        // Test multiple conflicts
        existingCodes.add("MAINLAB-1");
        code = codeGenerationService.generateCodeWithConflictResolution("Main Lab", "room", existingCodes);
        assertEquals("Should append next numeric suffix", "MAINLAB-2", code);

        // Test no conflict
        existingCodes.clear();
        code = codeGenerationService.generateCodeWithConflictResolution("Main Lab", "room", existingCodes);
        assertEquals("Should return base code when no conflict", "MAINLAB", code);
    }

    /**
     * Test numeric suffix appending T284: Numeric Suffix Appending - Append "-1",
     * "-2", etc. for conflicts
     */
    @Test
    public void testNumericSuffixAppending() {
        Set<String> existingCodes = new HashSet<>();
        existingCodes.add("MAINLAB");
        existingCodes.add("MAINLAB-1");
        existingCodes.add("MAINLAB-2");

        String code = codeGenerationService.generateCodeWithConflictResolution("Main Lab", "room", existingCodes);
        assertEquals("Should append next available suffix", "MAINLAB-3", code);

        // Test that suffix doesn't exceed 10 chars total
        existingCodes.clear();
        existingCodes.add("VERYLONGN"); // 9 chars
        code = codeGenerationService.generateCodeWithConflictResolution("Very Long Name", "room", existingCodes);
        // Should truncate base code if suffix would exceed 10 chars
        assertTrue("Code should be ≤10 chars", code.length() <= 10);
    }

    /**
     * Test edge cases
     */
    @Test
    public void testEdgeCases() {
        // Test empty string
        String code = codeGenerationService.generateCodeFromName("", "room");
        assertNotNull("Should handle empty string", code);
        assertTrue("Should generate fallback code", code.length() > 0);

        // Test all special characters
        code = codeGenerationService.generateCodeFromName("!!!###", "room");
        assertNotNull("Should handle all special chars", code);
        assertTrue("Should generate fallback code", code.length() > 0);

        // Test single character
        code = codeGenerationService.generateCodeFromName("A", "room");
        assertEquals("Should handle single char", "A", code);

        // Test null (should handle gracefully)
        try {
            code = codeGenerationService.generateCodeFromName(null, "room");
            assertNotNull("Should handle null", code);
        } catch (Exception e) {
            // Exception is acceptable for null input
            assertTrue("Should throw meaningful exception",
                    e instanceof IllegalArgumentException || e instanceof NullPointerException);
        }
    }
}
