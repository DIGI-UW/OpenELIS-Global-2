package org.openelisglobal.analyzer.service;

/**
 * Exact analyzer candidate acknowledged by Bridge during desired-state sync.
 */
public record BridgeRegisteredCandidate(String analyzerId, String profileId, int profileRevision,
        String desiredStateFingerprint) {
}
