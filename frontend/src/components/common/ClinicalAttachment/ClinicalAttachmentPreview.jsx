import React, { useEffect, useRef, useState } from "react";
import { Button, InlineNotification } from "@carbon/react";
import { Launch } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";

const MEDIA_TYPES = new Map(
  Object.entries({
    "data:image/jpeg": "image/jpeg",
    "data:image/png": "image/png",
    "data:application/pdf": "application/pdf",
  }),
);

/** Stored metadata never becomes HTML or a navigation URL. */
export default function ClinicalAttachmentPreview({
  attachment,
  ...buttonProps
}) {
  const intl = useIntl();
  const url = useRef(null);
  const [invalid, setInvalid] = useState(false);
  useEffect(() => {
    url.current = null;
    const storedType = attachment.fileType
      ?.split(";", 1)[0]
      .trim()
      .toLowerCase();
    const type = MEDIA_TYPES.get(storedType);
    if (!type) return;
    let objectUrl;
    try {
      const bytes = Uint8Array.from(atob(attachment.image), (char) =>
        char.charCodeAt(0),
      );
      objectUrl = URL.createObjectURL(new Blob([bytes], { type }));
      url.current = objectUrl;
    } catch {
      // Old malformed attachments remain stored, but cannot be opened as markup.
    }
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [attachment.fileType, attachment.image]);

  return (
    <>
      <Button
        {...buttonProps}
        renderIcon={Launch}
        onClick={() => {
          if (url.current) {
            setInvalid(false);
            window.open(url.current, "_blank", "noopener,noreferrer");
          } else setInvalid(true);
        }}
      >
        <FormattedMessage id="pathology.label.view" />
      </Button>
      {invalid && (
        <InlineNotification
          kind="error"
          hideCloseButton
          title={intl.formatMessage({
            id: "clinicalAttachment.previewUnavailable",
          })}
        />
      )}
    </>
  );
}
