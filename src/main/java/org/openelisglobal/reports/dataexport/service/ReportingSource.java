package org.openelisglobal.reports.dataexport.service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;

public interface ReportingSource {
    String id();

    List<ReportingVariable> catalog();

    void validateConfiguration(ReportSourceConfig configuration);

    long write(Writer output, ExportSnapshot request) throws IOException;
}
