package org.openelisglobal.resultvalidation.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.notification.service.TestNotificationService;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.bean.ValidationFilterCriteria;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultValidationServiceImpl implements ResultValidationService {

    private AnalysisService analysisService;
    private ResultService resultService;
    private NoteService noteService;
    private SampleService sampleService;
    private TestNotificationService testNotificationService;

    public ResultValidationServiceImpl(AnalysisService analysisService, ResultService resultService,
            NoteService noteService, SampleService sampleService, TestNotificationService testNotificationService) {
        this.analysisService = analysisService;
        this.resultService = resultService;
        this.noteService = noteService;
        this.sampleService = sampleService;
        this.testNotificationService = testNotificationService;
    }

    @Override
    @Transactional
    public void persistdata(List<Result> deletableList, List<Analysis> analysisUpdateList,
            ArrayList<Result> resultUpdateList, List<AnalysisItem> resultItemList, ArrayList<Sample> sampleUpdateList,
            ArrayList<Note> noteUpdateList, IResultSaveService resultSaveService, List<IResultUpdate> updaters,
            String sysUserId) {
        ResultSaveService.removeDeletedResultsInTransaction(deletableList, sysUserId);

        // update analysis
        for (Analysis analysis : analysisUpdateList) {
            analysisService.update(analysis);
        }

        for (Result resultUpdate : resultUpdateList) {
            if (resultUpdate.getId() != null) {
                resultService.update(resultUpdate);
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "persistdata",
                        "validating a result that doesn't exist yet. Creating result.");
                String id = resultService.insert(resultUpdate);
                LogEvent.logWarn(this.getClass().getSimpleName(), "persistdata",
                        "Result with id: " + id + " created while validating");
            }
            if (isResultAnalysisFinalized(resultUpdate, analysisUpdateList)) {
                try {
                    testNotificationService.createAndSendNotificationsToConfiguredSources(
                            NotificationNature.RESULT_VALIDATION, resultUpdate);
                } catch (RuntimeException e) {
                    LogEvent.logError(e);
                }
            }
        }

        checkIfSamplesFinished(resultItemList, sampleUpdateList, sysUserId);

        for (Sample sample : sampleUpdateList) {
            sample.setSysUserId(sysUserId);
            sampleService.update(sample);
        }

        for (Note note : noteUpdateList) {
            if (note != null) {
                if (note.getId() == null) {
                    if (!noteService.duplicateNoteExists(note)) {
                        noteService.insert(note);
                    }
                } else {
                    noteService.update(note);
                }
            }
        }

        for (IResultUpdate updater : updaters) {
            updater.transactionalUpdate(resultSaveService);
        }
    }

    private boolean isResultAnalysisFinalized(Result result, List<Analysis> analysisUpdateList) {
        String analysisId = result.getAnalysis().getId();
        for (Analysis analysis : analysisUpdateList) {
            if (analysis.getId().equals(analysisId)) {
                return analysis.getStatusId()
                        .equals(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
            }
        }
        return false;
    }

    private void checkIfSamplesFinished(List<AnalysisItem> resultItemList, List<Sample> sampleUpdateList,
            String sysUserId) {
        String currentSampleId = "";
        boolean sampleFinished = true;
        List<Integer> sampleFinishedStatus = getSampleFinishedStatuses();

        for (AnalysisItem analysisItem : resultItemList) {
            Sample analysisSample = sampleService.getSampleByAccessionNumber(analysisItem.getAccessionNumber());
            if (analysisSample == null) {
                continue;
            }
            String analysisSampleId = analysisSample.getId();
            if (!analysisSampleId.equals(currentSampleId)) {

                currentSampleId = analysisSampleId;

                List<Analysis> analysisList = analysisService.getAnalysesBySampleId(currentSampleId);

                for (Analysis analysis : analysisList) {
                    if (!sampleFinishedStatus.contains(Integer.parseInt(analysis.getStatusId()))) {
                        sampleFinished = false;
                        break;
                    }
                }

                if (sampleFinished) {
                    Sample sample = sampleService.get(currentSampleId);
                    sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));
                    sample.setSysUserId(sysUserId);
                    sampleUpdateList.add(sample);
                }

                sampleFinished = true;
            }
        }
    }

    private List<Integer> getSampleFinishedStatuses() {
        ArrayList<Integer> sampleFinishedStatus = new ArrayList<>();
        sampleFinishedStatus.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)));
        sampleFinishedStatus.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
        sampleFinishedStatus.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
        return sampleFinishedStatus;
    }

    @Override
    public List<AnalysisItem> applyFilters(List<AnalysisItem> resultList, ValidationFilterCriteria criteria) {
        List<AnalysisItem> filtered = new ArrayList<>(resultList);

        filtered = applyLabNumberFilter(filtered, criteria.getLabNumberFrom(), criteria.getLabNumberTo());
        filtered = applyDateFilter(filtered, criteria.getDateFrom(), criteria.getDateTo());
        filtered = applyAnalyzerFilter(filtered, criteria.getAnalyzer());
        filtered = applyEnteredByFilter(filtered, criteria.getEnteredBy());
        filtered = applyNormalFilter(filtered, criteria.getNormal());
        filtered = applyFlaggedFilter(filtered, criteria.getFlagged());

        return filtered;
    }

    private List<AnalysisItem> applyLabNumberFilter(List<AnalysisItem> items, String from, String to) {
        if (StringUtils.isBlank(from) && StringUtils.isBlank(to)) {
            return items;
        }
        return items.stream().filter(item -> {
            String accessionNumber = item.getAccessionNumber();
            if (StringUtils.isNotBlank(from) && accessionNumber.compareTo(from) < 0) {
                return false;
            }
            if (StringUtils.isNotBlank(to) && accessionNumber.compareTo(to) > 0) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private List<AnalysisItem> applyDateFilter(List<AnalysisItem> items, String dateFrom, String dateTo) {
        if (StringUtils.isBlank(dateFrom) && StringUtils.isBlank(dateTo)) {
            return items;
        }
        try {
            Date fromDate = StringUtils.isNotBlank(dateFrom) ? DateUtil.convertStringDateToSqlDate(dateFrom) : null;
            Date toDate = StringUtils.isNotBlank(dateTo) ? DateUtil.convertStringDateToSqlDate(dateTo) : null;
            return items.stream().filter(item -> {
                if (item.getTestDate() == null) {
                    return false;
                }
                Date testDate = DateUtil.convertStringDateToSqlDate(item.getTestDate());
                if (fromDate != null && testDate.before(fromDate)) {
                    return false;
                }
                if (toDate != null && testDate.after(toDate)) {
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "applyDateFilter",
                    "Error parsing date filters: " + e.getMessage());
            return items;
        }
    }

    private List<AnalysisItem> applyAnalyzerFilter(List<AnalysisItem> items, String analyzer) {
        if (StringUtils.isBlank(analyzer)) {
            return items;
        }
        return items.stream().filter(item -> analyzer.equals(item.getAnalyzer())).collect(Collectors.toList());
    }

    private List<AnalysisItem> applyEnteredByFilter(List<AnalysisItem> items, String enteredBy) {
        if (StringUtils.isBlank(enteredBy)) {
            return items;
        }
        return items.stream().filter(item -> {
            if (item.getEnteredByObject() != null) {
                return enteredBy.equals(item.getEnteredByObject().getName());
            }
            return false;
        }).collect(Collectors.toList());
    }

    private List<AnalysisItem> applyNormalFilter(List<AnalysisItem> items, String normalParam) {
        if (StringUtils.isBlank(normalParam)) {
            return items;
        }
        boolean isNormal = Boolean.parseBoolean(normalParam);
        return items.stream().filter(item -> item.isNormal() == isNormal).collect(Collectors.toList());
    }

    private List<AnalysisItem> applyFlaggedFilter(List<AnalysisItem> items, String flaggedParam) {
        if (StringUtils.isBlank(flaggedParam) || !Boolean.parseBoolean(flaggedParam)) {
            return items;
        }
        return items.stream().filter(item -> item.getFlags() != null && !item.getFlags().isEmpty())
                .collect(Collectors.toList());
    }
}
