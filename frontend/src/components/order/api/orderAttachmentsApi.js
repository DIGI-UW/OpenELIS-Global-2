import { postToOpenElisServerFormData } from "../../utils/Utils";

export const uploadOrderAttachments = (labNumber, files, onStatus) => {
  const formData = new FormData();
  files.forEach((file) => formData.append("files", file, file.name));
  postToOpenElisServerFormData(
    `/rest/order/${encodeURIComponent(labNumber)}/attachments`,
    formData,
    onStatus,
  );
};
