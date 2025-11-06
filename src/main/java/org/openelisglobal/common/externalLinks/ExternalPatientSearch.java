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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.params.CoreConnectionPNames;
import org.dom4j.DocumentException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.ExtendedPatientSearchResults;
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

    // protected for unit testing called from synchronized block
    protected void doSearch() {

        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        setTimeout(httpclient);

        HttpGet httpget = new HttpGet(connectionString);
        URI getUri = buildConnectionString(httpget.getURI());
        httpget.setURI(getUri);

        CloseableHttpResponse getResponse = null;
        try {
            // Ignore hostname mismatches and allow trust of self-signed certs
            // TODO shouldn't let a self signed cert through
            SSLSocketFactory sslsf = new SSLSocketFactory(new TrustSelfSignedStrategy(),
                    SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            Scheme https = new Scheme("https", 443, sslsf);
            ClientConnectionManager ccm = httpclient.getConnectionManager();
            ccm.getSchemeRegistry().register(https);

            getResponse = httpclient.execute(httpget);
            returnStatus = getResponse.getStatusLine().getStatusCode();
            setPossibleErrors();
            setResults(IOUtils.toString(getResponse.getEntity().getContent(), "UTF-8"));
        } catch (SocketTimeoutException e) {
            errors.add("Response from patient information server took too long.");
            LogEvent.logError(e);
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "Tinny
            // time out"
            // + e);
        } catch (ConnectException e) {
            errors.add("Unable to connect to patient information form service. Service may not be running");
            LogEvent.logError(e);
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "you no
            // talks? "
            // + e);
        } catch (IOException e) {
            errors.add("IO error trying to read input stream.");
            LogEvent.logError(e);
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "all else
            // failed
            // " + e);
        } catch (KeyManagementException e) {
            errors.add("Key management error trying to connect to external search service.");
            LogEvent.logError(e);
        } catch (UnrecoverableKeyException e) {
            errors.add("Unrecoverable key error trying to connect to external search service.");
            LogEvent.logError(e);
        } catch (NoSuchAlgorithmException e) {
            errors.add("No such encyrption algorithm error trying to connect to external search service.");
            LogEvent.logError(e);
        } catch (KeyStoreException e) {
            errors.add("Keystore error trying to connect to external search service.");
            LogEvent.logError(e);
        } catch (RuntimeException e) {
            errors.add("Runtime error trying to retrieve patient information.");
            LogEvent.logError(e);
            httpget.abort();
            throw e;
        } finally {
            if (getResponse != null) {
                try {
                    getResponse.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }

            httpclient.getConnectionManager().shutdown();
            try {
                httpclient.close();
            } catch (IOException e) {
                LogEvent.logError(e.getMessage(), e);
            }
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

    private void setTimeout(HttpClient httpclient) {
        // this one causes a timeout if a connection is established but there is
        // no response within <timeout> seconds
        httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);

        // this one causes a timeout if no connection is established within 10 seconds
        httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
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
