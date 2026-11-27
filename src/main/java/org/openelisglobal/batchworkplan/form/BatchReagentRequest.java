package org.openelisglobal.batchworkplan.form;

/**
 * Phase 2 of a batch: the reagent and lot it runs with, and the analyzer it
 * runs on ({@code instrumentId} null or blank for the bench).
 */
public record BatchReagentRequest(Long reagentId, Long reagentLotId, String instrumentId) {
}
