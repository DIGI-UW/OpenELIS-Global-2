import React, { useState } from "react";
import {
  FileUploader,
  Grid,
  Column,
  FormGroup,
  Stack,
  FileUploaderItem,
  FileUploaderDropContainer,
  FormLabel,
} from "@carbon/react";

const CompactFileInput = ({ data, results, setResultForm }) => {
  const [uploadedFiles, setUploadedFiles] = useState({});

  const handleUpload = async (files) => {
    const file = files[0];
    if (!file) return;

    const base64Content = await new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = (err) => reject(err);
    });

    const updatedResults = { ...results };
    if (!updatedResults.testResult[data.id].resultFiles) {
      updatedResults.testResult[data.id].resultFiles = [];
    }

    updatedResults.testResult[data.id].resultFiles.push({
      fileName: file.name,
      fileType: file.type,
      base64Content: base64Content,
    });

    setResultForm(updatedResults);
    setUploadedFiles((prev) => ({
      ...prev,
      [data.id]: updatedResults.testResult[data.id].resultFiles,
    }));
  };

  const handleDelete = (index) => {
    const updatedResults = { ...results };
    updatedResults.testResult[data.id].resultFiles.splice(index, 1);

    setResultForm(updatedResults);
    setUploadedFiles((prev) => ({
      ...prev,
      [data.id]: updatedResults.testResult[data.id].resultFiles,
    }));
  };

  return (
    <Grid narrow>
      <FormGroup legendText="">
        <Stack gap={5}>
          <FormLabel>Upload Results</FormLabel>
          <FileUploaderDropContainer
            accept={["image/jpeg", "image/png", "application/pdf"]}
            labelText="Drag and drop files or click to upload"
            multiple={false}
            onAddFiles={(e, { addedFiles }) => handleUpload(addedFiles)}
          />
          <div>
            <Stack gap={1}>
              {uploadedFiles[data.id]?.map((file, index) => (
                <FileUploaderItem
                  key={index}
                  name={file.fileName}
                  status="complete"
                  onDelete={() => handleDelete(index)}
                />
              ))}
            </Stack>
          </div>
        </Stack>
      </FormGroup>
    </Grid>
  );
};

export default CompactFileInput;
