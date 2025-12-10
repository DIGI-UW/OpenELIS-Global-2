package org.openelisglobal.sample.controller.rest;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sample.form.SampleSearchForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for sample operations
 */
@RestController
@RequestMapping("/rest/sample")
public class SampleRestController extends BaseRestController {

    @Autowired
    private SampleService sampleService;

    /**
     * Get sample by accession number
     *
     * @param accessionNumber Sample accession number
     * @return Sample information or 404 if not found
     */
    @GetMapping("/by-accession/{accessionNumber}")
    public ResponseEntity<SampleSearchForm> getSampleByAccessionNumber(@PathVariable String accessionNumber) {
        try {
            Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);

            if (sample == null) {
                return ResponseEntity.notFound().build();
            }

            SampleSearchForm form = convertToForm(sample);
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert Sample entity to SampleSearchForm
     */
    private SampleSearchForm convertToForm(Sample sample) {
        SampleSearchForm form = new SampleSearchForm();

        form.setId(Integer.parseInt(sample.getId()));
        form.setAccessionNumber(sample.getAccessionNumber());

        // Note: Sample type and referralTest information would need to be loaded from
        // SampleItem and Analysis/Test tables. For now, we'll leave them as null
        // and the frontend will display "-"

        return form;
    }
}
