package org.openelisglobal.qaevent.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation.AssociationType;

/**
 * Unit tests for NceResultAssociationServiceImpl. Tests business logic methods
 * including validation, severity comparison, and entity construction.
 */
public class NceResultAssociationServiceImplTest {

    private final NceResultAssociationServiceImpl service = new NceResultAssociationServiceImpl();

    // --- validateAssociation tests ---

    @Test(expected = IllegalArgumentException.class)
    public void validateAssociation_nullResult_throwsException() {
        NcEvent ncEvent = createNcEventWithId("1");
        service.validateAssociation(null, ncEvent, AssociationType.RESULT_TRIGGERED_NCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateAssociation_blankResult_throwsException() {
        NcEvent ncEvent = createNcEventWithId("1");
        service.validateAssociation("  ", ncEvent, AssociationType.RESULT_TRIGGERED_NCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateAssociation_nullNcEvent_throwsException() {
        service.validateAssociation("result-1", null, AssociationType.RESULT_TRIGGERED_NCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateAssociation_nullAssociationType_throwsException() {
        NcEvent ncEvent = createNcEventWithId("1");
        service.validateAssociation("result-1", ncEvent, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateAssociation_ncEventWithNullId_throwsException() {
        NcEvent ncEvent = new NcEvent();
        ncEvent.setId(null);
        service.validateAssociation("result-1", ncEvent, AssociationType.RESULT_TRIGGERED_NCE);
    }

    @Test
    public void validateAssociation_validInputs_doesNotThrow() {
        NcEvent ncEvent = createNcEventWithId("1");

        // Should not throw any exception
        service.validateAssociation("result-1", ncEvent, AssociationType.RESULT_TRIGGERED_NCE);
    }

    @Test
    public void validateAssociation_allAssociationTypes_allAccepted() {
        NcEvent ncEvent = createNcEventWithId("1");

        for (AssociationType type : AssociationType.values()) {
            service.validateAssociation("result-1", ncEvent, type);
        }
    }

    // --- NceResultAssociation entity construction tests ---

    @Test
    public void nceResultAssociationConstructor_setsAllFields() {
        NcEvent ncEvent = createNcEventWithId("1");

        NceResultAssociation association = new NceResultAssociation("result-1", ncEvent,
                AssociationType.RESULT_TRIGGERED_NCE, "admin");

        assertEquals("result-1", association.getResultId());
        assertEquals(ncEvent, association.getNcEvent());
        assertEquals("RESULT_TRIGGERED_NCE", association.getAssociationType());
        assertEquals("admin", association.getCreatedBy());
        assertNotNull(association.getCreatedDate());
    }

    @Test
    public void nceResultAssociationEnumConversion_roundTrip() {
        NceResultAssociation association = new NceResultAssociation();

        association.setAssociationTypeEnum(AssociationType.DELTA_CHECK_ESCALATION);
        assertEquals("DELTA_CHECK_ESCALATION", association.getAssociationType());
        assertEquals(AssociationType.DELTA_CHECK_ESCALATION, association.getAssociationTypeEnum());
    }

    @Test
    public void nceResultAssociationEnumConversion_invalidString_returnsFallback() {
        NceResultAssociation association = new NceResultAssociation();
        association.setAssociationType("INVALID_TYPE");

        // Should fall back to RESULT_TRIGGERED_NCE per implementation
        assertEquals(AssociationType.RESULT_TRIGGERED_NCE, association.getAssociationTypeEnum());
    }

    @Test
    public void nceResultAssociation_allAssociationTypesHaveDescriptions() {
        for (AssociationType type : AssociationType.values()) {
            assertNotNull("AssociationType " + type.name() + " should have a description", type.getDescription());
            assertFalse("AssociationType " + type.name() + " description should not be empty",
                    type.getDescription().isEmpty());
        }
    }

    @Test
    public void nceResultAssociation_equalsAndHashCode() {
        NceResultAssociation a1 = new NceResultAssociation();
        a1.setId(1);
        NceResultAssociation a2 = new NceResultAssociation();
        a2.setId(1);
        NceResultAssociation a3 = new NceResultAssociation();
        a3.setId(2);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, a3);
    }

    // --- Helper methods ---

    private NcEvent createNcEventWithId(String id) {
        NcEvent ncEvent = new NcEvent();
        ncEvent.setId(id);
        return ncEvent;
    }
}
