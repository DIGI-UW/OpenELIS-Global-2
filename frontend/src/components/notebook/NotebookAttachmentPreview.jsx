import React from "react";
import ClinicalAttachmentPreview from "../common/ClinicalAttachment/ClinicalAttachmentPreview";

export default function NotebookAttachmentPreview({ file, ...props }) {
  const encoded = file.base64File;
  const separator = encoded?.indexOf(";base64,") ?? -1;
  const type = separator >= 0 ? encoded.slice(0, separator) : file.fileType;
  return (
    <ClinicalAttachmentPreview
      {...props}
      allowText
      attachment={{
        image: separator >= 0 ? encoded.slice(separator + 8) : file.fileData,
        fileType: type?.startsWith("data:") ? type : `data:${type}`,
      }}
    />
  );
}
