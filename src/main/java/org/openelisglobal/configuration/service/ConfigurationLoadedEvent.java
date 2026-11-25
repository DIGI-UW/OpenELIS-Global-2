package org.openelisglobal.configuration.service;

/**
 * Published once a configuration load has run, at startup or on a reload, so
 * reference data built from configured entries (for example container types
 * from the environmental container dictionary, OGC-1611 FR-G4) can follow it.
 *
 * @param source where the load came from, as ConfigurationImportRun records it
 */
public record ConfigurationLoadedEvent(String source) {
}
