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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.externalLinks;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dom4j.DocumentException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.ExtendedPatientSearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

@Service
@Primary
@Scope("prototype")
public class ExternalPatientSearch implements IExternalPatientSearch {

    @Value("${org.openelisglobal.externalSearch.timeout:5000}")
    private Integer timeout;

    @Autowired
    @Qualifier("patientSearchHttpClient")
    private CloseableHttpClient httpClient;

    private static final String GET_PARAM_PWD = "pwd";
    private static final String GET_PARAM_NAME = "name";
    private static final String GET_PARAM_NATIONAL_ID = "nationalId";
    private static final String GET_PARAM_ST = "ST";
    private static final String GET_PARAM_SUBJECT = "subjectNumber";
    private static final String GET_PARAM_LAST = "last";
    private static final String GET_PARAM_FIRST = "first";
    private static final String GET_PARAM_GUID = "guid";

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
    protected List<ExtendedPatientSearchResults> searchResults;
    protected List<String> errors;
    protected int returnStatus = HttpStatus.SC_CREATED;

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

            convertXMLToResults();
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

    // Called from the synchronized search operation.
    protected void doSearch() {
        returnStatus = HttpStatus.SC_BAD_GATEWAY;
        URI endpoint;
        try {
            endpoint = new URI(connectionString);
        } catch (URISyntaxException e) {
            errors.add(URI_BUILD_FAILURE);
            return;
        }
        if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getUserInfo() != null || endpoint.getFragment() != null) {
            errors.add("Patient search requires an HTTPS endpoint without embedded credentials or a fragment.");
            return;
        }
        URI query = buildConnectionString(endpoint);
        if (query == null) {
            return;
        }
        HttpGet request = new HttpGet(query);
        request.setConfig(RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout)
                .setConnectionRequestTimeout(timeout).setRedirectsEnabled(false).build());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            returnStatus = response.getStatusLine().getStatusCode();
            setPossibleErrors();
            if (returnStatus == HttpStatus.SC_OK && response.getEntity() != null) {
                setResults(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
            }
        } catch (SocketTimeoutException e) {
            errors.add("Response from patient information server took too long.");
        } catch (ConnectException e) {
            errors.add("Unable to connect to patient information service.");
        } catch (IOException e) {
            errors.add("Unable to establish a verified connection or read the patient search response.");
            // Request URLs contain search credentials and patient terms; never log them.
            LogEvent.logError(getClass().getSimpleName(), "doSearch", e.getClass().getSimpleName());
        }
    }

    private void convertXMLToResults() {
        if (!GenericValidator.isBlankOrNull(resultXML)) {

            ExternalPatientSearchResultsXMLConverter converter = new ExternalPatientSearchResultsXMLConverter();

            try {
                searchResults = converter.convertXMLToSearchResults(resultXML);
            } catch (DocumentException e) {
                errors.add(MALFORMED_REPLY);
            }
        }
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
            errors.add("Unknown error trying to connect to patient information service. Resturn status was "
                    + returnStatus);
        }
        }
    }

    private URI buildConnectionString(URI uriStart) {

        URI uriFinal = null;
        try {
            uriFinal = new URIBuilder(uriStart).addParameter(GET_PARAM_FIRST, firstName)
                    .addParameter(GET_PARAM_LAST, lastName).addParameter(GET_PARAM_ST, STNumber)
                    .addParameter(GET_PARAM_SUBJECT, subjectNumber).addParameter(GET_PARAM_NATIONAL_ID, nationalId)
                    .addParameter(GET_PARAM_GUID, guid).addParameter(GET_PARAM_NAME, connectionName)
                    .addParameter(GET_PARAM_PWD, connectionPassword).build();
        } catch (URISyntaxException e) {
            errors.add(URI_BUILD_FAILURE);
        }

        return uriFinal;
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
