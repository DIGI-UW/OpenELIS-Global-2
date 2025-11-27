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

    List<NoteBook> filterNoteBookEntries(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate, Integer noteBookId);

    List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags, Date fromDate,
            Date toDate);

    List<NoteBook> getAllTemplateNoteBooks();

    List<NoteBook> getNoteBookEntries(Integer templateId);

    void updateWithStatus(Integer noteBookId, NoteBookStatus status, String sysUserId);

    NoteBook createWithFormValues(NoteBookForm form);

    void updateWithFormValues(Integer noteBookId, NoteBookForm form);

    NoteBookDisplayBean convertToDisplayBean(Integer noteBookId);

    NoteBookFullDisplayBean convertToFullDisplayBean(Integer noteBookId);

    Long getCountWithStatus(List<NoteBookStatus> statuses);

    Long getCountWithStatusBetweenDates(List<NoteBookStatus> statuses, Timestamp from, Timestamp to);

    Long getTotalCount();

    List<SampleDisplayBean> searchSampleItems(String accession);

    List<NoteBook> getAllActiveNotebooks();
}
