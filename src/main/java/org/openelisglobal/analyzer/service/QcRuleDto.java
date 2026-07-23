package org.openelisglobal.analyzer.service;

/**
 * Lightweight DTO for QC rules transmitted to the bridge. Contains only the
 * fields needed for rule evaluation.
 */
public record QcRuleDto(String ruleType, String targetField, String operand) {
}
