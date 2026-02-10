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
import org.springframework.stereotype.Service;

/**
 * Compilation stub for AnalyzerConfigurationService — satisfies plugin
 * dependencies.
 *
 * <p>
 * Real implementation (with DAO, status transitions, pattern matching) replaces
 * this stub when the analyzer feature lands. All methods throw
 * {@link UnsupportedOperationException} at runtime.
 */
@Service
public class AnalyzerConfigurationServiceImpl implements AnalyzerConfigurationService {

    @Override
    public Optional<AnalyzerConfiguration> findByIdentifierPatternMatch(String analyzerIdentifier) {
        throw new UnsupportedOperationException("AnalyzerConfigurationServiceImpl stub — not yet implemented");
    }
}
