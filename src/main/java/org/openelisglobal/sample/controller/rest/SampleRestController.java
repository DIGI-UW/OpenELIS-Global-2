package org.openelisglobal.sample.controller.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.sample.form.SampleSearchForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
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

	@Autowired
	private AnalysisService analysisService;

	@Autowired
	private IStatusService statusService;

	/**
	 * Get sample by accession number
	 *
	 * @param accessionNumber Sample accession number
	 * @return Sample information or 404 if not found
	 */
	@GetMapping("/by-accession/{accessionNumber}")
	public ResponseEntity<List<SampleSearchForm>> getSampleByAccessionNumber(@PathVariable String accessionNumber) {
		try {
			Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);

			if (sample == null) {
				return ResponseEntity.notFound().build();
			}

			List<SampleSearchForm> forms = convertToForm(sample);
			return ResponseEntity.ok(forms);
		} catch (Exception e) {
			LogEvent.logError(e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Convert Sample entity to SampleSearchForm
	 */
	private List<SampleSearchForm> convertToForm(Sample sample) {
		List<SampleSearchForm> forms = new ArrayList<SampleSearchForm>();

		Set<Integer> statusList = new HashSet<>();
		statusList.add(Integer.parseInt(statusService.getStatusID(AnalysisStatus.NotStarted)));

		List<Analysis> analyses = analysisService.getAnalysesBySampleIdAndStatusId(sample.getId(), statusList);

		analyses.stream().forEach(analysis -> {
			SampleSearchForm form = new SampleSearchForm();
			form.setId(Integer.parseInt(sample.getId()));
			form.setAccessionNumber(sample.getAccessionNumber());
			form.setAnalysisId(Integer.parseInt(analysis.getId()));
			TypeOfSample typeOfSample = analysis.getSampleItem().getTypeOfSample();
			if (typeOfSample != null) {
				form.setSampleType(typeOfSample.getDescription());
			}
			// Assuming referralTest is derived from analysis test name
			if (analysis.getTest() != null) {
				form.setReferralTest(analysis.getTest().getName());
			}
			forms.add(form);
		});

		return forms;
	}
}
