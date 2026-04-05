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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzer.service;

import java.util.List;

/**
 * Service for HL7 v2.x message parsing and generation.
 *
 * <p>
 *
 * <p>
 * Parses ORU^R01 result messages and generates ORM^O01 order messages for
 * analyzer integration.
 */
public interface HL7MessageService {

    /**
     * Parse an ORU^R01 observation result message.
     *
     * @param rawMessage Raw HL7 message string (pipe-delimited, segment-terminated)
     * @return Parsed result containing patient ID, order info, and observation
     *         results
     * @throws org.openelisglobal.analyzer.service.HL7MessageService.HL7ParseException if
     *                                                                                 parsing
     *                                                                                 fails
     */
    OruR01ParseResult parseOruR01(String rawMessage);

    /**
     * Generate an ORM^O01 order message for exporting orders to an analyzer.
     *
     * @param request Order details (patient, tests, etc.)
     * @return Encoded HL7 ORM^O01 message string
     * @throws org.openelisglobal.analyzer.service.HL7MessageService.HL7GenerationException if
     *                                                                                      generation
     *                                                                                      fails
     */
    String generateOrmO01(OrmO01Request request);

    /**
     * Extract MSH segment sender info for analyzer identification.
     *
     * @param rawMessage Raw HL7 message string
     * @return Sending application and facility from MSH-3, MSH-4
     */
    MshInfo extractMshInfo(String rawMessage);

    /**
     * Convert raw HL7 message to segment lines (one segment per line) for mapping
     * pipeline.
     *
     * @param rawMessage Raw HL7 message string
     * @return List of segment strings (MSH|..., PID|..., etc.)
     */
    List<String> toSegmentLines(String rawMessage);

    /** Parsed ORU^R01 result. */
    interface OruR01ParseResult {
        String getPatientId();

        String getPlacerOrderNumber();

        String getFillerOrderNumber();

        String getServiceId();

        List<ObxResult> getResults();
    }

    /** Single OBX observation result. */
    interface ObxResult {
        String getTestCode();

        String getTestName();

        String getValue();

        String getUnits();

        String getValueType();
    }

    /** MSH sender info for analyzer identification. */
    interface MshInfo {
        String getSendingApplication();

        String getSendingFacility();
    }

    /** Request for generating ORM^O01. */
    interface OrmO01Request {
        String getPatientId();

        String getPatientLastName();

        String getPatientFirstName();

        String getPatientDob();

        String getPatientGender();

        String getPlacerOrderNumber();

        String getFillerOrderNumber();

        /** Receiving application (MSH-5) when OpenELIS sends ORM to analyzer. */
        String getReceivingApplication();

        /** Receiving facility (MSH-6) when OpenELIS sends ORM to analyzer. */
        String getReceivingFacility();

        List<OrmOrderItem> getOrders();
    }

    /** Single order item for ORM^O01. */
    interface OrmOrderItem {
        String getTestCode();

        String getTestName();
    }

    /** Thrown when ORU^R01 parsing fails. */
    class HL7ParseException extends RuntimeException {
        public HL7ParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public HL7ParseException(String message) {
            super(message);
        }
    }

    /** Thrown when ORM^O01 generation fails. */
    class HL7GenerationException extends RuntimeException {
        public HL7GenerationException(String message, Throwable cause) {
            super(message, cause);
        }

        public HL7GenerationException(String message) {
            super(message);
        }
    }
}
