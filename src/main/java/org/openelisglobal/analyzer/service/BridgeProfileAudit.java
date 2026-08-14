package org.openelisglobal.analyzer.service;

import java.time.Instant;

public record BridgeProfileAudit(String action, String actor, Instant markedAt) {
}
