package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.valueholder.NoteBookSample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NoteBookSampleService extends BaseObjectService<NoteBookSample, Integer> {
    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    List<NoteBookSample> getNotebookSamplesBySampleItemId(Integer sampleItemId);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    List<SampleDisplayBean> getNotebookSamplesByNoteBookId(Integer noteBookId);
}
