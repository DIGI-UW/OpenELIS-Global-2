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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.referral.service.ReferringTestResultService;
import org.openelisglobal.referral.valueholder.ReferringTestResult;
import org.openelisglobal.reports.action.implementation.reportBeans.ConfirmationData;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

public class ConfirmationReport extends IndicatorReport implements IReportCreator, IReportParameterSetter {

    private List<ConfirmationData> reportItems;

    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private SampleItemService sampleItemService = SpringContext.getBean(SampleItemService.class);
    private SampleRequesterService requesterService = SpringContext.getBean(SampleRequesterService.class);
    private PersonService personService = SpringContext.getBean(PersonService.class);
    private ResultService resultService = SpringContext.getBean(ResultService.class);
    private DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    private OrganizationService organizationService = SpringContext.getBean(OrganizationService.class);
    private ReferringTestResultService referringTestResultService = SpringContext
            .getBean(ReferringTestResultService.class);
    private SampleService sampleService = SpringContext.getBean(SampleService.class);
    private RequesterTypeService requesterTypeService = SpringContext.getBean(RequesterTypeService.class);

    private long PERSON_REQUESTER_TYPE_ID;
    private long ORG_REQUESTER_TYPE_ID;

    public ConfirmationReport() {
        PERSON_REQUESTER_TYPE_ID = Long.parseLong(requesterTypeService.getRequesterTypeByName("provider").getId());
        ORG_REQUESTER_TYPE_ID = Long.parseLong(requesterTypeService.getRequesterTypeByName("organization").getId());
    }

    @Override
    protected byte[] renderReport() {
        return render(reportItems);
    }

    /** The confirmations under the shared header, grouped by requesting site. */
    byte[] render(List<ConfirmationData> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = startPdf(out, lowerDateRange + " - " + upperDateRange);
        String site = null;
        for (ConfirmationData item : items) {
            if (!Objects.equals(site, item.getOrganizationName())) {
                site = item.getOrganizationName();
                Paragraph siteLine = new Paragraph(MessageUtil.getMessage("report.site") + ": " + site, LABEL_FONT);
                siteLine.setSpacingBefore(10);
                document.add(siteLine);
            }
            PdfPTable order = new PdfPTable(4);
            order.setWidthPercentage(100);
            order.setSpacingBefore(6);
            for (String key : new String[] { "report.requestOrderNumber", "report.confirmationOrderNumber",
                    "report.sampleType", "report.reception" }) {
                order.addCell(plain(MessageUtil.getMessage(key), LABEL_FONT));
            }
            for (String value : new String[] { item.getRequesterAccession(), item.getLabAccession(),
                    item.getSampleType(), item.getReceptionDate() }) {
                order.addCell(plain(value, CELL_FONT));
            }
            PdfPCell contact = plain(
                    MessageUtil.getMessage("report.requesterContact") + ": " + nullToEmpty(item.getRequesterName()),
                    CELL_FONT);
            contact.setColspan(4);
            order.addCell(contact);
            PdfPCell reach = plain(MessageUtil.getMessage("report.telephoneAbv") + ": "
                    + nullToEmpty(item.getRequesterPhone()) + "    " + MessageUtil.getMessage("report.fax") + ": "
                    + nullToEmpty(item.getRequesterFax()) + "    " + MessageUtil.getMessage("report.email") + ": "
                    + nullToEmpty(item.getRequesterEMail()), CELL_FONT);
            reach.setColspan(4);
            order.addCell(reach);
            document.add(order);

            PdfPTable results = new PdfPTable(new float[] { 155, 120, 120, 123 });
            results.setWidthPercentage(100);
            PdfExportSupport.addHeaderRow(results, HEADER_FONT, 3, "", MessageUtil.getMessage("report.test"),
                    MessageUtil.getMessage("report.result"), MessageUtil.getMessage("report.completionDate"));
            for (int i = 0; i < item.getRequesterTest().size(); i++) {
                String result = item.getRequesterResult().get(i);
                addResultRow(results, i == 0 ? MessageUtil.getMessage("report.initialResults") : "",
                        item.getRequesterTest().get(i), "".equals(result) ? "No data" : result, "");
            }
            for (int i = 0; i < item.getLabTest().size(); i++) {
                String result = item.getLabResult().get(i);
                addResultRow(results, i == 0 ? MessageUtil.getMessage("report.confirmationResult") : "",
                        item.getLabTest().get(i),
                        "".equals(result) ? MessageUtil.getMessage("report.test.status.inProgress") : result,
                        item.getCompleationDate().get(i));
            }
            document.add(results);
            document.add(new Paragraph(MessageUtil.getMessage("report.note") + ": " + nullToEmpty(item.getNote()),
                    CELL_FONT));
        }
        document.close();
        return out.toByteArray();
    }

    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9);

    private static void addResultRow(PdfPTable table, String label, String test, String result, String date) {
        for (String value : new String[] { label, test, result, date }) {
            table.addCell(new Phrase(nullToEmpty(value), CELL_FONT));
        }
    }

    private static PdfPCell plain(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(nullToEmpty(text), font));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        setDateRange(form);

        setConfirmationData();
    }

    private void setConfirmationData() {
        reportItems = new ArrayList<>();

        List<Sample> referredSamples = getReferredInSamples();

        if (referredSamples.isEmpty()) {
            errorFound = true;
            ErrorMessages msgs = new ErrorMessages();
            msgs.setMsgLine1(MessageUtil.getMessage("report.error.message.noPrintableItems"));
            errorMsgs.add(msgs);
            return;
        }

        for (Sample sample : referredSamples) {
            reportItems.addAll(createConfirmationBeanFromSample(sample));
        }

        Collections.sort(reportItems, new Comparator<ConfirmationData>() {

            @Override
            public int compare(ConfirmationData o1, ConfirmationData o2) {
                int orgCompare = o1.getOrganizationName().compareTo(o2.getOrganizationName());
                if (orgCompare == 0) {
                    return o1.getLabAccession().compareTo(o2.getLabAccession());
                } else {
                    return orgCompare;
                }
            }
        });
    }

    private List<Sample> getReferredInSamples() {
        return sampleService.getConfirmationSamplesReceivedInDateRange(lowDate, highDate);
    }

    private List<ConfirmationData> createConfirmationBeanFromSample(Sample sample) {

        List<ConfirmationData> dataList = new ArrayList<>();

        String accessionNumber = sample.getAccessionNumber();
        String orgName = getOrganizationNameForSample(sample);
        Person requester = getRequesterForSample(sample);
        String requestDate = DateUtil.convertSqlDateToStringDate(sample.getReceivedDate());

        List<SampleItem> sampleItemList = sampleItemService.getSampleItemsBySampleId(sample.getId());

        for (SampleItem sampleItem : sampleItemList) {
            ConfirmationData data = new ConfirmationData();

            data.setLabAccession(accessionNumber + "-" + sampleItem.getSortOrder());
            data.setSampleType(SpringContext.getBean(TypeOfSampleService.class)
                    .getTypeOfSampleNameForId(sampleItem.getTypeOfSampleId()));
            data.setOrganizationName(StringUtil.replaceNullWithEmptyString(orgName));
            data.setRequesterAccession(sampleItem.getExternalId());
            data.setNote(getNoteForSampleItem(sampleItem));
            data.setRequesterEMail(StringUtil.replaceNullWithEmptyString(requester.getEmail()));
            data.setRequesterFax(StringUtil.replaceNullWithEmptyString(requester.getFax()));
            data.setRequesterName(StringUtil.replaceNullWithEmptyString(requester.getFirstName()) + " "
                    + StringUtil.replaceNullWithEmptyString(requester.getLastName()));
            data.setRequesterPhone(StringUtil.replaceNullWithEmptyString(requester.getWorkPhone()));
            data.setReceptionDate(requestDate);
            addResults(data, sampleItem);
            dataList.add(data);
        }

        return dataList;
    }

    private Person getRequesterForSample(Sample sample) {
        List<SampleRequester> requesters = requesterService.getRequestersForSampleId(sample.getId());

        for (SampleRequester requester : requesters) {
            if (PERSON_REQUESTER_TYPE_ID == requester.getRequesterTypeId()) {
                return personService.getPersonById(String.valueOf(requester.getRequesterId()));
            }
        }

        return new Person();
    }

    private String getOrganizationNameForSample(Sample sample) {
        List<SampleRequester> requesters = requesterService.getRequestersForSampleId(sample.getId());

        for (SampleRequester requester : requesters) {
            if (ORG_REQUESTER_TYPE_ID == requester.getRequesterTypeId()) {
                return organizationService.getOrganizationById(String.valueOf(requester.getRequesterId()))
                        .getOrganizationName();
            }
        }

        return "";
    }

    private String getNoteForSampleItem(SampleItem sampleItem) {
        NoteService noteService = SpringContext.getBean(NoteService.class);
        String notes = noteService.getNotesAsString(sampleItem, null, null);
        return notes == null ? "" : notes;
    }

    private void addResults(ConfirmationData data, SampleItem sampleItem) {
        List<Analysis> analysisList = analysisService.getAnalysesBySampleItem(sampleItem);

        List<String> requestTestList = new ArrayList<>();
        List<String> requestResultList = new ArrayList<>();
        List<String> labTestList = new ArrayList<>();
        List<String> labResultList = new ArrayList<>();
        List<String> completionDate = new ArrayList<>();

        for (Analysis analysis : analysisList) {
            labTestList.add(TestServiceImpl.getUserLocalizedTestName(analysis.getTest()));
            labResultList.add(getResultsForAnalysis(analysis));
            completionDate.add(getCompleationDate(analysis));
        }

        List<ReferringTestResult> referringTestResultList = referringTestResultService
                .getReferringTestResultsForSampleItem(sampleItem.getId());
        if (referringTestResultList.isEmpty()) {
            requestTestList.add(MessageUtil.getMessage("test.name.notSpecified"));
            requestResultList.add("");
        } else {
            for (ReferringTestResult result : referringTestResultList) {
                String name = result.getTestName();
                String resultValue = result.getResultValue();
                requestTestList.add(
                        GenericValidator.isBlankOrNull(name) ? MessageUtil.getMessage("test.name.notSpecified") : name);
                requestResultList.add(resultValue == null ? "" : resultValue);
            }
        }

        data.setLabResult(labResultList);
        data.setLabTest(labTestList);
        data.setRequesterTest(requestTestList);
        data.setRequesterResult(requestResultList);
        data.setCompleationDate(completionDate);
    }

    private String getResultsForAnalysis(Analysis analysis) {
        List<Result> results = resultService.getResultsByAnalysis(analysis);

        if (results != null && !results.isEmpty()) {
            String type = results.get(0).getResultType();

            if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(type)) {
                StringBuilder builder = new StringBuilder();
                boolean firstNumber = true;
                for (Result result : results) {
                    if (!firstNumber) {
                        builder.append(", ");
                    }
                    firstNumber = false;

                    if (!(GenericValidator.isBlankOrNull(result.getValue()) || "0".equals(result.getValue()))) {
                        builder.append(dictionaryService.getDictionaryById(result.getValue()).getLocalizedName());
                    }
                }

                return builder.toString();
            } else {
                return getResultsWithUOM(results.get(0).getValue(true), analysis);
            }
        }

        return "";
    }

    private String getResultsWithUOM(String value, Analysis analysis) {
        if (analysis.getTest().getUnitOfMeasure() != null) {
            return value + " " + analysis.getTest().getUnitOfMeasure().getUnitOfMeasureName();
        }

        return value;
    }

    private String getCompleationDate(Analysis analysis) {
        return DateUtil.convertTimestampToStringDate(analysis.getCompletedDate());
    }

    @Override
    protected String getNameForReportRequest() {
        return MessageUtil.getMessage("report.confirmation.request");
    }

    @Override
    protected String getNameForReport() {
        return MessageUtil.getContextualMessage("report.confirmation.title");
    }

    @Override
    protected String getLabNameLine1() {
        return ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName);
    }

    @Override
    protected String getLabNameLine2() {
        return "";
    }
}
