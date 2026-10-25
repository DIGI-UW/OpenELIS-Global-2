package org.openelisglobal.analyzerresults.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.dao.AnalyzerResultsDAO;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.analyzerresults.valueholder.SampleGrouping;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.util.TestReflexBean;
import org.openelisglobal.testreflex.action.util.TestReflexUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerResultsServiceImpl extends AuditableBaseObjectServiceImpl<AnalyzerResults, String>
        implements AnalyzerResultsService {
    @Autowired
    protected AnalyzerResultsDAO baseObjectDAO;

    @Autowired
    private NoteService noteService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;

    AnalyzerResultsServiceImpl() {
        super(AnalyzerResults.class);
        this.auditTrailLog = true;
    }

    @Override
    protected AnalyzerResultsDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerResults> getResultsbyAnalyzer(String analyzerId) {
        return baseObjectDAO.getAllMatchingOrdered("analyzerId", analyzerId, "id", false);
    }

    @Override
    public AnalyzerResults readAnalyzerResults(String idString) {
        return getBaseObjectDAO().readAnalyzerResults(idString);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerResults> findHeldMappingResultsByAnalyzer(String analyzerId) {
        return getBaseObjectDAO().findHeldMappingResultsByAnalyzer(analyzerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countHeldResultsByAnalyzerIds(List<String> analyzerIds) {
        return getBaseObjectDAO().countHeldResultsByAnalyzerIds(analyzerIds);
    }

    /**
     * Stage each distinct delivery. The importer owns replay prevention through
     * durable receipts; similar results remain visible for explicit local review.
     * Held recovery updates its retained row by ID, never by value or timestamp.
     */
    @Override
    @Transactional
    public void insertAnalyzerResults(List<AnalyzerResults> results, String sysUserId) {
        try {
            for (AnalyzerResults result : results) {
                // A control material is reused across independent runs. Delivery receipts
                // reject retransmissions; patient duplicate review must not suppress QC.
                List<AnalyzerResults> previous = result.getIsControl() ? List.of()
                        : baseObjectDAO.getDuplicateResultByAccessionAndTest(result);
                AnalyzerResults prior = previous == null || previous.isEmpty() ? null : previous.get(0);
                if (prior != null) {
                    result.setDuplicateAnalyzerResultId(prior.getId());
                    result.setReadOnly(true);
                }
                result.setSysUserId(sysUserId);
                String id = insert(result);
                result.setId(id);
                if (prior != null) {
                    prior.setDuplicateAnalyzerResultId(id);
                    prior.setSysUserId(sysUserId);
                    update(prior);
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AnalyzerResult insertAnalyzerResult()", e);
        }
    }

    @Override
    @Transactional
    public void persistAnalyzerResults(List<AnalyzerResults> deletableAnalyzerResults,
            List<SampleGrouping> sampleGroupList, String sysUserId) {
        removeHandledResultsFromAnalyzerResults(deletableAnalyzerResults, sysUserId);

        insertResults(sampleGroupList, sysUserId);
    }

    private void removeHandledResultsFromAnalyzerResults(List<AnalyzerResults> deletableAnalyzerResults,
            String sysUserId) {
        for (AnalyzerResults currentAnalyzerResult : deletableAnalyzerResults) {
            delete(currentAnalyzerResult.getId(), sysUserId);
        }
    }

    private boolean insertResults(List<SampleGrouping> sampleGroupList, String sysUserId) {
        for (SampleGrouping grouping : sampleGroupList) {
            if (grouping.addSample) {
                // try {
                sampleService.insertDataWithAccessionNumber(grouping.sample);
                // } catch (LIMSRuntimeException e) {
                // Errors errors = new BaseErrors();
                // String errorMsg = "warning.duplicate.accession";
                // errors.reject(errorMsg, new String[] { grouping.sample.getAccessionNumber()
                // },
                // errorMsg);
                // saveErrors(errors);
                // return false;
                // }
            } else if (grouping.updateSample) {
                sampleService.update(grouping.sample);
            }

            String sampleId = grouping.sample.getId();

            if (grouping.addSample) {
                grouping.sampleHuman.setSampleId(sampleId);
                sampleHumanService.insert(grouping.sampleHuman);

                RecordStatus patientStatus = grouping.statusSet.getPatientRecordStatus() == null
                        ? RecordStatus.NotRegistered
                        : null;
                RecordStatus sampleStatus = grouping.statusSet.getSampleRecordStatus() == null
                        ? RecordStatus.NotRegistered
                        : null;
                SpringContext.getBean(IStatusService.class).persistRecordStatusForSample(grouping.sample, sampleStatus,
                        grouping.patient, patientStatus, sysUserId);
            }

            if (grouping.addSampleItem) {
                grouping.sampleItem.setSample(grouping.sample);
                sampleItemService.insert(grouping.sampleItem);
            }

            for (int i = 0; i < grouping.analysisList.size(); i++) {

                Analysis analysis = grouping.analysisList.get(i);
                if (GenericValidator.isBlankOrNull(analysis.getId())) {
                    analysis.setSampleItem(grouping.sampleItem);
                    analysisService.insert(analysis);
                } else {
                    analysisService.update(analysis);
                }

                Result result = grouping.resultList.get(i);
                if (GenericValidator.isBlankOrNull(result.getId())) {
                    result.setAnalysis(analysis);
                    setAnalyte(result);
                    resultService.insert(result);
                } else {
                    resultService.update(result);
                }

                // The note was made for the analysis (its reference table), which has an id
                // only now.
                for (Note note : grouping.noteList.get(i)) {
                    note.setReferenceId(analysis.getId());
                    if (!alreadyOnAnalysis(note)) {
                        noteService.insert(note);
                    }
                }
            }
        }

        TestReflexUtil testReflexUtil = new TestReflexUtil();
        testReflexUtil.addNewTestsToDBForReflexTests(convertGroupListToTestReflexBeans(sampleGroupList), sysUserId);

        return true;
    }

    /**
     * An analysis carries a note once: each part of a result, and a rerun, may
     * bring the same one. Compared as the note service's duplicate rule compares
     * them.
     */
    private boolean alreadyOnAnalysis(Note note) {
        return noteService.getNotesByNoteTypeRefIdRefTable(note).stream()
                .anyMatch(existing -> sameNoteText(existing.getText(), note.getText())
                        && sameNoteText(existing.getSubject(), note.getSubject()));
    }

    private static boolean sameNoteText(String one, String other) {
        return Objects.equals(one == null ? null : one.trim().toLowerCase(),
                other == null ? null : other.trim().toLowerCase());
    }

    private List<TestReflexBean> convertGroupListToTestReflexBeans(List<SampleGrouping> sampleGroupList) {
        List<TestReflexBean> reflexBeanList = new ArrayList<>();

        for (SampleGrouping sampleGroup : sampleGroupList) {
            if (sampleGroup.accepted) {
                for (Result result : sampleGroup.resultList) {
                    TestReflexBean reflex = new TestReflexBean();
                    reflex.setPatient(sampleGroup.patient);
                    reflex.setTriggersToSelectedReflexesMap(sampleGroup.triggersToSelectedReflexesMap);
                    reflex.setResult(result);
                    reflex.setSample(sampleGroup.sample);
                    reflexBeanList.add(reflex);
                }
            }
        }

        return reflexBeanList;
    }

    private void setAnalyte(Result result) {
        TestAnalyte testAnalyte = ResultUtil.getTestAnalyteForResult(result);

        if (testAnalyte != null) {
            result.setAnalyte(testAnalyte.getAnalyte());
        }
    }
}
