package org.openelisglobal.result.service;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.result.valueholder.ResultFile;

public interface ResultFileService {
    void saveFile(String accessionNumber, String analysisId, String base64Content, String fileName, String contentType,
            String userId) throws LIMSRuntimeException;

    ResultFile getFileByAnalysisId(String analysisId) throws LIMSRuntimeException;

    void deleteFile(String analysisId, String userId) throws LIMSRuntimeException;
}