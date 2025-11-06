/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.services.historyservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.service.SampleServiceImpl;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public class NoteHistoryService extends AbstractHistoryService {

    protected NoteService noteService = SpringContext.getBean(NoteService.class);
    protected HistoryService historyService = SpringContext.getBean(HistoryService.class);

    private Map<String, String> noteIdToIndicatorMap;

    public NoteHistoryService(Sample sample) {
        setUpForNotes(sample);
    }

    @SuppressWarnings("unchecked")
    private void setUpForNotes(Sample sample) {
        noteIdToIndicatorMap = new HashMap<>();

        historyList = new ArrayList<>();

        addAnalysisNotes(sample);
        addOrderNotes(sample);
        addQANotes(sample);
        newValueMap = new HashMap<>();
    }

    private void addAnalysisNotes(Sample sample) {
        History searchHistory = new History();
        searchHistory.setReferenceTable(NoteServiceImpl.getTableReferenceId());
        SampleService sampleSampleService = SpringContext.getBean(SampleService.class);
        List<Analysis> analysisList = sampleSampleService.getAnalysis(sample);
        Note searchNote = new Note();
        searchNote.setReferenceTableId(AnalysisServiceImpl.getTableReferenceId());
        for (Analysis analysis : analysisList) {
            searchNote.setReferenceId(analysis.getId());

            List<Note> notes = noteService.getAllNotesByRefIdRefTable(searchNote);

            for (Note note : notes) {
                searchHistory.setReferenceId(note.getId());
                noteIdToIndicatorMap.put(note.getId(), analysis.getTest().getLocalizedName());
                historyList.addAll(historyService.getHistoryByRefIdAndRefTableId(searchHistory));
            }
        }
    }

    private void addOrderNotes(Sample sample) {
        History searchHistory = new History();
        searchHistory.setReferenceTable(NoteServiceImpl.getTableReferenceId());

        Note searchNote = new Note();
        searchNote.setReferenceTableId(SampleServiceImpl.getTableReferenceId());
        searchNote.setReferenceId(sample.getId());

        List<Note> notes = noteService.getAllNotesByRefIdRefTable(searchNote);

        for (Note note : notes) {
            searchHistory.setReferenceId(note.getId());
            noteIdToIndicatorMap.put(note.getId(), MessageUtil.getMessage("auditTrail.order"));
            historyList.addAll(historyService.getHistoryByRefIdAndRefTableId(searchHistory));
        }
    }

    private void addQANotes(Sample sample) {
        History searchHistory = new History();
        searchHistory.setReferenceTable(NoteServiceImpl.getTableReferenceId());

        Note searchNote = new Note();
        searchNote.setReferenceTableId(QAService.TABLE_REFERENCE_ID);

        SampleService sampleSampleService = SpringContext.getBean(SampleService.class);
        List<SampleQaEvent> qaEventList = sampleSampleService.getSampleQAEventList(sample);

        for (SampleQaEvent qaEvent : qaEventList) {
            searchNote.setReferenceId(qaEvent.getId());
            List<Note> notes = noteService.getAllNotesByRefIdRefTable(searchNote);

            for (Note note : notes) {
                searchHistory.setReferenceId(note.getId());
                noteIdToIndicatorMap.put(note.getId(), qaEvent.getQaEvent().getLocalizedName());
                historyList.addAll(historyService.getHistoryByRefIdAndRefTableId(searchHistory));
            }
        }
    }

    @Override
    protected void addInsertion(History history, List<AuditTrailItem> items) {
        Note note = noteService.getData(history.getReferenceId());
        identifier = noteIdToIndicatorMap.get(history.getReferenceId());
        AuditTrailItem audit = getCoreTrail(history);
        audit.setNewValue(note.getText());
        items.add(audit);
    }

    @Override
    protected void getObservableChanges(History history, Map<String, String> changeMap, String changes) {
    }

    @Override
    protected String getObjectName() {
        return MessageUtil.getMessage("note.note");
    }
}
