package org.openelisglobal.common.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DomainTest {

    @Test
    public void fromRaw_mapsLegacyCharsPerD030() {
        assertEquals(Domain.CLINICAL, Domain.fromRaw("H"));
        assertEquals(Domain.CLINICAL, Domain.fromRaw("N"));
        assertEquals(Domain.ENVIRONMENTAL, Domain.fromRaw("E"));
        assertEquals(Domain.VECTOR, Domain.fromRaw("A"));
    }

    @Test
    public void fromRaw_passesEnumValuesThrough() {
        assertEquals(Domain.CLINICAL, Domain.fromRaw("CLINICAL"));
        assertEquals(Domain.ENVIRONMENTAL, Domain.fromRaw("ENVIRONMENTAL"));
        assertEquals(Domain.VECTOR, Domain.fromRaw("VECTOR"));
    }

    @Test
    public void fromRaw_isCaseAndWhitespaceInsensitive() {
        assertEquals(Domain.ENVIRONMENTAL, Domain.fromRaw(" e "));
        assertEquals(Domain.VECTOR, Domain.fromRaw("vector"));
    }

    @Test
    public void fromRaw_returnsNullForBlankOrUnknown() {
        assertNull(Domain.fromRaw(null));
        assertNull(Domain.fromRaw(""));
        assertNull(Domain.fromRaw("  "));
        assertNull(Domain.fromRaw("X"));
        assertNull(Domain.fromRaw("BOTH"));
    }

    @Test
    public void normalize_returnsEnumName_defaultingClinical() {
        assertEquals("CLINICAL", Domain.normalize("H"));
        assertEquals("ENVIRONMENTAL", Domain.normalize("E"));
        assertEquals("VECTOR", Domain.normalize("A"));
        assertEquals("CLINICAL", Domain.normalize(null));
        assertEquals("CLINICAL", Domain.normalize("nonsense"));
    }
}
