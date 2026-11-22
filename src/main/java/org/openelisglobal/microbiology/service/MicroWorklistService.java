package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;

public interface MicroWorklistService {

    MicroWorklistPageForm getWorklistPage(MicroWorklistQueryForm query);
}
