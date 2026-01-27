/**
 * Logo Upload Section Component
 *
 * Handles logo file upload with validation and preview
 *
 * Task Reference: T032
 */

import React, { useState } from "react";
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
import { postToOpenElisServerFormData } from "../../../../components/utils/Utils";
import { TrashCan } from "@carbon/icons-react";
import { removeLogo } from "../../../utils/BrandingUtils";
import { Modal } from "@carbon/react";

function LogoUploadSection({
  type,
  currentLogoUrl,
  onLogoUploaded,
  onLogoRemoved,
  useHeaderLogoForLogin = false,
  onUseHeaderLogoChange,
}) {
  const intl = useIntl();
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(currentLogoUrl);
  const [error, setError] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [showRemoveConfirm, setShowRemoveConfirm] = useState(false);

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

    // Create preview
    const reader = new FileReader();
    reader.onloadend = () => {
      setPreview(reader.result);
    };
    reader.readAsDataURL(selectedFile);
  };

  const handleUpload = () => {
    if (!file) return;

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
          // Get logo URL from response or construct it
          const logoUrl = `/rest/site-branding/logo/${type}`;
          setPreview(logoUrl);
          if (onLogoUploaded) {
            onLogoUploaded(logoUrl);
          }
        } else {
          setError(intl.formatMessage({ id: "site.branding.save.error" }));
        }
      },
    );
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
                kind="danger"
                size="sm"
                renderIcon={TrashCan}
                onClick={handleRemove}
                style={{ marginLeft: "1rem" }}
              >
                <FormattedMessage id="site.branding.remove.logo" />
              </Button>
            </div>
          )}

          {/* Hide file uploader when login logo uses header logo */}
          {!(type === "login" && useHeaderLogoForLogin) && (
            <FileUploader
              buttonLabel={intl.formatMessage({
                id: "site.branding.upload.logo",
              })}
              iconDescription={intl.formatMessage({
                id: "site.branding.upload.logo",
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

          {file && !preview?.startsWith("/rest/site-branding/logo/") && (
            <Button
              onClick={handleUpload}
              disabled={isUploading}
              style={{ marginTop: "1rem" }}
            >
              <FormattedMessage id="site.branding.upload.logo" />
            </Button>
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
}

export default LogoUploadSection;
