import React, { useState, useEffect } from "react";
import { FormGroup, FileUploader } from "@carbon/react";
import { FormattedMessage } from "react-intl";

const CompactFileInput = ({ data, results, setResultForm }) => {
  const [uploadedFile, setUploadedFile] = useState({});

  const toBase64 = (file) =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = (err) => reject(err);
    });

  const handleUpload = async (evt) => {
    const file = evt.target.files?.[0];
    if (!file) return;

    const base64Content = await toBase64(file);
    const updatedResults = structuredClone(results);

    const testResultIndex = updatedResults.testResult.findIndex(
      (item) => item.accessionNumber === data.accessionNumber,
    );
    if (testResultIndex === -1) return;

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

  useEffect(() => {
    const testResult = results.testResult.find(
      (item) => item.accessionNumber === data.accessionNumber,
    );
    setUploadedFile((prev) => ({
      ...prev,
      [data.accessionNumber]: testResult?.resultFile || null,
    }));
  }, [results, data.accessionNumber]);

  const file = uploadedFile[data.accessionNumber];

  return (
    <>
      <FileUploader
        labelDescription={
          <FormattedMessage id="notebook.attachments.uploadPrompt" />
        }
        buttonLabel={<FormattedMessage id="label.button.uploadfile" />}
        filenameStatus={file ? "complete" : ""}
        accept={["image/jpeg", "image/png", "application/pdf"]}
        multiple={false}
        onChange={handleUpload}
      />
    </>
  );
};

export default CompactFileInput;
