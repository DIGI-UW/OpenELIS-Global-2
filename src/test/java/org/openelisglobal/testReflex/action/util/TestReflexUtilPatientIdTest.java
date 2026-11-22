package org.openelisglobal.testReflex.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.testreflex.action.util.TestReflexBean;
import org.openelisglobal.testreflex.action.util.TestReflexUtil;

/**
 * OGC-1192 §2 — reflex handling for a patientless (environmental) sample: the
 * patient id passed along to reflex actions is null instead of a
 * NullPointerException on {@code reflexBean.getPatient().getId()}.
 */
public class TestReflexUtilPatientIdTest {

    @Test
    public void patientIdOf_isNull_whenTheSampleHasNoPatient() {
        TestReflexBean bean = new TestReflexBean();
        assertNull(TestReflexUtil.patientIdOf(bean));
    }

    @Test
    public void patientIdOf_isThePatientId_whenThereIsOne() {
        Patient patient = new Patient();
        patient.setId("42");
        TestReflexBean bean = new TestReflexBean();
        bean.setPatient(patient);
        assertEquals("42", TestReflexUtil.patientIdOf(bean));
    }
}
