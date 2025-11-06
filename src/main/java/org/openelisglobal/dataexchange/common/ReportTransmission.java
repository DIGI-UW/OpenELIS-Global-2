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
package org.openelisglobal.dataexchange.common;

import ca.uhn.hl7v2.HL7Exception;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.resources.ResourceLocator;
import org.openelisglobal.dataexchange.orderresult.OrderResponseWorker;
import org.openelisglobal.dataexchange.resultreporting.beans.ResultReportXmit;
import org.openelisglobal.spring.util.SpringContext;
import org.xml.sax.InputSource;

public class ReportTransmission {
    public enum HTTP_TYPE {
        GET, POST
    }

    public void sendHL7Report(ResultReportXmit resultReport, String url, ITransmissionResponseHandler responseHandler) {
        OrderResponseWorker orWorker = SpringContext.getBean(OrderResponseWorker.class);
        try {
            orWorker.createReport(resultReport);
            sendRawReport(orWorker.getHl7Message().encode(), url, true, responseHandler, HTTP_TYPE.POST);
        } catch (HL7Exception e) {
            LogEvent.logError(e);
        } catch (IOException e) {
            LogEvent.logError(e);
        }
    }

    public void sendReport(Object reportObject, String castorPropertyName, String url, boolean sendAsychronously,
            ITransmissionResponseHandler responseHandler) {

        String xmlString = null;
        String castorMappingName = getCastorMappingName(castorPropertyName);

        InputSource source = getSource(castorMappingName);

        Mapping castorMapping = new Mapping();
        try {
            castorMapping.loadMapping(source);

            Marshaller marshaller = new Marshaller();
            marshaller.setMapping(castorMapping);
            Writer writer = new StringWriter();
            marshaller.setWriter(writer);
            marshaller.marshal(reportObject);
            xmlString = writer.toString();

            sendRawReport(xmlString, url, sendAsychronously, responseHandler, HTTP_TYPE.POST);

        } catch (UnknownHostException e) {
            List<String> errors = new ArrayList<>();
            errors.add(e.toString());
            responseHandler.handleResponse(HttpServletResponse.SC_BAD_REQUEST, errors, xmlString);
        } catch (ValidationException | MarshalException | IOException | MappingException e) {
            LogEvent.logError(e);
        } finally {
            try {
                source.getByteStream().close();
            } catch (IOException e) {
                LogEvent.logError(e);
            }
        }
    }

    public void sendRawReport(String contents, String url, boolean sendAsychronously,
            ITransmissionResponseHandler responseHandler, HTTP_TYPE httpType) {
        try {
            IExternalSender sender;
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", contents
            // );
            switch (httpType) {
            case GET:
                sender = SpringContext.getBean(HttpGetSender.class);
                break;
            case POST:
                sender = SpringContext.getBean(HttpPostSender.class);
                sender.setMessage(contents);
                break;
            default:
                sender = SpringContext.getBean(HttpPostSender.class);
                break;
            }

            sender.setURI(url);

            if (sendAsychronously) {
                IAsyncExternalSender asynchSender = SpringContext.getBean(IAsyncExternalSender.class);
                asynchSender.sendMessage(sender, responseHandler, contents);
            } else {
                sender.sendMessage();
                if (responseHandler != null) {
                    responseHandler.handleResponse(sender.getSendResponse(), sender.getErrors(), contents);
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    private String getCastorMappingName(String mapping) {
        InputStream propertyStream = null;

        ResourceLocator resourceLocator = ResourceLocator.getInstance();

        Properties transmissionMap = new Properties();
        try {
            propertyStream = resourceLocator.getNamedResourceAsInputStream(ResourceLocator.XMIT_PROPERTIES);
            transmissionMap.load(propertyStream);
        } catch (IOException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Unable to load transmission resource mappings.", e);
        } finally {
            if (null != propertyStream) {
                try {
                    propertyStream.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
        }

        String castorMappingName = transmissionMap.getProperty(mapping);
        return castorMappingName;
    }

    protected InputSource getSource(String castorMappingName) {
        InputStream mappingXml = Thread.currentThread().getContextClassLoader().getResourceAsStream(castorMappingName);

        return new InputSource(mappingXml);
    }
}
