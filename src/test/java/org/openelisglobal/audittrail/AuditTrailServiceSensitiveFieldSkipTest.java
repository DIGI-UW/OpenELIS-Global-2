package org.openelisglobal.audittrail;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import org.junit.Test;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.patient.valueholder.PatientIdDocument;

/**
 * Locks the sensitive-field skip list in AuditTrailServiceImpl.getChanges. The
 * field names declared in SENSITIVE_FIELD_NAMES must never appear in the audit
 * XML, regardless of which service triggers the audit emit. Tests the
 * reflection path directly so this passes without the full Hibernate / Spring
 * fixture.
 */
public class AuditTrailServiceSensitiveFieldSkipTest {

    private String invokeGetChanges(AuditTrailServiceImpl svc, Object newObj, Object oldObj) throws Exception {
        Method m = AuditTrailServiceImpl.class.getDeclaredMethod("getChanges",
                org.openelisglobal.common.valueholder.BaseObject.class,
                org.openelisglobal.common.valueholder.BaseObject.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(svc, newObj, oldObj, "patient_id_document");
    }

    @Test
    public void documentDataAndThumbnailData_skippedFromAuditXml() throws Exception {
        AuditTrailServiceImpl svc = new AuditTrailServiceImpl();

        PatientIdDocument oldDoc = new PatientIdDocument();
        oldDoc.setId(1);
        oldDoc.setPatientId("99");
        oldDoc.setDocumentData("ORIGINAL-BASE64-PAYLOAD-XXXXXXXXXXXXXXXXXXXX");
        oldDoc.setThumbnailData("ORIGINAL-THUMB-PAYLOAD-YYYYYYYYYYYYYYYY");
        oldDoc.setDocumentCategory("NATIONAL_ID");
        oldDoc.setDescription("original description");

        PatientIdDocument newDoc = new PatientIdDocument();
        newDoc.setId(1);
        newDoc.setPatientId("99");
        newDoc.setDocumentData("UPDATED-BASE64-PAYLOAD-ZZZZZZZZZZZZZZZZZZZZ");
        newDoc.setThumbnailData("UPDATED-THUMB-PAYLOAD-WWWWWWWWWWWWWWWW");
        newDoc.setDocumentCategory("NATIONAL_ID");
        newDoc.setDescription("changed description");

        String xml = invokeGetChanges(svc, newDoc, oldDoc);

        assertTrue("Audit XML must be non-empty (description changed) — got: " + xml, xml != null && xml.length() > 0);
        assertTrue("Audit XML must record the description change — got: " + xml, xml.contains("original description"));
        assertFalse("documentData base64 must NOT appear in audit XML — got: " + xml,
                xml.contains("ORIGINAL-BASE64-PAYLOAD"));
        assertFalse("thumbnailData base64 must NOT appear in audit XML — got: " + xml,
                xml.contains("ORIGINAL-THUMB-PAYLOAD"));
    }

    @Test
    public void documentDataChangedAlone_producesNoAuditRow() throws Exception {
        AuditTrailServiceImpl svc = new AuditTrailServiceImpl();

        PatientIdDocument oldDoc = new PatientIdDocument();
        oldDoc.setId(2);
        oldDoc.setPatientId("99");
        oldDoc.setDocumentData("ONLY-DOC-DATA-CHANGED-OLD");
        oldDoc.setDescription("same");

        PatientIdDocument newDoc = new PatientIdDocument();
        newDoc.setId(2);
        newDoc.setPatientId("99");
        newDoc.setDocumentData("ONLY-DOC-DATA-CHANGED-NEW");
        newDoc.setDescription("same");

        String xml = invokeGetChanges(svc, newDoc, oldDoc);

        // documentData is skipped; description is unchanged; no other field
        // differs; therefore optionList is empty and getChanges returns null.
        assertTrue("When only sensitive fields change, no audit XML should be produced. Got: " + xml,
                xml == null || xml.isEmpty());
    }
}
