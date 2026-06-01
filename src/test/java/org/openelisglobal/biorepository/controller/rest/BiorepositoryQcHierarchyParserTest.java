package org.openelisglobal.biorepository.controller.rest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BiorepositoryQcHierarchyParserTest {

    @Test
    public void parseHierarchyLevels_stripsNumericWellCoordinate() {
        String[] levels = BiorepositoryQcHierarchyParser
                .parseHierarchyLevels("fds > testbio > testbio > testbio > testbio > 3");

        assertArrayEquals(new String[] { "testbio", "testbio", "testbio", "testbio" }, levels);
    }

    @Test
    public void parseHierarchyLevels_stripsAlphanumericWellCoordinate() {
        String[] levels = BiorepositoryQcHierarchyParser
                .parseHierarchyLevels("fds > testbio > testbio > testbio > testbio > A1");

        assertArrayEquals(new String[] { "testbio", "testbio", "testbio", "testbio" }, levels);
    }

    @Test
    public void parseHierarchyLevels_supportsStandardFreezerPath() {
        String[] levels = BiorepositoryQcHierarchyParser.parseHierarchyLevels(
                "Ultra-Low Freezer 1 > Shelf-A > Rack-1 > Box-3 > B7");

        assertArrayEquals(
                new String[] { "Ultra-Low Freezer 1", "Shelf-A", "Rack-1", "Box-3" },
                levels);
    }

    @Test
    public void isWellCoordinateTail_matchesNumericAndAlphanumericWells() {
        assertTrue(BiorepositoryQcHierarchyParser.isWellCoordinateTail("3"));
        assertTrue(BiorepositoryQcHierarchyParser.isWellCoordinateTail("A1"));
        assertFalse(BiorepositoryQcHierarchyParser.isWellCoordinateTail("testbio"));
    }

    @Test
    public void parseRoomPrefixedRackLevels_stripsNumericTail() {
        String[] levels = BiorepositoryQcHierarchyParser
                .parseRoomPrefixedRackLevels("fds > testbio > testbio > testbio > 4");

        assertEquals("testbio", levels[0]);
        assertEquals("testbio", levels[1]);
        assertEquals("testbio", levels[2]);
    }
}
