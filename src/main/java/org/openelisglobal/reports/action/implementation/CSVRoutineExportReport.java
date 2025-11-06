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
package org.openelisglobal.reports.action.implementation;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * @author pahill (pahill@uw.edu)
 * @since Mar 17, 2011
 */
public abstract class CSVRoutineExportReport extends Report implements JRDataSource {

    /**
     * @see org.openelisglobal.reports.action.implementation.Report#getContentType()
     */
    @Override
    public String getContentType() {
        if (errorFound) {
            return super.getContentType();
        } else {
            return "text/plain; charset=UTF-8";
        }
    }

    /**
     * Either we generate a PDF with an error message or we generate data for a CSV
     * file, but if your expecting to get JasperReport data from this class, it
     * won't work.
     *
     * @see org.openelisglobal.reports.action.implementation.IReportCreator#getReportDataSource()
     */
    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("initializeReport not called first");
        }
        if (errorFound) {
            return new JRBeanCollectionDataSource(errorMsgs);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * This Report can not be sent to JasperReports
     *
     * @see net.sf.jasperreports.engine.JRDataSource#getFieldValue(net.sf.jasperreports.engine.JRField)
     */
    @Override
    public Object getFieldValue(JRField arg0) throws JRException {
        throw new UnsupportedOperationException();
    }

    /**
     * This Report can not be sent to JasperReports
     *
     * @see net.sf.jasperreports.engine.JRDataSource#next()
     */
    @Override
    public boolean next() throws JRException {
        throw new UnsupportedOperationException();
    }
}
