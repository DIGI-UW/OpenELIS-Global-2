package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;
import org.openelisglobal.microbiology.form.MicroWorklistRowForm;

public interface MicroWorklistService {

    List<MicroWorklistRowForm> getWorklistRows();

    MicroWorklistPageForm getWorklistPage(MicroWorklistQueryForm query);
}
