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
package org.openelisglobal.samplepdf.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.samplepdf.valueholder.SamplePdf;

/**
 * @author Hung Nguyen
 */
public interface SamplePdfDAO extends BaseDAO<SamplePdf, String> {

    public boolean isAccessionNumberFound(int accessionNumber) throws LIMSRuntimeException;

    // bugzilla 2529,2530,2531
    public SamplePdf getSamplePdfByAccessionNumber(SamplePdf samplePdf) throws LIMSRuntimeException;
}
