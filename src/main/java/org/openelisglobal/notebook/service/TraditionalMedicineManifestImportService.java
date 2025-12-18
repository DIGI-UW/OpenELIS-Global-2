package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import org.openelisglobal.notebook.form.TraditionalMedicineManifestImportForm;

public interface TraditionalMedicineManifestImportService {

    record TraditionalMedicineManifestRow(int rowNumber, String sampleId, String sampleCategory, String sourceType,
            String originLocation, String collectionSite, String collectionDate, String collectedBy, String localName,
            String scientificName, String species, String plantPart, String sampleCondition, String intendedUse,
            int numOfSamples, String notes) {
    }

    record ParseError(int rowNumber, String column, String message) {
    }

    record ParsedManifest(List<TraditionalMedicineManifestRow> rows, List<ParseError> errors) {
    }

    record TraditionalMedicineManifestImportResult(int totalRequested, int totalCreated, List<ParseError> errors,
            List<String> createdAccessionNumbers) {
    }

    ParsedManifest parseManifestCsv(InputStream csvInput, TraditionalMedicineManifestImportForm columnMapping);

    List<ParseError> validateSampleData(ParsedManifest manifest);

    TraditionalMedicineManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            String sysUserId);
}
