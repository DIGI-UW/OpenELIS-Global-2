package org.openelisglobal.resultvalidation.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.sample.valueholder.Sample;

public interface ResultValidationService {

    void persistdata(List<Result> deletableList, List<Analysis> analysisUpdateList, ArrayList<Result> resultUpdateList,
            List<AnalysisItem> resultItemList, ArrayList<Sample> sampleUpdateList, ArrayList<Note> noteUpdateList,
            IResultSaveService resultSaveService, List<IResultUpdate> updaters, String sysUserId);
}
