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
        assertEquals(Domain.VECTOR, Domain.fromRaw("V"));
    }

    /**
     * The vector sample-type CSVs ship {@code domain=V}. Before "V" was mapped,
     * normalize() fell through to the CLINICAL default, so those rows were stored
     * as CLINICAL: they leaked into clinical order entry while the vector endpoints
     * returned nothing and every vector species/trap row was skipped.
     */
    @Test
    public void fromRaw_mapsVectorShorthandV() {
        assertEquals(Domain.VECTOR, Domain.fromRaw("V"));
        assertEquals(Domain.VECTOR, Domain.fromRaw(" v "));
        assertEquals("VECTOR", Domain.normalize("V"));
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
