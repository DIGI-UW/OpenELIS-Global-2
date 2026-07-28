package org.openelisglobal.typeofsample.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SampleTypeDomainMapperTest {

    @Test
    public void normalize_mapsLegacyCharsPerD030() {
        assertEquals("CLINICAL", SampleTypeDomainMapper.normalize("H"));
        assertEquals("CLINICAL", SampleTypeDomainMapper.normalize("N"));
        assertEquals("ENVIRONMENTAL", SampleTypeDomainMapper.normalize("E"));
        assertEquals("VECTOR", SampleTypeDomainMapper.normalize("A"));
    }

    @Test
    public void normalize_passesEnumValuesThrough() {
        assertEquals("CLINICAL", SampleTypeDomainMapper.normalize("CLINICAL"));
        assertEquals("ENVIRONMENTAL", SampleTypeDomainMapper.normalize("ENVIRONMENTAL"));
        assertEquals("VECTOR", SampleTypeDomainMapper.normalize("VECTOR"));
    }

    @Test
    public void normalize_isCaseAndWhitespaceInsensitive() {
        assertEquals("ENVIRONMENTAL", SampleTypeDomainMapper.normalize(" e "));
        assertEquals("VECTOR", SampleTypeDomainMapper.normalize("vector"));
        assertEquals("CLINICAL", SampleTypeDomainMapper.normalize("clinical"));
    }

    @Test
    public void normalize_defaultsUnknownAndBlankToClinical() {
        assertEquals("CLINICAL", SampleTypeDomainMapper.normalize(null));
        assertEquals("CLINICAL", SampleTypeDomainMapper.normalize(""));
        assertEquals("CLINICAL", SampleTypeDomainMapper.normalize("  "));
        assertEquals("CLINICAL", SampleTypeDomainMapper.normalize("X"));
        assertEquals("CLINICAL", SampleTypeDomainMapper.normalize("BOTH"));
    }
}
