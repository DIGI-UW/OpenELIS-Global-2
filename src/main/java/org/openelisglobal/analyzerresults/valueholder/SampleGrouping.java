package org.openelisglobal.analyzerresults.valueholder;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Groups the entities created when accepting a staged analyzer result into OE's
 * results system. One SampleGrouping per accession number.
 *
 * <p>
 * Previously an inner class of AnalyzerResultsController. Extracted for proper
 * layering — the service creates these, the service persists them.
 */
public class SampleGrouping {

    public boolean accepted = true;
    public Sample sample;
    public SampleHuman sampleHuman;
    public Patient patient;
    public List<Note> noteList;
    public SampleItem sampleItem;
    public List<Analysis> analysisList;
    public List<Result> resultList;
    public Map<String, List<String>> triggersToSelectedReflexesMap;
    public StatusSet statusSet;
    public boolean addSample = false;
    public boolean updateSample = false;
    public boolean addSampleItem = false;
    public Map<Result, String> resultToUserserSelectionMap;
}
