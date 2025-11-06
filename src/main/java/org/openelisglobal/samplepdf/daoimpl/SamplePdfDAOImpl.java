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
package org.openelisglobal.samplepdf.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.samplepdf.dao.SamplePdfDAO;
import org.openelisglobal.samplepdf.valueholder.SamplePdf;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hung Nguyen
 */
@Component
@Transactional
public class SamplePdfDAOImpl extends BaseDAOImpl<SamplePdf, String> implements SamplePdfDAO {

    public SamplePdfDAOImpl() {
        super(SamplePdf.class);
    }

    @Override
    public boolean isAccessionNumberFound(int accessionNumber) throws LIMSRuntimeException {
        Boolean isFound = false;
        try {
            String sql = "from SamplePdf s where s.accessionNumber = :param and s.allowView='Y'";
            Query<SamplePdf> query = entityManager.unwrap(Session.class).createQuery(sql, SamplePdf.class);
            query.setParameter("param", accessionNumber);
            List<SamplePdf> list = query.list();
            if ((list != null) && !list.isEmpty()) {
                isFound = true;
            }
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SamplePdf isAccessionNumberFound()", e);
        }

        return isFound;
    }

    // bugzilla 2529,2530,2531
    @Override
    @Transactional(readOnly = true)
    public SamplePdf getSamplePdfByAccessionNumber(SamplePdf samplePdf) throws LIMSRuntimeException {
        try {
            String sql = "from SamplePdf s where s.accessionNumber = :param";
            Query<SamplePdf> query = entityManager.unwrap(Session.class).createQuery(sql, SamplePdf.class);
            query.setParameter("param", Integer.parseInt(samplePdf.getAccessionNumber()));

            List<SamplePdf> list = query.list();
            if ((list != null) && !list.isEmpty()) {
                samplePdf = list.get(0);
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SamplePdf getSamplePdfByAccessionNumber()", e);
        }

        return samplePdf;
    }
}
