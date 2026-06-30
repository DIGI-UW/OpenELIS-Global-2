package org.openelisglobal.analyzer.service;

public interface QcStatusService {
    QcStatus resolveStatus(String testId, String instrumentId);
}
