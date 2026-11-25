package org.openelisglobal.program.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.program.controller.cytology.CytologySampleForm;
import org.openelisglobal.program.controller.immunohistochemistry.ImmunohistochemistrySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.cytology.CytologySampleServiceImpl;

public class ClinicalAttachmentBoundaryTest {
    @Test
    public void pathologyRejectsBeforeMutatingTheLoadedCase() {
        PathologySampleForm form = new PathologySampleForm();
        PathologySampleForm.PathologyReportForm report = new PathologySampleForm.PathologyReportForm();
        report.setBase64Image("data:text/html;base64,PHNjcmlwdD4=");
        form.setReports(List.of(report));
        PathologySampleServiceImpl service = spy(new PathologySampleServiceImpl());
        var stored = mock(org.openelisglobal.program.valueholder.pathology.PathologySample.class);
        when(stored.getReports()).thenReturn(List.of());
        doReturn(stored).when(service).get(1);
        assertThrows(InvalidClinicalAttachmentException.class, () -> service.updateWithFormValues(1, form));
        verify(stored).getReports();
        verifyNoMoreInteractions(stored);
    }

    @Test
    public void cytologyRejectsBeforeMutatingTheLoadedCase() {
        CytologySampleForm form = new CytologySampleForm();
        CytologySampleForm.CytologySlideForm slide = new CytologySampleForm.CytologySlideForm();
        slide.setBase64Image("data:text/html;base64,PHNjcmlwdD4=");
        form.setSlides(List.of(slide));
        CytologySampleServiceImpl service = mock(CytologySampleServiceImpl.class, CALLS_REAL_METHODS);
        var stored = mock(org.openelisglobal.program.valueholder.cytology.CytologySample.class);
        when(stored.getSlides()).thenReturn(List.of());
        doReturn(stored).when(service).get(1);
        assertThrows(InvalidClinicalAttachmentException.class, () -> service.updateWithFormValues(1, form));
        verify(stored).getSlides();
        verifyNoMoreInteractions(stored);
    }

    @Test
    public void immunohistochemistryRejectsBeforeMutatingTheLoadedCase() {
        ImmunohistochemistrySampleForm form = new ImmunohistochemistrySampleForm();
        ImmunohistochemistrySampleForm.ImmunohistochemistryReportForm report = new ImmunohistochemistrySampleForm.ImmunohistochemistryReportForm();
        report.setBase64Image("data:text/html;base64,PHNjcmlwdD4=");
        form.setReports(List.of(report));
        ImmunohistochemistrySampleServiceImpl service = spy(new ImmunohistochemistrySampleServiceImpl());
        var stored = mock(org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.class);
        when(stored.getReports()).thenReturn(List.of());
        doReturn(stored).when(service).get(1);
        assertThrows(InvalidClinicalAttachmentException.class, () -> service.updateWithFormValues(1, form));
        verify(stored).getReports();
        verifyNoMoreInteractions(stored);
    }
}
