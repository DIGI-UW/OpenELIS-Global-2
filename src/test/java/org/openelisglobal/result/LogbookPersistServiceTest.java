package org.openelisglobal.result;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.service.ResultInventoryService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;

public class LogbookPersistServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private LogbookResultsPersistService logbookPersistService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ResultSignatureService resultSigService;

    @Autowired
    private ResultInventoryService resultInventoryService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-inventory.xml");
    }

    @Test
    public void persistDataSet() {
        List<Result> results = resultService.getAllResults();
        List<Sample> samples = sampleService.getAll();

        assertFalse("Results list is empty", results.isEmpty());
        assertFalse("Samples list is empty", samples.isEmpty());

        SystemUser systemUser = systemUserService.get("1");
        ReferenceTables referenceTables = referenceTablesService.get("1");

        Note note = new Note();
        note.setSubject("Test Note");
        note.setReferenceId("100");
        note.setNoteType("N");
        note.setText("This is a test note.");
        note.setSystemUser(systemUser);
        note.setReferenceTables(referenceTables);

        List<ResultInventory> resultInventories = resultInventoryService.getAll();
        List<ResultSignature> resultSignatures = resultSigService.getAll();
        List<Analysis> analysises = analysisService.getAll();
        List<Patient> patients = patientService.getAllPatients();

        assertFalse("ResultInventories list is empty", resultInventories.isEmpty());
        assertFalse("ResultSignatures list is empty", resultSignatures.isEmpty());
        assertFalse("Analyses list is empty", analysises.isEmpty());
        assertFalse("Patients list is empty", patients.isEmpty());

        Map<String, List<String>> triggersToSelectedReflexesMap = new HashMap<>();
        triggersToSelectedReflexesMap.put("1", List.of("test_2"));

        ResultSet resultSet = new ResultSet(results.get(0), resultSignatures.get(0), resultInventories.get(0),
                patients.get(0), samples.get(0), triggersToSelectedReflexesMap, false);

        ResultsUpdateDataSet resultsUpdateDataSet = new ResultsUpdateDataSet("");
        resultsUpdateDataSet.setPreviousAnalysis(analysises.get(0));
        resultsUpdateDataSet.setModifiedResults(List.of(resultSet));
        resultsUpdateDataSet.addToNoteList(note);

        List<IResultUpdate> updaters = new ArrayList<>();

        List<Analysis> analyses = logbookPersistService.persistDataSet(resultsUpdateDataSet, updaters, "");

        analyses.forEach(analysis -> {
            System.out.println("Persisted Analysis ID: " + analysis.getId());
        });
    }
}
