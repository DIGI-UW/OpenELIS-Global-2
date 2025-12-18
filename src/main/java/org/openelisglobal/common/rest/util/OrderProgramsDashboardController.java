package org.openelisglobal.common.rest.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.rest.provider.bean.OrderPrograms;
import org.openelisglobal.common.rest.provider.form.OrderProgramsDashboardForm;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.bean.DashboardSummary;
import org.openelisglobal.program.service.OrderProgramsDisplayService;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.valueholder.OrderProgramDisplayItem;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest")
public class OrderProgramsDashboardController extends BaseRestController {

    @Autowired
    private OrderProgramsDisplayService orderProgramsDisplayService;

    @Autowired
    private ProgramSampleService programSampleService;

    @Autowired
    private SampleService sampleService;

    private final OrderProgramsDashboardPaging paging = new OrderProgramsDashboardPaging();

    @GetMapping(value = "/programSamplesList", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DashboardSummary> getPaginatedProgramSamples(HttpServletRequest request,
            @RequestParam(required = false) String filter)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        OrderProgramsDashboardForm form = new OrderProgramsDashboardForm();
        List<OrderPrograms> orderPrograms;

        String requestedPage = request.getParameter("page");

        if (requestedPage == null || requestedPage.isBlank()) {

            List<ProgramSample> samples = (filter != null && !filter.isEmpty())
                    ? programSampleService.getProgramSamplesByAccessionNumberOrProgramName(filter)
                    : programSampleService.getAll();

            orderPrograms = samples.stream().map(this::convertToOrderPrograms).toList();

            paging.setDatabaseOrderPrograms(request, form, orderPrograms);

            paging.page(request, form, 1);

        } else {

            int pageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, pageNumber);

            orderPrograms = paging.getOrderPrograms(request);
        }

        DashboardSummary summary = new DashboardSummary();
        summary.setOrderProgramsDashboardForm(form);
        summary.setTotalEntries(orderPrograms.size());

        return ResponseEntity.ok(summary);
    }

    @GetMapping(value = "/programSample/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderProgramDisplayItem> getProgramSampleDisplayItem(@PathVariable int id) {
        OrderProgramDisplayItem psdi = orderProgramsDisplayService.getOrderProgramById(id);
        return ResponseEntity.ok(psdi);
    }

    private OrderPrograms convertToOrderPrograms(ProgramSample ps) {
        Patient patient = sampleService.getPatient(ps.getSample());
        OrderPrograms item = new OrderPrograms();
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
