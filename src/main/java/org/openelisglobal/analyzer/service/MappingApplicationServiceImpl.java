package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for applying field mappings to ASTM message segments
 * 
 * 
 * This service transforms raw ASTM message segments by applying configured
 * field mappings. Used by MappingAwareAnalyzerLineInserter wrapper to apply
 * mappings before delegating to plugin inserter.
 */
@Service
@Transactional
public class MappingApplicationServiceImpl implements MappingApplicationService {

    private final AnalyzerFieldMappingDAO analyzerFieldMappingDAO;
    private final AnalyzerFieldDAO analyzerFieldDAO;

    @Autowired
    public MappingApplicationServiceImpl(AnalyzerFieldMappingDAO analyzerFieldMappingDAO,
            AnalyzerFieldDAO analyzerFieldDAO) {
        this.analyzerFieldMappingDAO = analyzerFieldMappingDAO;
        this.analyzerFieldDAO = analyzerFieldDAO;
    }

    @Override
    public MappingApplicationResult applyMappings(String analyzerId, List<String> lines) {
        MappingApplicationResult result = new MappingApplicationResult();

        if (lines == null || lines.isEmpty()) {
            result.getErrors().add("ASTM message lines are empty");
            return result;
        }

        try {
            List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);

            if (mappings == null || mappings.isEmpty()) {
                result.setTransformedLines(new ArrayList<>(lines));
                result.setHasMappings(false);
                result.setSuccess(true);
                return result;
            }

            result.setHasMappings(true);

            Map<String, AnalyzerFieldMapping> mappingsByFieldName = new HashMap<>();
            for (AnalyzerFieldMapping mapping : mappings) {
                AnalyzerField field = mapping.getAnalyzerField();
                if (field != null && field.getFieldName() != null) {
                    mappingsByFieldName.put(field.getFieldName(), mapping);
                }
            }

            List<String> transformedLines = new ArrayList<>();
            List<String> unmappedFields = new ArrayList<>();

            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) {
                    transformedLines.add(line);
                    continue;
                }

                String[] segments = line.split("\\|");
                if (segments.length < 2) {
                    transformedLines.add(line);
                    continue;
                }

                String segmentType = segments[0].trim();
                if (segmentType.length() == 0) {
                    transformedLines.add(line);
                    continue;
                }

                String transformedLine = transformLine(line, segments, segmentType, mappingsByFieldName,
                        unmappedFields);
                transformedLines.add(transformedLine);
            }

            result.setTransformedLines(transformedLines);
            result.setUnmappedFields(unmappedFields);
            result.setSuccess(true);

        } catch (Exception e) {
            LogEvent.logError("Error applying mappings: " + e.getMessage(), e);
            result.getErrors().add("Error applying mappings: " + e.getMessage());
            result.setSuccess(false);
            // Return original lines on error
            result.setTransformedLines(new ArrayList<>(lines));
        }

        return result;
    }

    /**
     * Transform a single ASTM line by applying mappings
     * 
     * @param line                Original line
     * @param segments            Parsed segments
     * @param segmentType         Segment type (H, P, O, R, etc.)
     * @param mappingsByFieldName Map of mappings by field name
     * @param unmappedFields      List to collect unmapped fields
     * @return Transformed line
     */
    private String transformLine(String line, String[] segments, String segmentType,
            Map<String, AnalyzerFieldMapping> mappingsByFieldName, List<String> unmappedFields) {
        // Line passthrough — segment-type transformation deferred to Phase 2

        if (segmentType.equals("R")) {
            // Format: R|sequence|test_code|value|units|...
            if (segments.length >= 3) {
                String testCode = segments[2].trim();
                // Extract test code from field (format: ^^^TEST_CODE^...)
                String[] testCodeFields = testCode.split("\\^");
                if (testCodeFields.length >= 4) {
                    String actualTestCode = testCodeFields[3];
                    if (!mappingsByFieldName.containsKey(actualTestCode)) {
                        unmappedFields.add(actualTestCode);
                    }
                }
            }
        }

        return line;
    }

    @Override
    public boolean hasActiveMappings(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return false;
        }

        try {
            List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);
            return mappings != null && !mappings.isEmpty();
        } catch (Exception e) {
            LogEvent.logError("Error checking active mappings: " + e.getMessage(), e);
            return false;
        }
    }
}
