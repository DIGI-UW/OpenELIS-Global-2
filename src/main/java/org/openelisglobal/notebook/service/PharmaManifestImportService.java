package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import org.openelisglobal.notebook.form.PharmaManifestImportForm;
import org.openelisglobal.notebook.service.PharmaManifestImportService.ParseError;

public interface PharmaManifestImportService {

    record PharmaManifestRow(int rowNumber, String groupId, String sampleType, int numOfSamples, String chemicalName,
            String grade, String lotNumber, String dateOfManufacture, String expiryOrRetestDate,
            String storageCondition, String owner, String patientId, String clinicalTrialNumber, String consentStatus,
            String notes) {
    }

    record ParseError(int rowNumber, String column, String message) {
    }

    record ParsedManifest(List<PharmaManifestRow> rows, List<ParseError> errors) {
    }

    record PharmaManifestImportResult(int totalRequested, int totalCreated, List<ParseError> errors,
            List<String> createdAccessionNumbers) {
    }

    ParsedManifest parseManifestCsv(InputStream csvInput, PharmaManifestImportForm columnMapping);

    List<ParseError> validateSampleTypes(ParsedManifest manifest);

    PharmaManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);
}
