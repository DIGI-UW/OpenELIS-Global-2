import {
  translateManifestImportMessage,
  translateManifestImportMessages,
} from "./manifestImportErrorMessages";

const formatMessage = ({ defaultMessage }, values = {}) =>
  Object.entries(values).reduce((result, [key, value]) => {
    return result.replace(new RegExp(`\\{${key}\\}`, "g"), String(value));
  }, defaultMessage);

describe("manifestImportErrorMessages", () => {
  it("translates duplicate sample ids in the manifest", () => {
    expect(
      translateManifestImportMessage(
        "Duplicate sample ID in manifest: BIO-001",
        formatMessage,
      ),
    ).toBe(
      'This file contains the Sample ID "BIO-001" more than once. Keep only one row for each sample.',
    );
  });

  it("translates existing sample ids in the system", () => {
    expect(
      translateManifestImportMessage(
        "Sample ID already exists: BIO-002",
        formatMessage,
      ),
    ).toBe('Sample ID "BIO-002" is already registered in the system.');
  });

  it("translates invalid sample types into user language", () => {
    expect(
      translateManifestImportMessage(
        "Invalid sample type: old type",
        formatMessage,
      ),
    ).toBe(
      'Sample type "old type" is not recognized. Please correct it or use an existing sample type.',
    );
  });

  it("translates raw duplicate-key errors with sample context", () => {
    expect(
      translateManifestImportMessage(
        "Sample 'BIO-003': duplicate key value violates unique constraint \"type_of_sample_local_abbreviation_key\"",
        formatMessage,
      ),
    ).toBe(
      'Sample "BIO-003": This sample type already exists in the system. Please validate the file again and retry the import.',
    );
  });

  it("translates arrays of backend row errors", () => {
    expect(
      translateManifestImportMessages(
        [
          "Duplicate sample ID in manifest: BIO-001",
          "Sample 'BIO-004': duplicate key value violates unique constraint \"sample_item_external_id_key\"",
        ],
        formatMessage,
      ),
    ).toEqual([
      'This file contains the Sample ID "BIO-001" more than once. Keep only one row for each sample.',
      'Sample "BIO-004": A sample with the same Sample ID already exists in the system.',
    ]);
  });
});
