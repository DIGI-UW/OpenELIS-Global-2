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
package org.openelisglobal.reports.action.implementation;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfCopyFields;
import com.itextpdf.text.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openelisglobal.reports.form.ReportForm;

public abstract class CollectionReport implements IReportCreator {
    protected String requestedReport;
    protected String systemUserId;

    @Override
    public void setRequestedReport(String report) {
        requestedReport = report;
    }

    @Override
    public void setSystemUserId(String id) {
        systemUserId = id;
    };

    @Override
    public String getContentType() {
        return "application/pdf; charset=UTF-8";
    }

    protected String reportPath;
    protected ReportForm form;
    protected Set<String> handledOrders;

    @Override
    public String getResponseHeaderName() {
        return null;
    }

    @Override
    public String getResponseHeaderContent() {
        return null;
    }

    @Override
    public void initializeReport(ReportForm form) {
        handledOrders = new HashSet<>();
        this.form = form;
    }

    @Override
    public HashMap<String, ?> getReportParameters() throws IllegalStateException {
        return new HashMap<String, String>();
    }

    @Override
    public byte[] runReport() throws JRException, DocumentException {
        List<byte[]> byteList = generateReports();
        if (byteList.isEmpty()) {
            Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("SUBREPORT_DIR", reportPath);
            parameterMap.put("directorName",
                    ConfigurationProperties.getInstance().getPropertyValue(Property.labDirectorName));
            List<ErrorMessages> errorMsgs = new ArrayList<>();
            ErrorMessages msgs = new ErrorMessages();
            msgs.setMsgLine1(MessageUtil.getMessage("report.error.message.noPrintableItems"));
            errorMsgs.add(msgs);
            return JasperRunManager.runReportToPdf(reportPath + "NoticeOfReportError.jasper", parameterMap,
                    new JRBeanCollectionDataSource(errorMsgs));
        } else {
            return merge(byteList);
        }
    }

    @Override
    public void setReportPath(String path) {
        reportPath = path;
    }

    protected byte[] merge(List<byte[]> byteList) throws DocumentException {
        byte[] outputBytes;
        OutputStream outputStream = new ByteArrayOutputStream();

        try {

            PdfCopyFields pcf = new PdfCopyFields(outputStream);
            for (byte[] bytes : byteList) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                PdfReader pdfReader = new PdfReader(inputStream);

                // place holder in case we do have to rotate
                // if (false) {
                // int n = pdfReader.getNumberOfPages();
                // int rot;
                // PdfDictionary pageDict;
                // for (int i = 1; i <= n; i++) {
                // rot = pdfReader.getPageRotation(i);
                // pageDict = pdfReader.getPageN(i);
                // pageDict.put(PdfName.ROTATE, new PdfNumber(rot + 90));
                // }
                // }
                pcf.addDocument(pdfReader);
            }

            if (!byteList.isEmpty()) {
                pcf.close();
            }

            outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            return outputBytes;
        } catch (IOException e) {
            LogEvent.logDebug(e);
        } catch (DocumentException e) {
            LogEvent.logDebug(e);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                LogEvent.logDebug(e);
            }
        }
        return null;
    }

    protected byte[] createReport(String reportName) {
        IReportCreator reportCreator = ReportImplementationFactory.getReportCreator(reportName);

        if (reportCreator != null) {
            reportCreator.setRequestedReport(requestedReport);
            reportCreator.initializeReport(form);
            reportCreator.setReportPath(reportPath);

            @SuppressWarnings("unchecked")
            HashMap<String, String> parameterMap = (HashMap<String, String>) reportCreator.getReportParameters();
            parameterMap.put("SUBREPORT_DIR", reportPath);
            handledOrders.addAll(reportCreator.getReportedOrders());
            try {
                return reportCreator.runReport();
            } catch (IOException | SQLException | JRException | DocumentException | ParseException e) {
                LogEvent.logDebug(e);
            }
        }

        return null;
    }

    protected Patient getPatient() {
        String patientId = form.getPatientNumberDirect();
        return PatientUtil.getPatientByIdentificationNumber(patientId);
    }

    @Override
    public List<String> getReportedOrders() {
        List<String> orderList = new ArrayList<>();
        orderList.addAll(handledOrders);
        return orderList;
    }

    protected abstract List<byte[]> generateReports();
}
