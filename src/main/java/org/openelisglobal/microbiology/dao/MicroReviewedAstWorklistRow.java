package org.openelisglobal.microbiology.dao;

import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;

public record MicroReviewedAstWorklistRow(MicroCase microCase, MicroIsolate isolate, MicroAstRun run) {
}
