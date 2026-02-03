package org.openelisglobal.fhir.facade.service;

import org.springframework.http.HttpHeaders;

public interface FhirFacadeService {

    String proxyGetRequest(String path, String queryString, HttpHeaders headers);

    String proxyDeleteRequest(String path, HttpHeaders headers);

    String createResource(String resourceType, String body);

    String updateResource(String resourceType, String id, String body);

    int getLastResponseStatus();
}
