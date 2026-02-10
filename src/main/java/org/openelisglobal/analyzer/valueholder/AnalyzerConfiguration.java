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
package org.openelisglobal.analyzer.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Compilation stub for AnalyzerConfiguration — satisfies GenericASTM/GenericHL7
 * plugin dependencies.
 *
 * <p>
 * Real JPA entity (with @Entity, @Table, full field set) replaces this stub
 * when the analyzer feature lands.
 */
public class AnalyzerConfiguration extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private Analyzer analyzer;
    private String identifierPattern;
    private boolean genericPlugin = false;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public String getIdentifierPattern() {
        return identifierPattern;
    }

    public void setIdentifierPattern(String identifierPattern) {
        this.identifierPattern = identifierPattern;
    }

    public boolean isGenericPlugin() {
        return genericPlugin;
    }

    public void setGenericPlugin(boolean genericPlugin) {
        this.genericPlugin = genericPlugin;
    }

    /** Analyzer status values — matches database constraint. */
    public enum AnalyzerStatus {
        INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE, DELETED
    }
}
