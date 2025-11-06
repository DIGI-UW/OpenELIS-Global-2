package org.openelisglobal.samplepdf.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.samplepdf.dao.SamplePdfDAO;
import org.openelisglobal.samplepdf.valueholder.SamplePdf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SamplePdfServiceImpl extends AuditableBaseObjectServiceImpl<SamplePdf, String>
        implements SamplePdfService {
    @Autowired
    protected SamplePdfDAO baseObjectDAO;

    SamplePdfServiceImpl() {
        super(SamplePdf.class);
    }

    @Override
    protected SamplePdfDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public boolean isAccessionNumberFound(int accessionNumber) {
        return getBaseObjectDAO().isAccessionNumberFound(accessionNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public SamplePdf getSamplePdfByAccessionNumber(SamplePdf samplePdf) {
        return getBaseObjectDAO().getSamplePdfByAccessionNumber(samplePdf);
    }
}
