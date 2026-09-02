package org.openelisglobal.vector.service;

import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.vector.valueholder.VectorPool;

public interface VectorPoolLabelService {

    int MAX_LABEL_LENGTH = 64;
    int MAX_DECON_DEPTH = 4;

    String intakeLotLabel(String accession, int oneBasedPosition);

    String intakeLotBase(VectorPool intakePool, Sample sample);

    String subPoolLabel(String parentBase, int position);
}
