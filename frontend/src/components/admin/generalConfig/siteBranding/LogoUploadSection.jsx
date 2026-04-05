/**
 * Logo Upload Section Component
 *
 * Handles logo file upload with validation and preview
 *
 * Task Reference: T032
 */

import React, {
  useState,
  useEffect,
  useImperativeHandle,
  forwardRef,
} from "react";
import {
  Grid,
  Column,
  Section,
  FileUploader,
  Button,
  InlineNotification,
  Checkbox,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerFormData } from "../../../utils/Utils";
import { TrashCan } from "@carbon/icons-react";
import { removeLogo } from "../../../utils/BrandingUtils";
import config from "../../../../config.json";
import { Modal } from "@carbon/react";

const LogoUploadSection = forwardRef(function LogoUploadSection(
  {
    type,
    currentLogoUrl,
    onLogoUploaded,
    onLogoRemoved,
    onFileSelected,
    useHeaderLogoForLogin = false,
    onUseHeaderLogoChange,
  },
  ref,
) {
  const intl = useIntl();
  const [file, setFile] = useState(null);
  // Add serverBaseUrl prefix for REST endpoints (like Header.js does)
  const getDisplayUrl = (url) => {
    if (!url) return null;
    if (url.startsWith("data:")) return url; // base64 preview
    if (url.startsWith(config.serverBaseUrl)) return url; // already prefixed
    return `${config.serverBaseUrl}${url}?v=${Date.now()}`; // add prefix and cache-busting
  };
  const [preview, setPreview] = useState(getDisplayUrl(currentLogoUrl));
  const [error, setError] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [showRemoveConfirm, setShowRemoveConfirm] = useState(false);

  // Update preview when currentLogoUrl prop changes
  useEffect(() => {
    const newPreviewUrl = getDisplayUrl(currentLogoUrl);
    console.debug(`LogoUploadSection [${type}] - currentLogoUrl changed:`, {
      currentLogoUrl,
      newPreviewUrl,
    });
    setPreview(newPreviewUrl);
  }, [currentLogoUrl, type]);

  // Expose upload function to parent via ref
  useImperativeHandle(ref, () => ({
    uploadFile: () => {
      return new Promise((resolve, reject) => {
        if (!file) {
          resolve({ success: true, noFile: true });
          return;
        }

        setIsUploading(true);
        setError(null);

        const formData = new FormData();
        formData.append("file", file);

        postToOpenElisServerFormData(
          `/rest/site-branding/logo/${type}`,
          formData,
          (status) => {
            setIsUploading(false);
            if (status === 200 || status === 201) {
              const logoUrl = `/rest/site-branding/logo/${type}`;
              setPreview(getDisplayUrl(logoUrl));
              setFile(null);
              if (onLogoUploaded) {
                onLogoUploaded(logoUrl);
              }
              resolve({ success: true });
            } else {
              setError(intl.formatMessage({ id: "site.branding.save.error" }));
              reject(new Error("Upload failed"));
            }
          },
        );
      });
    },
    hasPendingFile: () => !!file,
  }));

  const handleFileChange = (event) => {
    const selectedFile = event.target.files?.[0];
    if (!selectedFile) return;

    // Validate file format
    const allowedFormats = [
      "image/png",
      "image/svg+xml",
      "image/jpeg",
      "image/jpg",
    ];
    if (!allowedFormats.includes(selectedFile.type)) {
      setError(intl.formatMessage({ id: "site.branding.file.format.error" }));
      return;
    }

    // Validate file size (2MB)
    const maxSize = 2 * 1024 * 1024; // 2MB
    if (selectedFile.size > maxSize) {
      setError(intl.formatMessage({ id: "site.branding.file.size.error" }));
      return;
    }

    setError(null);
    setFile(selectedFile);

    // Notify parent that a file was selected
    if (onFileSelected) {
      onFileSelected(selectedFile, type);
    }

    // Create preview
    const reader = new FileReader();
    reader.onloadend = () => {
      setPreview(reader.result);
    };
    reader.readAsDataURL(selectedFile);
  };

  const handleRemove = () => {
    // Task Reference: T064 - Show confirmation dialog before removal
    setShowRemoveConfirm(true);
  };

  const confirmRemove = () => {
    setShowRemoveConfirm(false);
    setError(null);

    removeLogo(type, async (response, extraParams) => {
      try {
        const status = response.status || 200;
        if (status === 200 || status === 204) {
          // Parse response body if available
          let responseData = null;
          if (response.ok) {
            try {
              responseData = await response.json();
            } catch (e) {
              // Response might not have JSON body
            }
          }

          setFile(null);
          setPreview(null);
          if (onLogoRemoved) {
            onLogoRemoved();
          }
        } else {
          setError(intl.formatMessage({ id: "site.branding.error.remove" }));
        }
      } catch (error) {
        console.error("Error removing logo:", error);
        setError(intl.formatMessage({ id: "site.branding.error.remove" }));
      }
    });
  };

  const cancelRemove = () => {
    setShowRemoveConfirm(false);
  };

  const getTitleKey = () => {
    switch (type) {
      case "header":
        return "site.branding.header.logo";
      case "login":
        return "site.branding.login.logo";
      case "favicon":
        return "site.branding.favicon";
      default:
        return "site.branding.upload.logo";
    }
  };

  const getDescriptionKey = () => {
    switch (type) {
      case "header":
        return "site.branding.header.logo.description";
      case "login":
        return "site.branding.login.logo.description";
      case "favicon":
        return "site.branding.favicon.description";
      default:
        return "";
    }
  };

  const getUploadButtonKey = () => {
    switch (type) {
      case "header":
        return "site.branding.upload.header.logo";
      case "login":
        return "site.branding.upload.login.logo";
      case "favicon":
        return "site.branding.upload.favicon";
      default:
        return "site.branding.upload.logo";
    }
  };

  const getRemoveButtonKey = () => {
    switch (type) {
      case "header":
        return "site.branding.remove.header.logo";
      case "login":
        return "site.branding.remove.login.logo";
      case "favicon":
        return "site.branding.remove.favicon";
      default:
        return "site.branding.remove.logo";
    }
  };

  return (
    <Section>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <h3>
            <FormattedMessage id={getTitleKey()} />
          </h3>
          {getDescriptionKey() && (
            <p>
              <FormattedMessage id={getDescriptionKey()} />
            </p>
          )}

          {error && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({ id: "error.title" })}
              subtitle={error}
              onClose={() => setError(null)}
            />
          )}

          {/* "Use same logo as header" checkbox for login logo */}
          {type === "login" && onUseHeaderLogoChange && (
            <div style={{ marginBottom: "1rem" }}>
              <Checkbox
                id="use-header-logo-for-login"
                labelText={intl.formatMessage({
                  id: "site.branding.use.header.logo.for.login",
                })}
                checked={useHeaderLogoForLogin}
                onChange={(event) => {
                  if (onUseHeaderLogoChange) {
                    onUseHeaderLogoChange(event.target.checked);
                  }
                }}
              />
            </div>
          )}

          {preview && !(type === "login" && useHeaderLogoForLogin) && (
            <div style={{ marginBottom: "1rem" }}>
              <img
                src={preview}
                alt={intl.formatMessage({ id: getTitleKey() })}
                style={{
                  maxWidth: "200px",
                  maxHeight: "100px",
                  objectFit: "contain",
                }}
              />
              <Button
                data-testid="remove-logo-button"
                kind="danger"
                size="sm"
                renderIcon={TrashCan}
                onClick={handleRemove}
                style={{ marginLeft: "1rem" }}
              >
                <FormattedMessage id={getRemoveButtonKey()} />
              </Button>
            </div>
          )}

          {/* Hide file uploader when login logo uses header logo */}
          {!(type === "login" && useHeaderLogoForLogin) && (
            <FileUploader
              buttonLabel={intl.formatMessage({
                id: getUploadButtonKey(),
              })}
              iconDescription={intl.formatMessage({
                id: getUploadButtonKey(),
              })}
              filenameStatus={file ? "complete" : undefined}
              accept={["image/png", "image/svg+xml", "image/jpeg", "image/jpg"]}
              multiple={false}
              onChange={handleFileChange}
              disabled={isUploading}
            />
          )}

          {type === "login" && useHeaderLogoForLogin && (
            <p style={{ marginTop: "1rem", fontStyle: "italic" }}>
              <FormattedMessage id="site.branding.login.using.header.logo" />
            </p>
          )}

          {/* Show pending upload indicator */}
          {file && preview?.startsWith("data:") && (
            <p
              style={{
                marginTop: "0.5rem",
                fontStyle: "italic",
                color: "#0f62fe",
              }}
            >
              <FormattedMessage
                id="site.branding.file.pending"
                defaultMessage="File selected - click Save Changes to upload"
              />
            </p>
          )}

          {/* Confirmation Modal for Logo Removal */}
          <Modal
            open={showRemoveConfirm}
            modalHeading={intl.formatMessage({
              id: "site.branding.confirm.remove",
            })}
            primaryButtonText={intl.formatMessage({
              id: "label.button.remove",
            })}
            secondaryButtonText={intl.formatMessage({
              id: "label.button.cancel",
            })}
            onRequestClose={cancelRemove}
            onRequestSubmit={confirmRemove}
            danger
          >
            <p>
              {intl.formatMessage({
                id: "site.branding.confirm.remove.message",
              })}
            </p>
          </Modal>
        </Column>
      </Grid>
    </Section>
  );
});

export default LogoUploadSection;
