package org.openelisglobal.typeofsample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

public class TypeOfSampleConfigurationHandlerIntegrationTest extends BaseWebContextSensitiveTest {
    private static final String ABBREVIATION = "FSWHONET";

    @Autowired
    private TypeOfSampleConfigurationHandler handler;
    @Autowired
    private TypeOfSampleService sampleTypes;

    @After
    public void removeSampleType() {
        TypeOfSample sampleType = sampleType();
        if (sampleType != null) {
            sampleTypes.delete(sampleType);
        }
    }

    private void load(String csv) throws Exception {
        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "test.csv");
    }

    private TypeOfSample sampleType() {
        return sampleTypes.getTypeOfSampleByLocalAbbrevAndDomain(ABBREVIATION, "H");
    }

    @Test
    public void aWhonetCodeIsSetOnACreatedAndAnUpdatedSampleType() throws Exception {
        load("description,localAbbreviation,whonetCode\nFresh site urine," + ABBREVIATION + ",ur\n");
        assertEquals("ur", sampleType().getWhonetCode());

        load("description,localAbbreviation,WHONETCODE\nFresh site urine," + ABBREVIATION + ",sf\n");
        assertEquals("sf", sampleType().getWhonetCode());
    }

    @Test
    public void aFileWithoutTheColumnLeavesTheWhonetCode() throws Exception {
        aUrineSampleType();

        load("description,localAbbreviation\nFresh site urine," + ABBREVIATION + "\n");

        assertEquals("ur", sampleType().getWhonetCode());
    }

    @Test
    public void aBlankCellClearsTheWhonetCode() throws Exception {
        aUrineSampleType();

        load("description,localAbbreviation,whonetCode\nFresh site urine," + ABBREVIATION + ",\n");

        assertNull(sampleType().getWhonetCode());
    }

    private void aUrineSampleType() throws Exception {
        load("description,localAbbreviation\nFresh site urine," + ABBREVIATION + "\n");
        TypeOfSample sampleType = sampleType();
        sampleType.setWhonetCode("ur");
        sampleType.setSysUserId("1");
        sampleTypes.update(sampleType);
    }
}
