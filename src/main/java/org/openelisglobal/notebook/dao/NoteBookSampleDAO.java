package org.openelisglobal.notebook.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NoteBookSample;

public interface NoteBookSampleDAO extends BaseDAO<NoteBookSample, Integer> {

    List<NoteBookSample> getNotebookSamplesBySampleItemId(Integer sampleItemId);

    List<NoteBookSample> getNotebookSamplesByNoteBookId(Integer noteBookId);
}
