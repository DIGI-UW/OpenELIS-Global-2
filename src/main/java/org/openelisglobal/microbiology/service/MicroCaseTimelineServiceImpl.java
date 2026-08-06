package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseTimelineServiceImpl implements MicroCaseTimelineService {

    private static final String NOTE_SUBJECT_PREFIX = "MICROBIOLOGY_CASE:";

    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final NoteService noteService;
    private final ReferenceTablesService referenceTablesService;
    private final String configuredSampleItemTableId;

    public MicroCaseTimelineServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO, NoteService noteService,
            ReferenceTablesService referenceTablesService) {
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.noteService = noteService;
        this.referenceTablesService = referenceTablesService;
        this.configuredSampleItemTableId = null;
    }

    MicroCaseTimelineServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO, NoteService noteService,
            String sampleItemTableId) {
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.noteService = noteService;
        this.referenceTablesService = null;
        this.configuredSampleItemTableId = sampleItemTableId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseActivityForm> getTimeline(String caseId) {
        MicroCase microCase = requireCase(caseId);
        List<MicroCaseActivityForm> timeline = new ArrayList<>();
        for (MicroCaseActivity activity : activityDAO.getByCaseId(caseId)) {
            timeline.add(toForm(activity));
        }
        for (Note note : noteService.getNotesChronologicallyByRefIdAndRefTableAndType(microCase.getSampleItemId(),
                sampleItemTableId(), List.of(Note.INTERNAL))) {
            if (subject(caseId).equals(note.getSubject())) {
                timeline.add(toForm(note, caseId));
            }
        }
        timeline.sort(Comparator.comparing(form -> form.occurredAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return timeline;
    }

    @Override
    @Transactional
    public MicroCaseActivityForm addNote(String caseId, String text, String performedBy) {
        MicroCaseServiceImpl.requireText(text, "text");
        MicroCaseServiceImpl.requireText(performedBy, "performedBy");
        MicroCase microCase = requireCase(caseId);
        Note note = noteService.createSavableNote(binding(microCase), NoteServiceImpl.NoteType.INTERNAL, text.trim(),
                subject(caseId), performedBy);
        if (note == null) {
            throw new IllegalArgumentException("MICROBIOLOGY_NOTE_TEXT_REQUIRED");
        }
        noteService.insert(note);
        return toForm(note, caseId);
    }

    private MicroCase requireCase(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
    }

    private NoteObject binding(MicroCase microCase) {
        return new NoteObject() {
            @Override
            public String getTableId() {
                return sampleItemTableId();
            }

            @Override
            public String getObjectId() {
                return microCase.getSampleItemId();
            }

            @Override
            public NoteServiceImpl.BoundTo getBoundTo() {
                return NoteServiceImpl.BoundTo.SAMPLE_ITEM;
            }
        };
    }

    private MicroCaseActivityForm toForm(MicroCaseActivity activity) {
        MicroCaseActivityForm form = new MicroCaseActivityForm();
        form.id = activity.getId();
        form.caseId = activity.getCaseId();
        form.activityType = activity.getActivityType();
        form.occurredAt = activity.getOccurredAt();
        form.performedBy = activity.getPerformedBy();
        form.note = activity.getNote();
        form.structuredData = activity.getStructuredData();
        return form;
    }

    private MicroCaseActivityForm toForm(Note note, String caseId) {
        MicroCaseActivityForm form = new MicroCaseActivityForm();
        form.id = "note-" + note.getId();
        form.caseId = caseId;
        form.activityType = MicroCaseActivityType.MANUAL_NOTE.name();
        form.occurredAt = note.getLastupdated();
        form.performedBy = note.getSystemUser() == null ? note.getSysUserId() : note.getSystemUser().getId();
        form.note = note.getText();
        return form;
    }

    private String subject(String caseId) {
        return NOTE_SUBJECT_PREFIX + caseId;
    }

    private String sampleItemTableId() {
        return configuredSampleItemTableId == null
                ? referenceTablesService.getReferenceTableByName("SAMPLE_ITEM").getId()
                : configuredSampleItemTableId;
    }
}
