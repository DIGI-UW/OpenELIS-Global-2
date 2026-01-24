package org.openelisglobal.reports.adhoc.service;

import java.util.List;
import org.openelisglobal.reports.adhoc.dto.AvailableFieldsDTO;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO;

public interface AdHocFieldDefinitionService {

    AvailableFieldsDTO getAvailableFields();

    List<ReportFieldDTO> getPatientFields();

    List<ReportFieldDTO> getSampleFields();

    ReportFieldDTO getFieldById(String fieldId);

    boolean validateFieldIds(List<String> fieldIds);
}
