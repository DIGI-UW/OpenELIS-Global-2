package org.openelisglobal.common.management.controller.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.management.form.SampleTypeManagementForm;
import org.openelisglobal.common.management.valueholder.SampleTypeDisplay;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class SampleTypeManagementRestController extends BaseController {

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @RequestMapping(value = "/SampleTypeManagement", method = RequestMethod.GET)
    public SampleTypeManagementForm showSampleTypeManagement(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "N") String search,
            @RequestParam(required = false, defaultValue = "1") int startingRecNo,
            @RequestParam(required = false) String searchString,
            @RequestParam(required = false) String filter) {

        SampleTypeManagementForm form = new SampleTypeManagementForm();

        try {
            // Get all human sample types (domain = "H")
            List<TypeOfSample> sampleTypes = typeOfSampleService.getTypesOfSampleForDomain("H");
            List<SampleTypeDisplay> displayList = new ArrayList<>();

            // Convert to display objects
            for (TypeOfSample sampleType : sampleTypes) {
                if (sampleType != null) {
                    SampleTypeDisplay display = new SampleTypeDisplay();
                    display.setId(sampleType.getId());
                    display.setDescription(sampleType.getDescription());
                    display.setLocalAbbreviation(sampleType.getLocalAbbreviation());
                    display.setSortOrder(String.valueOf(sampleType.getSortOrder()));
                    display.setActive(sampleType.isActive());
                    display.setDomain(sampleType.getDomain());

                    // Apply search filter
                    boolean matchesSearch = true;
                    if (searchString != null && !searchString.trim().isEmpty()) {
                        String searchTerm = searchString.toLowerCase().trim();
                        matchesSearch = (sampleType.getDescription() != null &&
                                        sampleType.getDescription().toLowerCase().contains(searchTerm)) ||
                                       (sampleType.getLocalAbbreviation() != null &&
                                        sampleType.getLocalAbbreviation().toLowerCase().contains(searchTerm));
                    }

                    // Apply active filter
                    boolean matchesActiveFilter = true;
                    if ("isActive".equals(filter)) {
                        matchesActiveFilter = sampleType.isActive();
                    }

                    if (matchesSearch && matchesActiveFilter) {
                        displayList.add(display);
                    }
                }
            }

            form.setMenuList(displayList);
            form.setTotalRecordCount(String.valueOf(displayList.size()));
            form.setFromRecordCount("1");
            form.setToRecordCount(String.valueOf(displayList.size()));
            form.setSearchString(searchString != null ? searchString : "");

        } catch (LIMSRuntimeException e) {
            // Handle error gracefully
            form.setMenuList(new ArrayList<>());
            form.setTotalRecordCount("0");
            form.setFromRecordCount("0");
            form.setToRecordCount("0");
        }

        return form;
    }

    @RequestMapping(value = "/SampleTypeManagement/deactivate", method = RequestMethod.POST)
    public ResponseEntity<?> deactivateSampleTypes(@RequestParam String IDS) {
        try {
            if (IDS != null && !IDS.trim().isEmpty()) {
                String[] ids = IDS.split(",");

                for (String id : ids) {
                    if (id != null && !id.trim().isEmpty()) {
                        TypeOfSample sampleType = typeOfSampleService.get(id.trim());
                        if (sampleType != null) {
                            sampleType.setIsActive(false);
                            sampleType.setLastupdated(DateUtil.getCurrentDateAsText());
                            typeOfSampleService.update(sampleType);
                        }
                    }
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(value = "/SampleTypeManagement/export", method = RequestMethod.GET)
    public ResponseEntity<byte[]> exportSampleTypes(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String searchString,
            @RequestParam(required = false) String filter) {

        try {
            // Get filtered data using the same logic as the main endpoint
            List<TypeOfSample> sampleTypes = typeOfSampleService.getTypesOfSampleForDomain("H");
            List<SampleTypeDisplay> displayList = new ArrayList<>();

            for (TypeOfSample sampleType : sampleTypes) {
                if (sampleType != null) {
                    // Apply search filter
                    boolean matchesSearch = true;
                    if (searchString != null && !searchString.trim().isEmpty()) {
                        String searchTerm = searchString.toLowerCase().trim();
                        matchesSearch = (sampleType.getDescription() != null &&
                                        sampleType.getDescription().toLowerCase().contains(searchTerm)) ||
                                       (sampleType.getLocalAbbreviation() != null &&
                                        sampleType.getLocalAbbreviation().toLowerCase().contains(searchTerm));
                    }

                    // Apply active filter
                    boolean matchesActiveFilter = true;
                    if ("isActive".equals(filter)) {
                        matchesActiveFilter = sampleType.isActive();
                    }

                    if (matchesSearch && matchesActiveFilter) {
                        SampleTypeDisplay display = new SampleTypeDisplay();
                        display.setId(sampleType.getId());
                        display.setDescription(sampleType.getDescription());
                        display.setLocalAbbreviation(sampleType.getLocalAbbreviation());
                        display.setSortOrder(String.valueOf(sampleType.getSortOrder()));
                        display.setActive(sampleType.isActive());
                        display.setDomain(sampleType.getDomain());
                        displayList.add(display);
                    }
                }
            }

            // Generate CSV
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                // Header
                csvPrinter.printRecord(Arrays.asList(
                    MessageUtil.getMessage("sample.type.description"),
                    MessageUtil.getMessage("sample.type.abbreviation"),
                    MessageUtil.getMessage("sample.type.sort.order"),
                    MessageUtil.getMessage("label.status")
                ));

                // Data rows
                for (SampleTypeDisplay display : displayList) {
                    csvPrinter.printRecord(Arrays.asList(
                        display.getDescription(),
                        display.getLocalAbbreviation(),
                        display.getSortOrder(),
                        display.isActive() ? "Active" : "Inactive"
                    ));
                }

                csvPrinter.flush();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"sample_types_export.csv\"");
            headers.add("Content-Type", "text/csv; charset=UTF-8");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(outputStream.toByteArray());

        } catch (IOException | LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleTypeManagementDefinition";
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
