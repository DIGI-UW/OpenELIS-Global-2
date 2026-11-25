package org.openelisglobal.resultrun.service;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.RunControlVerdict;
import org.openelisglobal.resultrun.form.RunTestVerdict;
import org.openelisglobal.testcatalog.valueholder.TestQcPolicy;

public class RunQcServiceImplTest {

    @Test
    public void verdictOfReturnsNotRequiredWhenEmptyAndNotRequired() {
        TestQcPolicy policy = new TestQcPolicy();
        policy.setControlRequired(false);

        assertEquals(RunTestVerdict.NOT_REQUIRED, RunQcServiceImpl.verdictOf(List.of(), policy));
        assertEquals(RunTestVerdict.NOT_REQUIRED, RunQcServiceImpl.verdictOf(List.of(), null));
    }

    @Test
    public void verdictOfReturnsNoControlWhenEmptyAndRequired() {
        TestQcPolicy policy = new TestQcPolicy();
        policy.setControlRequired(true);

        assertEquals(RunTestVerdict.NO_CONTROL, RunQcServiceImpl.verdictOf(List.of(), policy));
    }

    @Test
    public void verdictOfReturnsPassWhenEveryControlPassed() {
        QCResult c1 = new QCResult();
        c1.setRunVerdict(RunControlVerdict.PASS);
        QCResult c2 = new QCResult();
        c2.setRunVerdict(RunControlVerdict.PASS);

        assertEquals(RunTestVerdict.PASS, RunQcServiceImpl.verdictOf(List.of(c1, c2), null));
    }

    @Test
    public void verdictOfReturnsFailWhenAnyControlFailed() {
        QCResult c1 = new QCResult();
        c1.setRunVerdict(RunControlVerdict.PASS);
        QCResult c2 = new QCResult();
        c2.setRunVerdict(RunControlVerdict.FAIL);

        assertEquals(RunTestVerdict.FAIL, RunQcServiceImpl.verdictOf(List.of(c1, c2), null));
    }

    @Test
    public void verdictOfDoesNotReturnPassWhenControlWasNeverJudged() {
        QCResult unjudged = new QCResult();
        unjudged.setRunVerdict(null);

        TestQcPolicy required = new TestQcPolicy();
        required.setControlRequired(true);

        TestQcPolicy optional = new TestQcPolicy();
        optional.setControlRequired(false);

        assertEquals(RunTestVerdict.NO_CONTROL, RunQcServiceImpl.verdictOf(List.of(unjudged), required));
        assertEquals(RunTestVerdict.NOT_REQUIRED, RunQcServiceImpl.verdictOf(List.of(unjudged), optional));
        assertEquals(RunTestVerdict.NOT_REQUIRED, RunQcServiceImpl.verdictOf(List.of(unjudged), null));
    }

    @Test
    public void verdictOfDoesNotReturnPassWhenPassingControlAccompaniedByUnjudgedControl() {
        QCResult passed = new QCResult();
        passed.setRunVerdict(RunControlVerdict.PASS);
        QCResult unjudged = new QCResult();
        unjudged.setRunVerdict(null);

        TestQcPolicy required = new TestQcPolicy();
        required.setControlRequired(true);

        assertEquals(RunTestVerdict.NO_CONTROL, RunQcServiceImpl.verdictOf(List.of(passed, unjudged), required));
        assertEquals(RunTestVerdict.NOT_REQUIRED, RunQcServiceImpl.verdictOf(List.of(passed, unjudged), null));
    }
}
