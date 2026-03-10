package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.dao.UnitMappingDAO;
import org.openelisglobal.analyzer.valueholder.UnitMapping;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for UnitMapping operations
 * 
 * Provides business logic for managing unit mappings with: - Conversion factor
 * validation - Unit mismatch handling
 */
@Service
@Transactional
public class UnitMappingServiceImpl extends BaseObjectServiceImpl<UnitMapping, String> implements UnitMappingService {

    private final UnitMappingDAO unitMappingDAO;

    @Autowired
    public UnitMappingServiceImpl(UnitMappingDAO unitMappingDAO) {
        super(UnitMapping.class);
        this.unitMappingDAO = unitMappingDAO;
    }

    @Override
    protected BaseDAO<UnitMapping, String> getBaseObjectDAO() {
        return unitMappingDAO;
    }

    @Override
    @Transactional
    public String createMapping(UnitMapping mapping) {
        // Validate conversion factor if units don't match
        validateConversionFactor(mapping);

        return unitMappingDAO.insert(mapping);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitMapping> getMappingsByAnalyzerFieldId(String analyzerFieldId) {
        return unitMappingDAO.findByAnalyzerFieldId(analyzerFieldId);
    }

    /**
     * Validate that conversion factor is provided when units don't match
     * 
     * Rules: - If units match: conversion factor is optional - If units don't
     * match: conversion factor is required (unless rejectIfMismatch=true)
     * 
     * @param mapping The mapping to validate
     * @throws LIMSRuntimeException if unit mismatch without conversion factor
     */
    private void validateConversionFactor(UnitMapping mapping) {
        if (mapping.getAnalyzerUnit() == null || mapping.getAnalyzerUnit().trim().isEmpty()) {
            throw new LIMSRuntimeException("AnalyzerUnit is required");
        }

        if (mapping.getOpenelisUnit() == null || mapping.getOpenelisUnit().trim().isEmpty()) {
            throw new LIMSRuntimeException("OpenELISUnit is required");
        }

        String analyzerUnit = mapping.getAnalyzerUnit().trim();
        String openelisUnit = mapping.getOpenelisUnit().trim();

        // If units match, conversion factor is optional
        if (analyzerUnit.equalsIgnoreCase(openelisUnit)) {
            return; // No validation needed for matching units
        }

        // Units don't match - check if conversion factor is provided
        if (mapping.getConversionFactor() == null) {
            // If rejectIfMismatch is true, we can reject without conversion factor
            if (mapping.getRejectIfMismatch() == null || !mapping.getRejectIfMismatch()) {
                throw new LIMSRuntimeException("Conversion factor is required when units don't match. Analyzer unit: '"
                        + analyzerUnit + "', OpenELIS unit: '" + openelisUnit
                        + "'. Set rejectIfMismatch=true to reject mismatched units instead.");
            }
        }
    }
}
