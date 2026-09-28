package org.openelisglobal.sample.service;

public interface SampleItemRejectionService {

    void reject(String sampleItemId, String reason, String authenticatedUserId);
}
