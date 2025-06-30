package org.openelisglobal.sample.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.sample.form.SampleTbEntryForm;
import org.openelisglobal.sample.service.TbSampleService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SampleTbEntryController extends BaseSampleEntryController {

    @Value("${org.openelisglobal.requester.identifier:}")
    private String requestFhirUuid;

    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private TestService testService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private TbSampleService tbSampleService;

    @Autowired
    private PanelItemService panelItemService;

    private Task task = null;
    private Practitioner requesterPerson = null;
    private Practitioner collector = null;
    private org.hl7.fhir.r4.model.Organization referringOrganization = null;
    private Location location = null;
    private ServiceRequest serviceRequest = null;
    private Specimen specimen = null;
    private Patient fhirPatient = null;

    private static final String[] ALLOWED_FIELDS = new String[] {};

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/MicrobiologyTb", method = RequestMethod.GET)
    public ModelAndView showSampleEntryByProject(HttpServletRequest request) {
        SampleTbEntryForm form = new SampleTbEntryForm();
        request.setAttribute(IActionConstants.PAGE_SUBTITLE_KEY, MessageUtil.getMessage("add.tb.sample.title"));

        Date today = Calendar.getInstance().getTime();
        String dateAsText = DateUtil.formatDateAsText(today);
        form.setReceivedDate(dateAsText);

        setDisplayLists(form);
        addFlashMsgsToRequest(request);

        return findForward(FWD_SUCCESS, form);
    }

    @RequestMapping(value = "/MicrobiologyTb", method = RequestMethod.POST)
    public ModelAndView postTbSampleEntryByProject(HttpServletRequest request,
            @ModelAttribute("form") @Valid SampleTbEntryForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            saveErrors(result);
            setDisplayLists(form);
            return findForward(FWD_FAIL_INSERT, form);
        }
        form.setSysUserId(this.getSysUserId(request));
        if (tbSampleService.persistTbData(form, request)) {
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
            setDisplayLists(form);
            return findForward(FWD_SUCCESS_INSERT, form);
        }
        logAndAddMessage(request, "postTbSampleEntryByProject", "errors.UpdateException");

        return findForward(FWD_FAIL_INSERT, form);
    }

    private void setDisplayLists(SampleTbEntryForm form) {
        List<Dictionary> listOfDictionary = new ArrayList<>();
        List<IdValuePair> genders = DisplayListService.getInstance().getList(ListType.GENDERS);

        for (IdValuePair i : genders) {
            Dictionary dictionary = new Dictionary();
            dictionary.setId(i.getId());
            dictionary.setDictEntry(i.getValue());
            listOfDictionary.add(dictionary);
        }

        form.setGenders(genders);
        form.setReferralOrganizations(
                DisplayListService.getInstance().getList(ListType.SAMPLE_PATIENT_REFERRING_CLINIC));
        form.setTbSpecimenNatures(
                userService.getUserSampleTypes(getSysUserId(request), Constants.ROLE_RECEPTION, "TB"));
        form.setTestSectionList(DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE));
        form.setCurrentDate(DateUtil.getCurrentDateAsText());
        form.setRejectReasonList(DisplayListService.getInstance().getList(ListType.REJECTION_REASONS));
        form.setTbOrderReasons(DisplayListService.getInstance().getList(ListType.TB_ORDER_REASONS));
        form.setTbDiagnosticReasons(DisplayListService.getInstance().getList(ListType.TB_DIAGNOSTIC_REASONS));
        form.setTbFollowupReasons(DisplayListService.getInstance().getList(ListType.TB_FOLLOWUP_REASONS));
        form.setTbDiagnosticMethods(DisplayListService.getInstance().getList(ListType.TB_ANALYSIS_METHODS));
        form.setTbAspects(DisplayListService.getInstance().getList(ListType.TB_SAMPLE_ASPECTS));
        form.setTbFollowupPeriodsLine1(DisplayListService.getInstance().getList(ListType.TB_FOLLOWUP_LINE1));
        form.setTbFollowupPeriodsLine2(DisplayListService.getInstance().getList(ListType.TB_FOLLOWUP_LINE2));
    }

    @GetMapping(value = "MicrobiologyTb/panel_test")
    public ResponseEntity<Map<String, Object>> getPanelTestsElement(@RequestParam("method") String method) {
        Map<String, Object> response = new HashMap<String, Object>();
        try {
            List<Test> tests = testService.getTbTestByMethod(method);
            List<Panel> panels = testService.getTbPanelsByMethod(method);
            List<Map<String, Object>> testsList = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> panelsList = new ArrayList<Map<String, Object>>();
            tests.forEach(test -> {
                Map<String, Object> el = new HashMap<String, Object>();
                el.put("id", test.getId());
                el.put("name", test.getLocalizedName());
                List<PanelItem> pItems = panelItemService.getPanelItemByTestId(test.getId());
                pItems.forEach(item -> {
                    int idxPanel = 0;
                    Map<String, Object> sPanel = new HashMap<String, Object>();
                    for (int k = 0; k < panelsList.size(); k++) {
                        if (panelsList.get(k).get("name").equals(item.getPanel().getLocalizedName())) {
                            sPanel = panelsList.get(k);
                            idxPanel = k;
                            break;
                        }
                    }
                    if (ObjectUtils.isEmpty(sPanel)) {
                        sPanel.put("name", item.getPanel().getLocalizedName());
                        sPanel.put("id", item.getPanel().getId());
                        sPanel.put("test_ids", "" + test.getId());
                        panelsList.add(sPanel);
                    } else {
                        sPanel.put("test_ids", sPanel.get("test_ids") + "," + test.getId());
                        panelsList.set(idxPanel, sPanel);
                    }
                });
                testsList.add(el);
            });

            List<Map<String, Object>> newPanelsList = new ArrayList<Map<String, Object>>();
            List<String> realPanelIds = panels.stream().map(p -> p.getId()).collect(Collectors.toList());
            panelsList.forEach(elm -> {
                if (realPanelIds.contains(elm.get("id"))) {
                    newPanelsList.add(elm);
                }
            });

            response.put("tests", testsList);
            response.put("panels", newPanelsList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleTbEntryDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/MicrobiologyTb";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "homePageDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
