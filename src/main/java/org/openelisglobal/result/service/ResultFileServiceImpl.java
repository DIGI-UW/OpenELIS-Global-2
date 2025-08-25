package org.openelisglobal.result.service;

import java.util.UUID;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.result.dao.ResultFileDAO;
import org.openelisglobal.result.valueholder.ResultFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResultFileServiceImpl implements ResultFileService {

    @Autowired
    private ResultFileDAO resultFileDAO;

    @Override
    public void saveFile(String accessionNumber, String analysisId, String base64Content, String fileName,
            String contentType, String userId) throws LIMSRuntimeException {
        try {
            ResultFile resultFile = resultFileDAO.getFileByAnalysisId(analysisId);

            if (resultFile == null) {
                resultFile = new ResultFile();
                resultFile.setId(UUID.randomUUID().toString());
                resultFile.setAccessionNumber(accessionNumber);
                resultFile.setAnalysisId(analysisId);
                resultFile.setUploadDate(DateUtil.getNowAsTimestamp());
            }

            resultFile.setFileContent(base64Content);
            resultFile.setFileName(fileName);
            resultFile.setContentType(contentType);
            resultFile.setSysUserId(userId);

            if (resultFileDAO.getFileByAnalysisId(analysisId) == null) {
                resultFileDAO.insert(resultFile);
            } else {
                resultFileDAO.update(resultFile);
            }
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error saving result file", e);
        }
    }

    @Override
    public ResultFile getFileByAnalysisId(String analysisId) throws LIMSRuntimeException {
        try {
            return resultFileDAO.getFileByAnalysisId(analysisId);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error getting result file by analysis ID", e);
        }
    }

    @Override
    public void deleteFile(String analysisId, String userId) throws LIMSRuntimeException {
        try {
            ResultFile file = resultFileDAO.getFileByAnalysisId(analysisId);
            if (file != null) {
                file.setSysUserId(userId);
                resultFileDAO.delete(file);
            }
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error deleting result file", e);
        }
    }
}