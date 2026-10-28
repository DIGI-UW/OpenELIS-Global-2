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
    public void pathologyRejectsBeforeLoadingOrMutatingTheCase() {
        PathologySampleForm form = new PathologySampleForm();
        PathologySampleForm.PathologyReportForm report = new PathologySampleForm.PathologyReportForm();
        report.setBase64Image("data:text/html;base64,PHNjcmlwdD4=");
        form.setReports(List.of(report));
        PathologySampleServiceImpl service = spy(new PathologySampleServiceImpl());
        assertThrows(InvalidClinicalAttachmentException.class, () -> service.updateWithFormValues(1, form));
        verify(service, never()).get(1);
    }

    @Test
    public void cytologyRejectsBeforeLoadingOrMutatingTheCase() {
        CytologySampleForm form = new CytologySampleForm();
        CytologySampleForm.CytologySlideForm slide = new CytologySampleForm.CytologySlideForm();
        slide.setBase64Image("data:text/html;base64,PHNjcmlwdD4=");
        form.setSlides(List.of(slide));
        CytologySampleServiceImpl service = mock(CytologySampleServiceImpl.class, CALLS_REAL_METHODS);
        assertThrows(InvalidClinicalAttachmentException.class, () -> service.updateWithFormValues(1, form));
        verify(service, never()).get(1);
    }

    @Test
    public void immunohistochemistryRejectsBeforeLoadingOrMutatingTheCase() {
        ImmunohistochemistrySampleForm form = new ImmunohistochemistrySampleForm();
        ImmunohistochemistrySampleForm.ImmunohistochemistryReportForm report = new ImmunohistochemistrySampleForm.ImmunohistochemistryReportForm();
        report.setBase64Image("data:text/html;base64,PHNjcmlwdD4=");
        form.setReports(List.of(report));
        ImmunohistochemistrySampleServiceImpl service = spy(new ImmunohistochemistrySampleServiceImpl());
        assertThrows(InvalidClinicalAttachmentException.class, () -> service.updateWithFormValues(1, form));
        verify(service, never()).get(1);
    }
}
