package org.openelisglobal.organization.locations;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Test;

/**
 * A row the save refuses says why: the record's field messages, not the
 * wrapper's "The record was not saved".
 */
public class ImportRejectReasonTest {

    @Test
    public void aRefusedSaveNamesTheFieldMessages() {
        RuntimeException failure = new RuntimeException("transaction failed",
                new LocationsValidationException("The record was not saved",
                        Map.of("referral.fhirId", "Another organization already uses this FHIR id: Mbale")));
        assertEquals("Another organization already uses this FHIR id: Mbale",
                LocationsImportServiceImpl.rejectReason(failure));
    }

    @Test
    public void anyOtherFailureKeepsItsDeepestMessage() {
        RuntimeException failure = new RuntimeException("could not execute batch",
                new IllegalStateException("value too long for type character varying(10)"));
        assertEquals("value too long for type character varying(10)", LocationsImportServiceImpl.rejectReason(failure));
    }
}
