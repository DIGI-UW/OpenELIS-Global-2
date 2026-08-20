export const filenameFromContentDisposition = (value, fallback) => {
  const match = String(value || "").match(/filename="?([^";]+)"?/i);
  return match?.[1] || fallback;
};

export const downloadAttachment = (blob, filename) => {
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
};
