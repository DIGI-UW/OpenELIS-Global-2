package org.openelisglobal.analyzer.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.dao.UnitMappingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.analyzer.valueholder.UnitMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of QCResultExtractionService for extracting QC result data
 * from parsed Q-segments
 * 
 * 
 * Applies QC field mappings to QCSegmentData and returns QCResultDTO with all
 * required fields populated.
 */
@Service
@Transactional
public class QCResultExtractionServiceImpl implements QCResultExtractionService {

    @Autowired
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Autowired
    private AnalyzerFieldDAO analyzerFieldDAO;

    @Autowired
    private UnitMappingDAO unitMappingDAO;

    /**
     * Setter for testing purposes (allows Mockito injection)
     */
    public void setAnalyzerFieldMappingDAO(AnalyzerFieldMappingDAO analyzerFieldMappingDAO) {
        this.analyzerFieldMappingDAO = analyzerFieldMappingDAO;
    }

    /**
     * Setter for testing purposes (allows Mockito injection)
     */
    public void setAnalyzerFieldDAO(AnalyzerFieldDAO analyzerFieldDAO) {
        this.analyzerFieldDAO = analyzerFieldDAO;
    }

    /**
     * Setter for testing purposes (allows Mockito injection)
     */
    public void setUnitMappingDAO(UnitMappingDAO unitMappingDAO) {
        this.unitMappingDAO = unitMappingDAO;
    }

    @Override
    public QCResultDTO extractQCResult(QCSegmentData qcData, String analyzerId) {
        if (qcData == null) {
            throw new LIMSRuntimeException("QC segment data cannot be null");
        }
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            throw new LIMSRuntimeException("Analyzer ID cannot be null or empty");
        }

        // Validate control level (must be L, N, or H)
        String controlLevelStr = qcData.getControlLevel();
        if (controlLevelStr == null || controlLevelStr.trim().isEmpty()) {
            throw new LIMSRuntimeException("Control level cannot be null or empty");
        }

        QCResultDTO.ControlLevel controlLevel = mapControlLevel(controlLevelStr);
        if (controlLevel == null) {
            throw new LIMSRuntimeException("Invalid control level: " + controlLevelStr + ". Must be L, N, or H");
        }

        String testId = mapTestCodeToTestId(qcData.getTestCode(), analyzerId);
        if (testId == null) {
            throw new LIMSRuntimeException("No mapping found for test code: " + qcData.getTestCode());
        }

        String controlLotId = mapControlLotNumberToControlLotId(qcData.getControlLotNumber(), analyzerId);
        if (controlLotId == null) {
            throw new LIMSRuntimeException("No mapping found for control lot number: " + qcData.getControlLotNumber());
        }

        BigDecimal resultValue = parseResultValue(qcData.getResultValue());
        if (resultValue == null) {
            throw new LIMSRuntimeException("Invalid result value: " + qcData.getResultValue());
        }

        String unit = qcData.getUnit() != null ? qcData.getUnit().trim() : "";
        BigDecimal convertedValue = applyUnitConversion(resultValue, unit, qcData.getTestCode(), analyzerId);
        String convertedUnit = getConvertedUnit(unit, qcData.getTestCode(), analyzerId);

        Date timestamp = qcData.getTimestamp();
        if (timestamp == null) {
            timestamp = new Date(); // Use current time if not provided
        }

        QCResultDTO dto = new QCResultDTO();
        dto.setAnalyzerId(analyzerId);
        dto.setTestId(testId);
        dto.setControlLotId(controlLotId);
        dto.setControlLevel(controlLevel);
        dto.setResultValue(convertedValue);
        dto.setUnit(convertedUnit);
        dto.setTimestamp(timestamp);

        return dto;
    }

    /**
     * Map control level string (L/N/H) to ControlLevel enum
     * 
     * @param controlLevelStr Control level string from Q-segment
     * @return ControlLevel enum value, or null if invalid
     */
    private QCResultDTO.ControlLevel mapControlLevel(String controlLevelStr) {
        if (controlLevelStr == null) {
            return null;
        }

        String level = controlLevelStr.trim().toUpperCase();
        switch (level) {
        case "L":
            return QCResultDTO.ControlLevel.LOW;
        case "N":
            return QCResultDTO.ControlLevel.NORMAL;
        case "H":
            return QCResultDTO.ControlLevel.HIGH;
        default:
            return null;
        }
    }

    /**
     * Map test code to Test ID using QC field mappings
     * 
     * @param testCode   Test code from Q-segment
     * @param analyzerId Analyzer ID
     * @return Test ID if mapping found, null otherwise
     */
    private String mapTestCodeToTestId(String testCode, String analyzerId) {
        Optional<AnalyzerField> fieldOpt = analyzerFieldDAO.findByAnalyzerIdAndFieldName(analyzerId, testCode);
        if (!fieldOpt.isPresent()) {
            return null;
        }

        AnalyzerField field = fieldOpt.get();
        List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findByAnalyzerFieldId(field.getId());

        for (AnalyzerFieldMapping mapping : mappings) {
            if (mapping.getIsActive()
                    && mapping.getOpenelisFieldType() == AnalyzerFieldMapping.OpenELISFieldType.TEST) {
                return mapping.getOpenelisFieldId();
            }
        }

        return null;
    }

    /**
     * Map control lot number to Control Lot ID using QC field mappings
     * 
     * @param controlLotNumber Control lot number from Q-segment
     * @param analyzerId       Analyzer ID
     * @return Control Lot ID if mapping found, null otherwise
     */
    private String mapControlLotNumberToControlLotId(String controlLotNumber, String analyzerId) {
        Optional<AnalyzerField> fieldOpt = analyzerFieldDAO.findByAnalyzerIdAndFieldName(analyzerId, controlLotNumber);
        if (!fieldOpt.isPresent()) {
            return null;
        }

        AnalyzerField field = fieldOpt.get();
        List<AnalyzerFieldMapping> mappings = analyzerFieldMappingDAO.findByAnalyzerFieldId(field.getId());

        for (AnalyzerFieldMapping mapping : mappings) {
            if (mapping.getIsActive() && mapping.getOpenelisFieldType() == AnalyzerFieldMapping.OpenELISFieldType.QC) {
                return mapping.getOpenelisFieldId();
            }
        }

        return null;
    }

    /**
     * Parse result value string to BigDecimal
     * 
     * @param resultValueStr Result value string from Q-segment
     * @return BigDecimal value, or null if invalid
     */
    private BigDecimal parseResultValue(String resultValueStr) {
        if (resultValueStr == null || resultValueStr.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(resultValueStr.trim());
        } catch (NumberFormatException e) {
            LogEvent.logError("Invalid result value format: " + resultValueStr, e);
            return null;
        }
    }

    /**
     * Apply unit conversion if UnitMapping is configured
     * 
     * @param resultValue Original result value
     * @param unit        Original unit
     * @param testCode    Test code (for finding field)
     * @param analyzerId  Analyzer ID
     * @return Converted result value, or original if no conversion
     */
    private BigDecimal applyUnitConversion(BigDecimal resultValue, String unit, String testCode, String analyzerId) {
        if (unit == null || unit.trim().isEmpty()) {
            return resultValue; // No unit to convert
        }

        Optional<AnalyzerField> fieldOpt = analyzerFieldDAO.findByAnalyzerIdAndFieldName(analyzerId, testCode);
        if (!fieldOpt.isPresent()) {
            return resultValue;
        }

        AnalyzerField field = fieldOpt.get();
        List<UnitMapping> unitMappings = unitMappingDAO.findByAnalyzerFieldId(field.getId());

        for (UnitMapping unitMapping : unitMappings) {
            if (unitMapping.getAnalyzerUnit().equals(unit)) {
                BigDecimal conversionFactor = unitMapping.getConversionFactor();
                if (conversionFactor != null && conversionFactor.compareTo(BigDecimal.ZERO) != 0) {
                    return resultValue.multiply(conversionFactor);
                }
            }
        }

        return resultValue;
    }

    /**
     * Get converted unit if UnitMapping is configured
     *
     * @param unit       Original unit
     * @param testCode   Test code (for finding field)
     * @param analyzerId Analyzer ID
     * @return Converted unit, or original if no conversion
     */
    private String getConvertedUnit(String unit, String testCode, String analyzerId) {
        if (unit == null || unit.trim().isEmpty()) {
            return unit;
        }

        Optional<AnalyzerField> fieldOpt = analyzerFieldDAO.findByAnalyzerIdAndFieldName(analyzerId, testCode);
        if (!fieldOpt.isPresent()) {
            return unit;
        }

        AnalyzerField field = fieldOpt.get();
        List<UnitMapping> unitMappings = unitMappingDAO.findByAnalyzerFieldId(field.getId());

        for (UnitMapping unitMapping : unitMappings) {
            if (unitMapping.getAnalyzerUnit().equals(unit)) {
                String openelisUnit = unitMapping.getOpenelisUnit();
                if (openelisUnit != null && !openelisUnit.trim().isEmpty()) {
                    return openelisUnit;
                }
            }
        }

        return unit;
    }
}
