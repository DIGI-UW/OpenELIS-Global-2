import React, { useState, useEffect } from "react";
import { FormGroup, FileUploader } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { toBase64 } from "../../utils/Utils";

const CompactFileInput = ({ data, results, setResultForm }) => {
  const [uploadedFile, setUploadedFile] = useState({});
  const handleUpload = async (evt) => {
    const file = evt.target.files?.[0];
    if (!file) return;

    const base64Content = await toBase64(file);
    const updatedResults = structuredClone(results);

    updatedResults.testResult[data.id].isModified = true;
    updatedResults.testResult[data.id].resultFile = {
      fileName: file.name,
      fileType: file.type,
      base64Content,
    };

    setResultForm(updatedResults);
    setUploadedFile((prev) => ({
      ...prev,
      [data.id]: updatedResults.testResult[data.id].resultFile,
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
