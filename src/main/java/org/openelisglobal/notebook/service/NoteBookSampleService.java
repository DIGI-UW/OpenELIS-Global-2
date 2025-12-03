package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.valueholder.NoteBookSample;

public interface NoteBookSampleService extends BaseObjectService<NoteBookSample, Integer> {
    List<NoteBookSample> getNotebookSamplesBySampleItemId(Integer sampleItemId);

    List<SampleDisplayBean> getNotebookSamplesByNoteBookId(Integer noteBookId);
}
