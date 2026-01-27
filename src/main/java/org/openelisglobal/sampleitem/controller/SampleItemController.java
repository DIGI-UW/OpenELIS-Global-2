package org.openelisglobal.sampleitem.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.apache.commons.beanutils.BeanUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.form.SampleItemAliquotForm;
import org.openelisglobal.sampleitem.form.SampleItemForm;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(value = "/rest/")
public class SampleItemController extends BaseController {

    private final String[] ALLOWED_FIELDS = new String[] {
            // Simple fields
            "accessionNumber",
            // SampleItems array fields
            "sampleItems*.sampleItemIdNumber", "sampleItems*.externalId", "sampleItems*.typeOfSample",
            "sampleItems*.collectionDate", "sampleItems*.collector", "sampleItems*.quantity",
            // UnitOfMeasure nested fields (if needed)
            "sampleItems*.uom.id", "sampleItems*.uom.name", "sampleItems*.uom.description", "sampleItems*.uom.code",
            // Analysis nested fields (if needed)
            "sampleItems*.analysis*.id", "sampleItems*.analysis*.status", "sampleItems*.analysis*.analysisType",
            "sampleItems*.analysis*.startedDate", "sampleItems*.analysis*.completedDate",
            "sampleItems*.analysis*.releasedDate" };
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private AnalysisService analysisService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "SampleItem", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SampleItemForm getSampleItemByAccessionNumber(HttpServletRequest request,
            @RequestParam(required = true) String accessionNumber) {

        SampleItemForm form = new SampleItemForm();
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);

        if (sample != null && !org.apache.commons.validator.GenericValidator.isBlankOrNull(sample.getId())) {
            form.setAccessionNumber(sample.getAccessionNumber());
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());

            List<SampleItemForm.SampleItemEntry> sampleItemEntries = new ArrayList<>();

            for (SampleItem item : sampleItems) {
                List<Analysis> analysisList = analysisService.getAnalysesBySampleItemsExcludingByStatusIds(item, null);

                // Create a new sample item entry
                SampleItemForm.SampleItemEntry entry = new SampleItemForm.SampleItemEntry();
                entry.setSampleItemIdNumber(item.getSampleItemId());
                entry.setTypeOfSample(item.getTypeOfSample().getDescription());
                entry.setCollectionDate(item.getCollectionDate());
                entry.setCollector(item.getCollector());
                entry.setQuantity(item.getQuantity());
                entry.setUom(item.getUnitOfMeasure());
                entry.setExternalId(item.getExternalId());
                entry.setAnalysis(analysisList);

                sampleItemEntries.add(entry);
            }

            form.setSampleItems(sampleItemEntries);
        }

        return form;
    }

    @PostMapping(value = "Aliquot", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, List<String>> updateSampleItem(HttpServletRequest request,
            @Validated(SampleItemAliquotForm.SampleItemAliquot.class) @RequestBody SampleItemAliquotForm form,
            BindingResult result) {

        Map<String, List<String>> response = new HashMap<>();

        try {
            if (result.hasErrors()) {
                response.put("errors", List.of("Validation failed"));
                return response;
            }

            String accessionNumber = form.getAccessionNumber();
            List<SampleItemAliquotForm.SampleItem> sampleItems = form.getSampleItems();

            for (SampleItemAliquotForm.SampleItem sampleItem : sampleItems) {
                String sampleItemExternalId = sampleItem.getExternalId();
                List<SampleItem> dbSampleItems = sampleItemService.getSampleItemsByExternalID(sampleItemExternalId);
                SampleItem lastSampleItem = dbSampleItems.isEmpty() ? null : dbSampleItems.getLast();

                if (lastSampleItem == null) {
                    response.put("errors", List.of("Sample item not found for external ID: " + sampleItemExternalId));
                    return response;
                }
                List<SampleItem> sampleItemsToInsert = new ArrayList<>();
                List<List<String>> analysisGroups = new ArrayList<>();

                List<SampleItemAliquotForm.Aliquot> aliquots = sampleItem.getAliquots();

                for (SampleItemAliquotForm.Aliquot aliquot : aliquots) {
                    SampleItem sampleItemToInsert = new SampleItem();
                    BeanUtils.copyProperties(sampleItemToInsert, lastSampleItem);

                    sampleItemToInsert.setId(null);
                    String aliquotExternalId = aliquot.getExternalId();
                    Double quantity = aliquot.getQuantity();
                    List<String> analysisIds = aliquot.getAnalyses();

                    analysisGroups.add(analysisIds);
                    sampleItemToInsert.setQuantity(quantity);
                    sampleItemToInsert.setExternalId(aliquotExternalId);
                    sampleItemToInsert.setFhirUuid(UUID.randomUUID());
                    sampleItemsToInsert.add(sampleItemToInsert);

                }
                lastSampleItem.setVoided(true);
                lastSampleItem.setVoidReason("Aliquoted into new sample items");

                sampleItemService.insertAliquots(lastSampleItem, sampleItemsToInsert, analysisGroups);

            }

            response.put("success", List.of("Aliquoting completed successfully"));
            return response;

        } catch (Exception e) {
            response.put("errors", List.of("Error processing aliquoting: " + e.getMessage()));
            return response;
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "redirect:/Aliquot";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/Aliquot";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "Aliquot";
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
