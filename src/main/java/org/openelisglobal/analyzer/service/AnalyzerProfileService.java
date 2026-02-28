package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfile;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerProfileService extends BaseObjectService<AnalyzerProfile, String> {

    List<AnalyzerProfile> listBySource(String source);

    List<AnalyzerProfile> listByMetaId(String profileMetaId);

    AnalyzerProfile getLatestByMetaId(String profileMetaId);

    String importProfile(Map<String, Object> profilePayload, String source, String sysUserId);

    void applyProfileToAnalyzer(String analyzerId, String profileId, String sysUserId);
}
