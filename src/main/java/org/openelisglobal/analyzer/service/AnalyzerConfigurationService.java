/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzer.service;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;

/**
 * Compilation stub for AnalyzerConfigurationService — satisfies
 * GenericASTM/GenericHL7 plugin dependencies.
 *
 * <p>
 * Real interface (extending BaseObjectService, with full method set) replaces
 * this stub when the analyzer feature lands.
 */
public interface AnalyzerConfigurationService {

    /**
     * Find a generic-plugin AnalyzerConfiguration whose identifier_pattern matches
     * the given analyzer identifier.
     *
     * @param analyzerIdentifier identifier extracted from inbound analyzer message
     *                           (e.g. ASTM H-segment field 4, HL7 MSH-3)
     * @return Optional matching AnalyzerConfiguration
     */
    Optional<AnalyzerConfiguration> findByIdentifierPatternMatch(String analyzerIdentifier);
}
