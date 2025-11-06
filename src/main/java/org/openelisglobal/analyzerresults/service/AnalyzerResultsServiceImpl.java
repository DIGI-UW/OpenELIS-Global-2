package org.openelisglobal.analyzerresults.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.dao.AnalyzerResultsDAO;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.controller.AnalyzerResultsController.SampleGrouping;
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
    }

    @Override
    protected AnalyzerResultsDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerResults> getResultsbyAnalyzer(String analyzerId) {
        return baseObjectDAO.getAllMatchingOrdered("analyzerId", Integer.parseInt(analyzerId), "id", false);
    }

    @Override
    public AnalyzerResults readAnalyzerResults(String idString) {
        return getBaseObjectDAO().readAnalyzerResults(idString);
    }

    @Override
    public void insertAnalyzerResults(List<AnalyzerResults> results, String sysUserId) {
        try {
            for (AnalyzerResults result : results) {
                boolean duplicateByAccessionAndTestOnly = false;
                List<AnalyzerResults> previousResults = baseObjectDAO.getDuplicateResultByAccessionAndTest(result);
                AnalyzerResults previousResult = null;

                // This next block may seem more complicated then it need be but it covers the
                // case where there may be a third duplicate
                // and it covers rereading the same file
                if (previousResults != null) {
                    duplicateByAccessionAndTestOnly = true;
                    for (AnalyzerResults foundResult : previousResults) {
                        previousResult = foundResult;
                        if (foundResult.getCompleteDate() != null
                                && foundResult.getCompleteDate().equals(result.getCompleteDate())) {
                            duplicateByAccessionAndTestOnly = false;
                            break;
                        }
                    }
                }

                if (duplicateByAccessionAndTestOnly && previousResult != null) {
                    result.setDuplicateAnalyzerResultId(previousResult.getId());
                    result.setReadOnly(true);
                }

                if (previousResults == null || duplicateByAccessionAndTestOnly) {
                    result.setSysUserId(sysUserId);
                    String id = insert(result);
                    result.setId(id);

                    if (duplicateByAccessionAndTestOnly && previousResult != null) {
                        previousResult.setDuplicateAnalyzerResultId(id);
                        previousResult.setSysUserId(sysUserId);
                    }

                    if (duplicateByAccessionAndTestOnly) {
                        update(previousResult);
                    }
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

                Note note = grouping.noteList.get(i);

                if (note != null) {
                    note.setReferenceId(result.getId());
                    noteService.insert(note);
                }
            }
        }

        TestReflexUtil testReflexUtil = new TestReflexUtil();
        testReflexUtil.addNewTestsToDBForReflexTests(convertGroupListToTestReflexBeans(sampleGroupList), sysUserId);

        return true;
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
