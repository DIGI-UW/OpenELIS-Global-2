/**
 * Site Branding Configuration Component
 *
 * Allows administrators to configure site branding including logos and colors
 *
 * Task Reference: T021
 */

import React, { useState, useEffect, useContext, useRef } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Button,
  Loading,
  Modal,
  InlineLoading,
} from "@carbon/react";
import {
  getBranding,
  updateBranding,
  resetBranding,
} from "../../../utils/BrandingUtils";
import { NotificationContext } from "../../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import PageBreadCrumb from "../../../common/PageBreadCrumb";
import LogoUploadSection from "./LogoUploadSection";
import ColorPickerSection from "./ColorPickerSection";
import config from "../../../../config.json";

function SiteBrandingConfig() {
  const intl = useIntl();
  const history = useHistory();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [branding, setBranding] = useState(null);
  const [savedBranding, setSavedBranding] = useState(null); // Track saved state
  const [showResetConfirm, setShowResetConfirm] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [hasPendingFiles, setHasPendingFiles] = useState(false);
  const initialBrandingRef = useRef(null);

  // Refs for LogoUploadSection components to trigger uploads
  const headerLogoRef = useRef(null);
  const loginLogoRef = useRef(null);
  const faviconRef = useRef(null);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "sidenav.label.admin.formEntry.siteInfoconfig",
      link: "/MasterListsPage#SiteInformationMenu",
    },
    { label: "site.branding.title", link: "#SiteBrandingMenu" },
  ];

  useEffect(() => {
    loadBranding();
  }, []);

  const loadBranding = () => {
    setIsLoading(true);
    getBranding((response) => {
      console.debug("loadBranding response:", response);
      console.debug("Logo URLs in response:", {
        headerLogoUrl: response?.headerLogoUrl,
        loginLogoUrl: response?.loginLogoUrl,
        faviconUrl: response?.faviconUrl,
      });
      if (response) {
        setBranding(response);
        setSavedBranding(JSON.parse(JSON.stringify(response))); // Deep copy for comparison
        initialBrandingRef.current = JSON.parse(JSON.stringify(response));
        // Apply colors immediately
        applyBrandingColors(response);
        // Update favicon if custom favicon exists
        if (response.faviconUrl) {
          updateFavicon(response.faviconUrl);
        }
      } else {
        // Handle error - use default values
        const defaultBranding = {
          headerColor: "#295785",
          primaryColor: "#0f62fe",
          secondaryColor: "#393939",
          colorMode: "light",
          useHeaderLogoForLogin: false,
        };
        setBranding(defaultBranding);
        setSavedBranding(JSON.parse(JSON.stringify(defaultBranding)));
        initialBrandingRef.current = JSON.parse(
          JSON.stringify(defaultBranding),
        );
        // Apply default colors
        applyBrandingColors(defaultBranding);
      }
      setIsLoading(false);
      setHasUnsavedChanges(false);
    });
  };

  // Task Reference: T073 - Detect form state changes
  useEffect(() => {
    if (!branding) {
      // No branding loaded yet, no changes
      setHasUnsavedChanges(false);
      return;
    }

    if (!savedBranding) {
      // Branding loaded but no saved state yet - wait for it
      // However, if branding exists, there might be unsaved changes
      // Set to false initially, will be recalculated when savedBranding loads
      setHasUnsavedChanges(false);
      return;
    }

    // Compare only the fields that can be changed via the save button
    // Exclude logo URLs as they are managed separately via upload endpoints
    const compareFields = (obj) => {
      if (!obj) return {};
      return {
        id: obj.id || null,
        headerColor: (obj.headerColor || "").trim().toLowerCase(),
        primaryColor: (obj.primaryColor || "").trim().toLowerCase(),
        secondaryColor: (obj.secondaryColor || "").trim().toLowerCase(),
        colorMode: (obj.colorMode || "").trim().toLowerCase(),
        useHeaderLogoForLogin: Boolean(obj.useHeaderLogoForLogin),
      };
    };

    const currentFields = compareFields(branding);
    const savedFields = compareFields(savedBranding);
    const hasChanges =
      JSON.stringify(currentFields) !== JSON.stringify(savedFields);

    // Debug logging (remove in production)
    if (hasChanges) {
      console.debug("Unsaved changes detected:", {
        current: currentFields,
        saved: savedFields,
      });
    }

    setHasUnsavedChanges(hasChanges);
  }, [branding, savedBranding]);

  // Task Reference: T073 - Warn user when navigating away with unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (hasUnsavedChanges) {
        e.preventDefault();
        e.returnValue = intl.formatMessage({
          id: "site.branding.unsaved.changes",
        });
        return e.returnValue;
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [hasUnsavedChanges, intl]);

  // Task Reference: T046 - Update favicon in document head
  // Apply branding colors to the DOM immediately
  const applyBrandingColors = (brandingData) => {
    if (!brandingData) {
      console.warn("applyBrandingColors called with null/undefined data");
      return;
    }

    const root = document.documentElement;

    console.debug("Applying branding colors to DOM:", {
      header: brandingData.headerColor,
      primary: brandingData.primaryColor,
      secondary: brandingData.secondaryColor,
    });

    if (brandingData.headerColor) {
      root.style.setProperty(
        "--site-branding-header",
        brandingData.headerColor,
      );
      console.debug("Set --site-branding-header to:", brandingData.headerColor);
    }
    if (brandingData.primaryColor) {
      root.style.setProperty("--cds-interactive-01", brandingData.primaryColor);
      root.style.setProperty(
        "--site-branding-primary",
        brandingData.primaryColor,
      );
      console.debug(
        "Set --cds-interactive-01 and --site-branding-primary to:",
        brandingData.primaryColor,
      );
    }
    if (brandingData.secondaryColor) {
      root.style.setProperty(
        "--cds-interactive-02",
        brandingData.secondaryColor,
      );
      root.style.setProperty(
        "--site-branding-secondary",
        brandingData.secondaryColor,
      );
      console.debug(
        "Set --cds-interactive-02 and --site-branding-secondary to:",
        brandingData.secondaryColor,
      );
    }

    // Verify the properties were set
    const computedPrimary = getComputedStyle(root)
      .getPropertyValue("--cds-interactive-01")
      .trim();
    console.debug(
      "Verified CSS property --cds-interactive-01 is now:",
      computedPrimary,
    );
  };

  const updateFavicon = (faviconUrl) => {
    // Remove existing favicon links
    const existingLinks = document.querySelectorAll('link[rel*="icon"]');
    existingLinks.forEach((link) => link.remove());

    // Add new favicon link
    const link = document.createElement("link");
    link.rel = "icon";
    link.type = "image/x-icon";
    link.href = `${config.serverBaseUrl}${faviconUrl}`;
    document.head.appendChild(link);
  };

  const resetFavicon = () => {
    // Remove existing favicon links
    const existingLinks = document.querySelectorAll('link[rel*="icon"]');
    existingLinks.forEach((link) => link.remove());

    // Add default favicon link
    const link = document.createElement("link");
    link.rel = "icon";
    link.href = "../images/favicon-16x16.png";
    document.head.appendChild(link);
  };

  // Handler for when a file is selected in LogoUploadSection
  const handleFileSelected = (file, type) => {
    setHasPendingFiles(true);
  };

  const handleSave = async () => {
    if (!branding || isSaving) return;

    // Task Reference: T097 - Disable form during save operation
    setIsSaving(true);

    // Prepare data for sending - ensure colors are always valid
    // Exclude logo URLs as they are managed separately via upload endpoints
    // Colors must be provided as database requires NOT NULL
    const dataToSend = {
      id: branding.id,
      headerColor: branding.headerColor?.trim() || "#295785",
      primaryColor: branding.primaryColor?.trim() || "#0f62fe",
      secondaryColor: branding.secondaryColor?.trim() || "#393939",
      colorMode: branding.colorMode?.trim() || "light",
      useHeaderLogoForLogin: branding.useHeaderLogoForLogin || false,
      // Do not include headerLogoUrl, loginLogoUrl, or faviconUrl
      // These are managed via separate logo upload endpoints
    };

    // Save branding configuration FIRST (including useHeaderLogoForLogin flag)
    // This must happen before logo uploads so the backend has correct state
    updateBranding(dataToSend, async (status, errorMessage, responseData) => {
      if (status !== 200 && status !== 201) {
        setIsSaving(false);
        console.error("Save failed:", { status, errorMessage, dataToSend });
        const errorText = errorMessage
          ? `${intl.formatMessage({ id: "site.branding.save.error" })}: ${errorMessage}`
          : intl.formatMessage({ id: "site.branding.save.error" });
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: errorText,
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        return;
      }

      // Now upload any pending logo files AFTER branding config is saved
      // Upload sequentially to avoid race conditions with DB updates
      try {
        if (headerLogoRef.current?.hasPendingFile()) {
          console.debug("Uploading header logo...");
          await headerLogoRef.current.uploadFile();
          console.debug("Header logo uploaded successfully");
        }
        if (loginLogoRef.current?.hasPendingFile()) {
          console.debug("Uploading login logo...");
          await loginLogoRef.current.uploadFile();
          console.debug("Login logo uploaded successfully");
        }
        if (faviconRef.current?.hasPendingFile()) {
          console.debug("Uploading favicon...");
          await faviconRef.current.uploadFile();
          console.debug("Favicon uploaded successfully");
        }

        // Reset pending files state
        setHasPendingFiles(false);
      } catch (error) {
        console.error("Error uploading logos:", error);
        // Don't return early - continue to loadBranding to show any successful uploads
        // The individual upload errors are already shown in their respective components
      }

      // All saves complete - finalize
      setIsSaving(false);

      // Task Reference: T074 - Re-fetch branding config after save to ensure consistency
      // Apply colors immediately from the data we sent
      console.debug(
        "Save successful. Applying colors immediately from sent data:",
        dataToSend,
      );
      applyBrandingColors(dataToSend);

      // Reload from server to get complete state including logo URLs
      loadBranding();

      // Dispatch event to notify Header and other components to reload branding
      window.dispatchEvent(new CustomEvent("branding-updated"));

      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "site.branding.save.success" }),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);
    });
  };

  const handleCancel = () => {
    if (hasUnsavedChanges) {
      // Show confirmation if there are unsaved changes
      if (
        window.confirm(
          intl.formatMessage({ id: "site.branding.unsaved.changes" }),
        )
      ) {
        // Discard changes - reload from saved state
        if (savedBranding) {
          setBranding(JSON.parse(JSON.stringify(savedBranding)));
          setHasUnsavedChanges(false);
        } else {
          loadBranding();
        }
      }
    } else {
      // No changes, just reload
      loadBranding();
    }
  };

  const handleReset = () => {
    setShowResetConfirm(true);
  };

  const confirmReset = () => {
    setShowResetConfirm(false);
    setIsLoading(true);

    resetBranding((status) => {
      setIsLoading(false);
      if (status === 200 || status === 201) {
        // Reload branding after reset
        loadBranding();

        // Reset CSS custom properties to defaults
        document.documentElement.style.setProperty(
          "--site-branding-header",
          "#295785",
        );
        document.documentElement.style.setProperty(
          "--cds-interactive-01",
          "#0f62fe",
        );
        document.documentElement.style.setProperty(
          "--cds-interactive-02",
          "#393939",
        );
        document.documentElement.style.setProperty(
          "--site-branding-primary",
          "#0f62fe",
        );
        document.documentElement.style.setProperty(
          "--site-branding-secondary",
          "#393939",
        );

        // Reset favicon
        resetFavicon();

        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({ id: "site.branding.reset.success" }),
          kind: NotificationKinds.success,
        });
        setNotificationVisible(true);
      } else {
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({ id: "site.branding.reset.error" }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
      }
    });
  };

  const cancelReset = () => {
    setShowResetConfirm(false);
  };

  if (isLoading) {
    return (
      <div className="adminPageContent">
        <Loading
          description={intl.formatMessage({ id: "loading.description" })}
        />
      </div>
    );
  }

  return (
    <div className="adminPageContent">
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="site.branding.title" />
            </Heading>
            <p>
              <FormattedMessage id="site.branding.description" />
            </p>
          </Section>
        </Column>
      </Grid>

      {/* Logo Upload Sections */}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <LogoUploadSection
            ref={headerLogoRef}
            type="header"
            currentLogoUrl={branding?.headerLogoUrl}
            onFileSelected={handleFileSelected}
            onLogoUploaded={(url) => {
              // Don't call loadBranding() here - handleSave calls it once after all uploads complete
              // Just dispatch event to notify Header to reload branding
              window.dispatchEvent(new CustomEvent("branding-updated"));
            }}
            onLogoRemoved={() => {
              // Logo removal is saved immediately, so reload from server to sync state
              loadBranding();
              // Dispatch event to notify Header to reload branding
              window.dispatchEvent(new CustomEvent("branding-updated"));
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <LogoUploadSection
            ref={loginLogoRef}
            type="login"
            currentLogoUrl={branding?.loginLogoUrl}
            useHeaderLogoForLogin={branding?.useHeaderLogoForLogin || false}
            onFileSelected={handleFileSelected}
            onLogoUploaded={(url) => {
              // Don't call loadBranding() here - handleSave calls it once after all uploads complete
              // Just dispatch event to notify Header to reload branding
              window.dispatchEvent(new CustomEvent("branding-updated"));
            }}
            onLogoRemoved={() => {
              // Logo removal is saved immediately, so reload from server to sync state
              loadBranding();
              // Dispatch event to notify Header to reload branding
              window.dispatchEvent(new CustomEvent("branding-updated"));
            }}
            onUseHeaderLogoChange={(useHeader) => {
              setBranding((prev) => ({
                ...prev,
                useHeaderLogoForLogin: useHeader,
              }));
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <LogoUploadSection
            ref={faviconRef}
            type="favicon"
            currentLogoUrl={branding?.faviconUrl}
            onFileSelected={handleFileSelected}
            onLogoUploaded={(url) => {
              // Update favicon in document head
              updateFavicon(url);
              // Don't call loadBranding() here - handleSave calls it once after all uploads complete
              // Just dispatch event to notify Header to reload branding
              window.dispatchEvent(new CustomEvent("branding-updated"));
            }}
            onLogoRemoved={() => {
              // Logo removal is saved immediately, so reload from server to sync state
              // Reset to default favicon
              resetFavicon();
              loadBranding();
              // Dispatch event to notify Header to reload branding
              window.dispatchEvent(new CustomEvent("branding-updated"));
            }}
          />
        </Column>
      </Grid>

      {/* Color Configuration Sections */}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <ColorPickerSection
            label={intl.formatMessage({ id: "site.branding.header.color" })}
            description={intl.formatMessage({
              id: "site.branding.header.color.description",
            })}
            value={branding?.headerColor || "#295785"}
            onChange={(color) => {
              setBranding((prev) => ({ ...prev, headerColor: color }));
              // Apply color immediately for preview
              document.documentElement.style.setProperty(
                "--site-branding-header",
                color,
              );
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <ColorPickerSection
            label={intl.formatMessage({ id: "site.branding.primary.color" })}
            description={intl.formatMessage({
              id: "site.branding.primary.color.description",
            })}
            value={branding?.primaryColor || "#0f62fe"}
            onChange={(color) => {
              setBranding((prev) => ({ ...prev, primaryColor: color }));
              // Apply color immediately for preview
              document.documentElement.style.setProperty(
                "--cds-interactive-01",
                color,
              );
              document.documentElement.style.setProperty(
                "--site-branding-primary",
                color,
              );
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <ColorPickerSection
            label={intl.formatMessage({ id: "site.branding.secondary.color" })}
            description={intl.formatMessage({
              id: "site.branding.secondary.color.description",
            })}
            value={branding?.secondaryColor || "#393939"}
            onChange={(color) => {
              setBranding((prev) => ({ ...prev, secondaryColor: color }));
              // Apply color immediately for preview
              document.documentElement.style.setProperty(
                "--cds-interactive-02",
                color,
              );
              document.documentElement.style.setProperty(
                "--site-branding-secondary",
                color,
              );
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Button
              onClick={handleSave}
              disabled={(!hasUnsavedChanges && !hasPendingFiles) || isSaving}
              style={{ marginRight: "1rem" }}
            >
              {isSaving ? (
                <InlineLoading
                  description={intl.formatMessage({
                    id: "loading.description",
                  })}
                />
              ) : (
                <FormattedMessage id="site.branding.save" />
              )}
            </Button>
            <Button
              data-testid="branding-cancel-button"
              onClick={handleCancel}
              kind="secondary"
              style={{ marginRight: "1rem" }}
            >
              <FormattedMessage id="site.branding.cancel" />
            </Button>
            <Button
              data-testid="branding-reset-button"
              kind="danger"
              onClick={handleReset}
            >
              <FormattedMessage id="site.branding.reset.to.defaults" />
            </Button>
            {(hasUnsavedChanges || hasPendingFiles) && (
              <p
                style={{
                  marginTop: "1rem",
                  fontStyle: "italic",
                  color: "#da1e28",
                }}
              >
                <FormattedMessage id="site.branding.unsaved.changes.warning" />
              </p>
            )}
          </Section>
        </Column>
      </Grid>

      {/* Reset Confirmation Modal */}
      <Modal
        open={showResetConfirm}
        modalHeading={intl.formatMessage({
          id: "site.branding.reset.to.defaults",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.button.reset" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestClose={cancelReset}
        onRequestSubmit={confirmReset}
        danger
      >
        <p>{intl.formatMessage({ id: "site.branding.reset.confirmation" })}</p>
      </Modal>
    </div>
  );
}

export default SiteBrandingConfig;
