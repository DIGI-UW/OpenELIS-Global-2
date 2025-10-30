package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;

public interface NoteBookService extends BaseObjectService<NoteBook, Integer> {

    List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags, Date fromDate,
            Date toDate);

    void updateWithStatus(Integer noteBookId, NoteBookStatus status);

    void createWithFormValues(NoteBookForm form);

    void updateWithFormValues(Integer noteBookId, NoteBookForm form);

    NoteBookDisplayBean convertToDisplayBean(Integer noteBookId);

    NoteBookFullDisplayBean convertToFullDisplayBean(Integer noteBookId);

    Long getCountWithStatus(List<NoteBookStatus> statuses);

    Long getCountWithStatusBetweenDates(List<NoteBookStatus> statuses, Timestamp from, Timestamp to);

    Long getTotalCount();

    List<SampleDisplayBean> searchSampleItems(String patientId, String accession);
}
