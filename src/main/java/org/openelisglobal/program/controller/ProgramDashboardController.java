package org.openelisglobal.program.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.rest.provider.bean.ViewItems;
import org.openelisglobal.common.rest.provider.form.GenericProgramDashboardForm;
import org.openelisglobal.common.rest.util.GenericProgramDashboardPaging;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.bean.DashboardSummary;
import org.openelisglobal.program.service.GenericProgramDisplayService;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.ProgramSampleDisplayItem;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest")
public class ProgramDashboardController extends BaseRestController {

    @Autowired
    private GenericProgramDisplayService genericProgramDisplayService;

    @Autowired
    private ProgramSampleService programSampleService;

    @Autowired
    private SampleService sampleService;

    private final GenericProgramDashboardPaging paging = new GenericProgramDashboardPaging();

    @GetMapping(value = "/programSamplesList", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DashboardSummary> getPaginatedProgramSamples(HttpServletRequest request,
            @RequestParam(required = false) String filter)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        GenericProgramDashboardForm form = new GenericProgramDashboardForm();
        List<ViewItems> viewItems;

        String requestedPage = request.getParameter("page");

        if (requestedPage == null || requestedPage.isBlank()) {

            List<ProgramSample> samples = (filter != null && !filter.isEmpty())
                    ? programSampleService.getProgramSamplesByAccessionNumberOrProgramName(filter)
                    : programSampleService.getAll();

            viewItems = samples.stream().map(this::convertToViewItem).toList();

            paging.setDatabaseResults(request, form, viewItems);

            paging.page(request, form, 1);

        } else {

            int pageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, pageNumber);

            viewItems = paging.getResults(request);
        }

        DashboardSummary summary = new DashboardSummary();
        summary.setGenericProgramDashboardForm(form);
        summary.setTotalEntries(viewItems.size());

        return ResponseEntity.ok(summary);
    }

    @GetMapping(value = "/programSample/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProgramSampleDisplayItem> getProgramSampleDisplayItem(@PathVariable int id) {
        ProgramSampleDisplayItem psdi = genericProgramDisplayService.getProgramSampleById(id);
        return ResponseEntity.ok(psdi);
    }

    private ViewItems convertToViewItem(ProgramSample ps) {
        Patient patient = sampleService.getPatient(ps.getSample());
        ViewItems item = new ViewItems();
        item.setProgramSampleId(ps.getId().toString());
        item.setFirstName(patient.getPerson().getFirstName());
        item.setLastName(patient.getPerson().getLastName());
        item.setGender(patient.getGender());
        item.setPatientPK(patient.getId());
        item.setProgramName(ps.getProgram().getProgramName());
        item.setProgramCode(ps.getProgram().getCode());
        item.setReceivedDate(ps.getSample().getReceivedDate());
        item.setAccessionNumber(ps.getSample().getAccessionNumber());
        item.setQuestionnaireResponseUuid(ps.getQuestionnaireResponseUuid());
        return item;
    }
}
