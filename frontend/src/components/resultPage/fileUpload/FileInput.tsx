import React, {
  useState,
  useEffect,
  useCallback,
  ChangeEvent,
  memo,
} from "react";
import { FileUploaderButton, Link } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFormData,
} from "../../utils/Utils";
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import config from "../../../config.json";
import {
  ScopedAttachment,
  attachmentVisibleOnRow,
  scopedAttachmentUploadUrl,
} from "../attachmentScope";

/**
 * OGC-811 — result-row attachment control, backed by the order-attachment
 * API (the same store Add Order and the unified Results page use). Uploads
 * are scoped to the row's analysis and result component, so a file attached
 * on one component never appears under its siblings; multiple files per row
 * are supported (the legacy inline result_file allowed a single image and
 * lived in a separate store nobody else could see).
 */
interface RowData {
  accessionNumber: string;
  analysisId?: string;
  testResultComponentId?: string;
  [key: string]: any;
}

interface CompactFileInputProps {
  data: RowData;
  /** legacy props kept for mount compatibility; no longer used */
  results?: unknown;
  setResultForm?: unknown;
}

const MAX_SIZE = 10 * 1024 * 1024;

const CompactFileInput: React.FC<CompactFileInputProps> = memo(({ data }) => {
  const [files, setFiles] = useState<ScopedAttachment[]>([]);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(false);

  const accessionNumber = data.accessionNumber;
  const analysisId = data.analysisId;
  const componentId = data.testResultComponentId;

  const loadFiles = useCallback(() => {
    if (!accessionNumber) {
      return;
    }
    getFromOpenElisServer(
      `/rest/order/${accessionNumber}/attachments`,
      (body: ScopedAttachment[]) => setFiles(Array.isArray(body) ? body : []),
    );
  }, [accessionNumber]);

  useEffect(loadFiles, [loadFiles]);

  const handleUpload = (e: ChangeEvent<HTMLInputElement>): void => {
    const file = e.target.files?.[0];
    if (!file || !accessionNumber) {
      return;
    }
    if (file.size > MAX_SIZE) {
      setError(true);
      return;
    }
    setError(false);
    setUploading(true);
    const formData = new FormData();
    formData.append("files", file, file.name);
    postToOpenElisServerFormData(
      scopedAttachmentUploadUrl(accessionNumber, analysisId, componentId),
      formData,
      (status: number) => {
        setUploading(false);
        if (status >= 200 && status < 300) {
          loadFiles();
        } else {
          setError(true);
        }
      },
    );
  };

  const visible = files.filter((file) =>
    attachmentVisibleOnRow(file, analysisId, componentId),
  );

  return (
    <div className="cds--form-item">
      <span
        className="cds--label"
        aria-hidden="true"
        style={{ display: "block" }}
      >
        &nbsp;
      </span>
      <FileUploaderButton
        labelText={<FormattedMessage id="label.button.uploadfile" />}
        accept={["image/jpeg", "image/png", "application/pdf", "image/tiff"]}
        multiple={false}
        disabled={uploading}
        onChange={handleUpload}
        disableLabelChanges
      />
      {visible.map((file) => (
        <div key={file.id}>
          <Link
            onClick={() =>
              window.open(
                `${config.serverBaseUrl}/rest/order/attachments/${file.id}/view`,
                "_blank",
              )
            }
            style={{ fontSize: "12px", cursor: "pointer" }}
          >
            {file.fileName}
          </Link>
        </div>
      ))}
      {error && (
        <div style={{ color: "#da1e28", fontSize: "12px" }}>
          <FormattedMessage id="label.results.attachments.uploadFailed" />
        </div>
      )}
    </div>
  );
});

export default CompactFileInput;
