package org.openelisglobal.fhir.service;

import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.sample.valueholder.Sample;

public interface TaskTransformService {

    Task transformToTask(Sample sample);

}
