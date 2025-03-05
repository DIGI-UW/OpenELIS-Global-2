package org.openelisglobal.referral.fhir.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.referral.fhir.form.FhirReferralForm;
import org.openelisglobal.referral.fhir.form.FhirReferralSearchForm;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FhirReferralReceptionController extends BaseController {

    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private StatusOfSampleService statusOfSampleService;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private DisplayListService displayListService;

    private static final String[] ALLOWED_FIELDS = new String[] { "externalAccessionNumber", "patientID",
            "patientLastName", "patientFirstName", "dateOfBirth", "gender", "page" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping("/FhirReferralReception")
    public ModelAndView showFhirReferralReceptionPage(@ModelAttribute @Valid FhirReferralSearchForm searchForm,
            BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            searchForm.setPage(1);
        }

        FhirReferralForm form = new FhirReferralForm(searchForm);
        form.setGenders(displayListService.getList(ListType.GENDERS));
        if (!GenericValidator.isBlankOrNull(searchForm.getExternalAccessionNumber())
                || !GenericValidator.isBlankOrNull(searchForm.getPatientFirstName())
                || !GenericValidator.isBlankOrNull(searchForm.getPatientLastName())
                || !GenericValidator.isBlankOrNull(searchForm.getPatientID())
                || !GenericValidator.isBlankOrNull(searchForm.getDateOfBirth())
                || !GenericValidator.isBlankOrNull(searchForm.getGender())) {
            List<ElectronicOrder> electronicOrders = electronicOrderService
                    .getAllElectronicOrdersContainingValuesOrderedBy(searchForm.getExternalAccessionNumber(),
                            searchForm.getPatientLastName(), searchForm.getPatientFirstName(), searchForm.getGender(),
                            SortOrder.LAST_UPDATED_ASC);
            // TODO add to sql query instead of filtering here
            electronicOrders = electronicOrders.stream()
                    .filter(e -> e.getData().contains(fhirConfig.getOeFhirSystem() + "/refer_reason"))
                    .collect(Collectors.toList());
            form.setElectronicOrders(electronicOrders);

            // correct for proper bounds
            int startIndex = (searchForm.getPage() - 1) * 50;
            startIndex = startIndex > electronicOrders.size() ? 0 : startIndex;
            int endIndex = startIndex + 50 > electronicOrders.size() ? electronicOrders.size() : startIndex + 50;

            // set attributes for use in jsp
            request.setAttribute("startIndex", startIndex);
            request.setAttribute("endIndex", endIndex);
            request.setAttribute("total", electronicOrders.size());
            for (ElectronicOrder eOrder : electronicOrders) {
                eOrder.setStatus(statusOfSampleService.get(eOrder.getStatusId()));
            }
        }
        return findForward(FWD_SUCCESS, form);
    }

    @PostMapping("/FhirReferralReception")
    public ModelAndView referAnalysises(@ModelAttribute("form") FhirReferralForm form) {

        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        switch (forward) {
        case FWD_SUCCESS:
            return "fhirReferralDefinition";
        default:
            return "";
        }
    }

    @Override
    protected String getPageTitleKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        // TODO Auto-generated method stub
        return null;
    }
}
