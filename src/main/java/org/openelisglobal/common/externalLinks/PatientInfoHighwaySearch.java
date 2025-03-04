package org.openelisglobal.common.externalLinks;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import javax.xml.namespace.QName;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.HttpStatus;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.ExtendedPatientSearchResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service("InfoHighwaySearch")
@Scope("prototype")
public class PatientInfoHighwaySearch implements IExternalPatientSearch {

    @Value("${org.openelisglobal.externalSearch.infohighway.timeout:50000}")
    private Integer timeout;

    public static final String MALFORMED_REPLY = "Malformed reply";
    public static final String URI_BUILD_FAILURE = "Failed to build URI";

    private boolean finished = false;

    private String firstName;
    private String lastName;
    private String STNumber;
    private String subjectNumber;
    private String nationalId;
    private String guid;
    private String connectionString;
    private String connectionName;
    private String connectionPassword;

    protected String resultXML;
    protected List<ExtendedPatientSearchResults> searchResults = new ArrayList<>();
    protected List<String> errors;
    protected int returnStatus = HttpStatus.SC_CREATED;

    private enum InfoHighwayField {
        CITIZEN_NIC_NUMBER("CITIZEN.NIC_NUMBER"), CITIZEN_SURNAME("CITIZEN.SURNAME"),
        CITIZEN_FIRST_NAME("CITIZEN.FIRST_NAME");

        private String fieldName;

        private InfoHighwayField(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    @Override
    public synchronized void setConnectionCredentials(String connectionString, String name, String password) {
        if (finished) {
            throw new IllegalStateException("ServiceCredentials set after ExternalPatientSearch thread was started");
        }

        this.connectionString = connectionString;
        connectionName = name;
        connectionPassword = password;
    }

    @Override
    public synchronized void setSearchCriteria(String lastName, String firstName, String STNumber, String subjectNumber,
            String nationalID, String guid) throws IllegalStateException {

        if (finished) {
            throw new IllegalStateException("Search criteria set after ExternalPatientSearch thread was started");
        }

        this.lastName = lastName;
        this.firstName = firstName;
        this.STNumber = STNumber;
        this.subjectNumber = subjectNumber;
        this.nationalId = nationalID;
        this.guid = guid;
    }

    @Override
    public synchronized List<ExtendedPatientSearchResults> getSearchResults() {

        if (!finished) {
            throw new IllegalStateException("Results requested before ExternalPatientSearch thread was finished");
        }

        if (searchResults == null) {
            searchResults = new ArrayList<>();
        }

        return searchResults;
    }

    public int getSearchResultStatus() {
        if (!finished) {
            throw new IllegalStateException("Result status requested ExternalPatientSearch before search was finished");
        }

        return returnStatus;
    }

    @Override
    @Async
    public Future<Integer> runExternalSearch() {
        try {
            synchronized (this) {
                if (noSearchTerms()) {
                    throw new IllegalStateException("Search requested before without any search terms.");
                }

                if (connectionCredentialsIncomplete()) {
                    throw new IllegalStateException("Search requested before connection credentials set.");
                }
                errors = new ArrayList<>();

                doSearch();
            }
        } finally {
            finished = true;
        }
        return new AsyncResult<>(getSearchResultStatus());
    }

    private boolean connectionCredentialsIncomplete() {
        return GenericValidator.isBlankOrNull(connectionString) || GenericValidator.isBlankOrNull(connectionName)
                || GenericValidator.isBlankOrNull(connectionPassword);
    }

    private boolean noSearchTerms() {
        return GenericValidator.isBlankOrNull(firstName) && GenericValidator.isBlankOrNull(lastName)
                && GenericValidator.isBlankOrNull(nationalId) && GenericValidator.isBlankOrNull(STNumber);
    }

    protected void doSearch() {
        String soapAction = "query";
        try {
            callSoapWebService(connectionString, soapAction);
        } catch (SOAPException e) {
            LogEvent.logError(e);
        }
        setPossibleErrors();
    }

    protected void setResults(String resultsAsXml) {
        resultXML = resultsAsXml;
    }

    private void setPossibleErrors() {
        switch (returnStatus) {
        case HttpStatus.SC_UNAUTHORIZED: {
            errors.add("Access denied to patient information service.");
            break;
        }
        case HttpStatus.SC_INTERNAL_SERVER_ERROR: {
            errors.add("Internal error on patient information service.");
            break;
        }
        case HttpStatus.SC_OK: {
            break; // NO-OP
        }
        default: {
            errors.add("Unknown error trying to connect to patient information service. Return status was "
                    + returnStatus);
        }
        }
    }

    private void createSoapEnvelope(SOAPMessage soapMessage) throws SOAPException {
        SOAPPart soapPart = soapMessage.getSOAPPart();

        String soapPrefix = "soapenv";
        String soapNamespaceURI = "http://schemas.xmlsoap.org/soap/envelope/";
        String wsPrefix = "ws";
        String wsNamespaceURI = "http://ws.server.mhaccess.crimsonlogic.com/";

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        changeDefaultSoapNamespace(envelope, soapPrefix, soapNamespaceURI);

        envelope.addNamespaceDeclaration(wsPrefix, wsNamespaceURI);

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapQueryElem = soapBody.addChildElement("query", wsPrefix);
        SOAPElement soapQueryInputElem = soapQueryElem.addChildElement("queryInput");
        SOAPElement soapUserIdElem = soapQueryInputElem.addChildElement("userId");
        soapUserIdElem.addTextNode(connectionName);
        SOAPElement soapPassElem = soapQueryInputElem.addChildElement("pass");
        soapPassElem.addTextNode(connectionPassword);
        SOAPElement soapQueryIdElem = soapQueryInputElem.addChildElement("queryId");
        if (!GenericValidator.isBlankOrNull(nationalId)) {
            soapQueryIdElem.addTextNode("MOH003");
            addSearchParamIfNotBlank(nationalId, InfoHighwayField.CITIZEN_NIC_NUMBER, soapQueryInputElem);
        } else {
            soapQueryIdElem.addTextNode("MOH004");
            addSearchParamIfNotBlank(firstName, InfoHighwayField.CITIZEN_FIRST_NAME, soapQueryInputElem);
            addSearchParamIfNotBlank(lastName, InfoHighwayField.CITIZEN_SURNAME, soapQueryInputElem);
        }
    }

    private void addSearchParamIfNotBlank(String fieldValue, InfoHighwayField field, SOAPElement soapQueryInputElem)
            throws SOAPException {
        if (!GenericValidator.isBlankOrNull(fieldValue)) {
            // TODO Auto-generated method stub
            SOAPElement soapQwsInputParamsElem = soapQueryInputElem.addChildElement("qwsInputParams");
            SOAPElement soapFieldElem = soapQwsInputParamsElem.addChildElement("field");
            SOAPElement soapValuesElem = soapQwsInputParamsElem.addChildElement("values");
            soapFieldElem.addTextNode(field.getFieldName());
            soapValuesElem.addTextNode(fieldValue);
        }
    }

    private void changeDefaultSoapNamespace(SOAPEnvelope envelope, String soapPrefix, String soapNamespaceURI)
            throws SOAPException {
        envelope.removeNamespaceDeclaration(envelope.getPrefix());
        envelope.addNamespaceDeclaration(soapPrefix, soapNamespaceURI);
        envelope.setPrefix(soapPrefix);
        envelope.getHeader().setPrefix(soapPrefix);
        envelope.getBody().setPrefix(soapPrefix);
    }

    private void callSoapWebService(String soapEndpointUrl, String soapAction)
            throws UnsupportedOperationException, SOAPException {
        // Create SOAP Connection
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = null;
        try {
            soapConnection = soapConnectionFactory.createConnection();
            SOAPMessage soapMessage = createSOAPRequest(soapAction);

            // Send SOAP Message to SOAP Server
            SOAPMessage soapResponse = soapConnection.call(soapMessage, soapEndpointUrl);
            // Print the SOAP Response
            if (soapResponse.getSOAPPart().getEnvelope().getBody().getFault() != null) {
                // TODO gather actual fault codes and specify a better returnStatus
                String faultCode = soapResponse.getSOAPPart().getEnvelope().getBody().getFault().getFaultCode();
                returnStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            } else {
                returnStatus = HttpStatus.SC_OK;
                processResponse(soapResponse);
            }

        } catch (Exception e) {
            LogEvent.logError("Error occurred while sending SOAP Request to Server!", e);
        } finally {
            if (soapConnection != null) {
                soapConnection.close();
            }
        }
    }

    private void processResponse(SOAPMessage soapResponse) throws SOAPException {
        SOAPBody soapResponseBody = soapResponse.getSOAPBody();
        QName bodyName = new QName("http://ws.server.mhaccess.crimsonlogic.com/", "queryResponse", "ns3");
        Iterator<jakarta.xml.soap.Node> iterator = soapResponseBody.getChildElements(bodyName);
        while (iterator.hasNext()) {
            SOAPElement queryResponse = (SOAPElement) iterator.next();

            Node returnNode = queryResponse.getElementsByTagName("return").item(0);
            if (returnNode.getNodeType() == Node.ELEMENT_NODE) {
                Element returnElement = (Element) returnNode;
                NodeList fieldsList = returnElement.getElementsByTagName("fields");
                NodeList valuesList = returnElement.getElementsByTagName("values");
                for (int i = 0; i < valuesList.getLength(); ++i) {
                    if (valuesList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element valuesElement = (Element) valuesList.item(i);
                        addPatient(fieldsList, valuesElement.getElementsByTagName("value"));
                    }
                }
            }
        }
    }

    private void addPatient(NodeList fieldsList, NodeList valueList) {
        try {
            ExtendedPatientSearchResults patient = new ExtendedPatientSearchResults();
            patient.setDataSourceName(MessageUtil.getMessage("externalconnections.infohighway"));
            for (int i = 0; i < fieldsList.getLength(); ++i) {
                addField(patient, fieldsList.item(i).getTextContent(), valueList.item(i).getTextContent());
            }
            searchResults.add(patient);
        } catch (DOMException | ParseException e) {
            LogEvent.logError("Could not add patient retrieved from infohighway. Continuing", e);
        }
    }

    private void addField(ExtendedPatientSearchResults patient, String fieldName, String value) throws ParseException {
        switch (fieldName) {
        case "NIC_NUMBER":
            patient.setNationalId(value);
            break;
        case "SURNAME":
            patient.setLastName(value);
            break;
        case "FIRST_NAME":
            patient.setFirstName(value);
            break;
        case "SEX":
            patient.setGender(value);
            break;
        case "BIRTH_DATE":
            setDate(patient, value);
            break;
        case "FLAT_NO_APARTMENT_NAME":
            patient.setFlatNumberApartmentName(value);
            break;
        case "STREET_NAME":
            patient.setStreetName(value);
            break;
        case "POSTAL_CODE":
            patient.setPostalCode(value);
            break;
        case "TOWN_VILLAGE":
            patient.setCampCommune(value);
            break;
        case "LOCALITY":
            patient.setTown(value);
            break;
        case "DISTRICT":
            patient.setCounty(value);
            break;
        default:
            break;
        }
    }

    private void setDate(ExtendedPatientSearchResults patient, String dateString) throws ParseException {

        String dateFormat = "yyyy-MM-dd";
        String dateTimeFormat = "yyyy-MM-dd HH:mm:ss.S";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat(dateTimeFormat);
        if (!GenericValidator.isBlankOrNull(dateString)) {
            if (dateString.length() == dateFormat.length()) {
                Date date = dateFormatter.parse(dateString);
                patient.setBirthdate(DateUtil.formatDateAsText(date));
            } else if (dateString.length() == dateTimeFormat.length()) {
                Date date = dateTimeFormatter.parse(dateString);
                patient.setBirthdate(DateUtil.formatDateAsText(date));
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "setDate",
                        "Could not parse date received from infohighway search");
            }
        }
    }

    private SOAPMessage createSOAPRequest(String soapAction) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        createSoapEnvelope(soapMessage);

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", soapAction);

        soapMessage.saveChanges();

        return soapMessage;
    }

    @Override
    public String getConnectionString() {
        return connectionString;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }
}
