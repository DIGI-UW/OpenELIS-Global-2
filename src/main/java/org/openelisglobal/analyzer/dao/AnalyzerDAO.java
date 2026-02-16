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
 */
package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerDAO extends BaseDAO<Analyzer, String> {

    Optional<Analyzer> findByIpAddress(String ipAddress);

    Optional<Analyzer> findByName(String name);

    List<Analyzer> findGenericAnalyzersWithPatterns();

    List<Analyzer> findAllWithTypes();

    Optional<Analyzer> findByIdWithType(String id);
}
