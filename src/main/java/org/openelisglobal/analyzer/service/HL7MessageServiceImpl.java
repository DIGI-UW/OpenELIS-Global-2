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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzer.service;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Compilation stub for HL7MessageService — satisfies plugin dependencies.
 *
 * <p>
 * Real implementation replaces this stub when the analyzer feature lands. All
 * methods throw {@link UnsupportedOperationException} at runtime.
 */
@Service
public class HL7MessageServiceImpl implements HL7MessageService {

    @Override
    public OruR01ParseResult parseOruR01(String rawMessage) {
        throw new UnsupportedOperationException("HL7MessageServiceImpl stub — not yet implemented");
    }

    @Override
    public String generateOrmO01(OrmO01Request request) {
        throw new UnsupportedOperationException("HL7MessageServiceImpl stub — not yet implemented");
    }

    @Override
    public MshInfo extractMshInfo(String rawMessage) {
        throw new UnsupportedOperationException("HL7MessageServiceImpl stub — not yet implemented");
    }

    @Override
    public List<String> toSegmentLines(String rawMessage) {
        throw new UnsupportedOperationException("HL7MessageServiceImpl stub — not yet implemented");
    }
}
