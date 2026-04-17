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
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NoteBookService extends BaseObjectService<NoteBook, Integer> {

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_MANAGE')")
    List<NoteBook> filterNoteBookEntries(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate, Integer noteBookId);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_MANAGE')")
    List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags, Date fromDate,
            Date toDate);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    List<NoteBook> getAllTemplateNoteBooks();

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    List<NoteBook> getNoteBookEntries(Integer templateId);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_MANAGE')")
    void updateWithStatus(Integer noteBookId, NoteBookStatus status, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_MANAGE')")
    NoteBook createWithFormValues(NoteBookForm form);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_MANAGE')")
    void updateWithFormValues(Integer noteBookId, NoteBookForm form);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    NoteBookDisplayBean convertToDisplayBean(Integer noteBookId);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    NoteBookFullDisplayBean convertToFullDisplayBean(Integer noteBookId);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    Long getCountWithStatus(List<NoteBookStatus> statuses);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    Long getCountWithStatusBetweenDates(List<NoteBookStatus> statuses, Timestamp from, Timestamp to);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    Long getTotalCount();

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    List<SampleDisplayBean> searchSampleItems(String accession);

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    List<NoteBook> getAllActiveNotebooks();

    @PreAuthorize("hasAuthority('PRIV_NOTEBOOK_VIEW')")
    SampleDisplayBean convertSampleToDisplayBean(SampleItem sampleItem);
}
