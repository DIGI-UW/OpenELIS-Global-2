import React, { useState, useEffect } from "react";
import {
  Grid,
  FormGroup,
  Stack,
  FileUploaderItem,
  FileUploaderDropContainer,
  FormLabel,
} from "@carbon/react";

const CompactFileInput = ({ data, results, setResultForm }) => {
  const [uploadedFile, setUploadedFile] = useState({});

  const toBase64 = (file) =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = (err) => reject(err);
    });

  const handleUpload = async (files) => {
    const file = files[0];
    if (!file) return;

    const base64Content = await toBase64(file);

    const updatedResults = structuredClone(results);

    const testResultIndex = updatedResults.testResult.findIndex(
      (item) => item.accessionNumber === data.accessionNumber,
    );

    if (testResultIndex === -1) {
      console.error(
        `Test result with accessionNumber ${data.accessionNumber} not found`,
      );
      return;
    }

    updatedResults.testResult[testResultIndex].resultFile = {
      fileName: file.name,
      fileType: file.type,
      base64Content,
    };

    setResultForm(updatedResults);

    setUploadedFile((prev) => ({
      ...prev,
      [data.accessionNumber]:
        updatedResults.testResult[testResultIndex].resultFile,
    }));
  };

  const handleDelete = () => {
    const updatedResults = structuredClone(results);

    const testResultIndex = updatedResults.testResult.findIndex(
      (item) => item.accessionNumber === data.accessionNumber,
    );

    if (testResultIndex === -1) return;

    updatedResults.testResult[testResultIndex].resultFile = null;

    setResultForm(updatedResults);

    setUploadedFile((prev) => ({
      ...prev,
      [data.accessionNumber]: null,
    }));
  };

  useEffect(() => {
    const testResult = results.testResult.find(
      (item) => item.accessionNumber === data.accessionNumber,
    );

    if (testResult?.resultFile) {
      setUploadedFile((prev) => ({
        ...prev,
        [data.accessionNumber]: testResult.resultFile,
      }));
    } else {
      setUploadedFile((prev) => ({
        ...prev,
        [data.accessionNumber]: null,
      }));
    }
  }, [results, data.accessionNumber]);

  const file = uploadedFile[data.accessionNumber];

  return (
    <Grid narrow>
      <FormGroup legendText="">
        <Stack gap={5}>
          <FormLabel>Upload Results</FormLabel>

          <FileUploaderDropContainer
            accept={["image/jpeg", "image/png", "application/pdf"]}
            labelText="Drag and drop a file or click to upload"
            multiple={false}
            onAddFiles={(e, { addedFiles }) => handleUpload(addedFiles)}
          />

          <div>
            <Stack gap={1}>
              {file && (
                <FileUploaderItem
                  key={file.fileName}
                  name={file.fileName}
                  status="complete"
                  onDelete={handleDelete}
                />
              )}
            </Stack>
          </div>
        </Stack>
      </FormGroup>
    </Grid>
  );
};

export default CompactFileInput;
