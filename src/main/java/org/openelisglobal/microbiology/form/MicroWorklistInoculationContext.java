package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public record MicroWorklistInoculationContext(String caseId, Timestamp firstInoculatedAt) {
}
