package org.openelisglobal.fhir.service;

import java.util.List;
import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.result.valueholder.Result;

/**
 * OpenELIS Result to FHIR Observation.
 */
public interface ObservationTransformService {

    Observation transformResultToObservation(String resultId);

    Observation transformResultToObservation(Result result);

    Observation transformResultToObservation(Result result, List<Note> analysisNotes);
}
