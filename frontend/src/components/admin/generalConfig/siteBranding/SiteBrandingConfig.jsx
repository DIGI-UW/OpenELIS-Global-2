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
import { getBranding, updateBranding, resetBranding } from "../../../../services/siteBrandingService";
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
  const initialBrandingRef = useRef(null);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    { label: "sidenav.label.admin.formEntry.siteInfoconfig", link: "/MasterListsPage#SiteInformationMenu" },
    { label: "site.branding.title", link: "#SiteBrandingMenu" },
  ];

  useEffect(() => {
    loadBranding();
  }, []);

  const loadBranding = () => {
    setIsLoading(true);
    getBranding((response) => {
      if (response) {
        setBranding(response);
        setSavedBranding(JSON.parse(JSON.stringify(response))); // Deep copy for comparison
        initialBrandingRef.current = JSON.parse(JSON.stringify(response));
        // Update favicon if custom favicon exists
        if (response.faviconUrl) {
          updateFavicon(response.faviconUrl);
        }
      } else {
        // Handle error - use default values
        const defaultBranding = {
          primaryColor: "#1d4ed8",
          secondaryColor: "#64748b",
          accentColor: "#0891b2",
          colorMode: "light",
          useHeaderLogoForLogin: false,
        };
        setBranding(defaultBranding);
        setSavedBranding(JSON.parse(JSON.stringify(defaultBranding)));
        initialBrandingRef.current = JSON.parse(JSON.stringify(defaultBranding));
      }
      setIsLoading(false);
      setHasUnsavedChanges(false);
    });
  };

  // Task Reference: T073 - Detect form state changes
  useEffect(() => {
    if (branding && savedBranding) {
      const hasChanges = JSON.stringify(branding) !== JSON.stringify(savedBranding);
      setHasUnsavedChanges(hasChanges);
    }
  }, [branding, savedBranding]);

  // Task Reference: T073 - Warn user when navigating away with unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (hasUnsavedChanges) {
        e.preventDefault();
        e.returnValue = intl.formatMessage({ id: "site.branding.unsaved.changes" });
        return e.returnValue;
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [hasUnsavedChanges, intl]);

  // Task Reference: T046 - Update favicon in document head
  const updateFavicon = (faviconUrl) => {
    // Remove existing favicon links
    const existingLinks = document.querySelectorAll('link[rel*="icon"]');
    existingLinks.forEach(link => link.remove());

    // Add new favicon link
    const link = document.createElement('link');
    link.rel = 'icon';
    link.type = 'image/x-icon';
    link.href = `../api${faviconUrl}`;
    document.head.appendChild(link);
  };

  const resetFavicon = () => {
    // Remove existing favicon links
    const existingLinks = document.querySelectorAll('link[rel*="icon"]');
    existingLinks.forEach(link => link.remove());

    // Add default favicon link
    const link = document.createElement('link');
    link.rel = 'icon';
    link.href = '../images/favicon-16x16.png';
    document.head.appendChild(link);
  };

  const handleSave = () => {
    if (!branding || isSaving) return;

    // Task Reference: T097 - Disable form during save operation
    setIsSaving(true);

    // Task Reference: T072 - Call siteBrandingService.updateBranding() with form data
    updateBranding(
      branding,
      (status) => {
        setIsSaving(false);
        if (status === 200 || status === 201) {
          // Task Reference: T074 - Re-fetch branding config after save to ensure consistency
          loadBranding(); // Reload to get updated state from server
          
          addNotification(
            intl.formatMessage({ id: "site.branding.save.success" }),
            NotificationKinds.success
          );
          setNotificationVisible(true);
        } else {
          addNotification(
            intl.formatMessage({ id: "site.branding.save.error" }),
            NotificationKinds.error
          );
          setNotificationVisible(true);
        }
      }
    );
  };

  const handleCancel = () => {
    if (hasUnsavedChanges) {
      // Show confirmation if there are unsaved changes
      if (window.confirm(intl.formatMessage({ id: "site.branding.unsaved.changes" }))) {
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

    resetBranding(
      (status) => {
        setIsLoading(false);
        if (status === 200 || status === 201) {
          // Reload branding after reset
          loadBranding();
          
          // Reset CSS custom properties to defaults
          document.documentElement.style.setProperty('--cds-interactive-01', '#1d4ed8');
          document.documentElement.style.setProperty('--cds-interactive-02', '#64748b');
          document.documentElement.style.setProperty('--cds-support-01', '#0891b2');
          document.documentElement.style.setProperty('--site-branding-primary', '#1d4ed8');
          document.documentElement.style.setProperty('--site-branding-secondary', '#64748b');
          document.documentElement.style.setProperty('--site-branding-accent', '#0891b2');
          
          // Reset favicon
          resetFavicon();

          addNotification(
            intl.formatMessage({ id: "site.branding.reset.success" }),
            NotificationKinds.success
          );
          setNotificationVisible(true);
        } else {
          addNotification(
            intl.formatMessage({ id: "site.branding.reset.error" }),
            NotificationKinds.error
          );
          setNotificationVisible(true);
        }
      }
    );
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
            type="header"
            currentLogoUrl={branding?.headerLogoUrl}
            onLogoUploaded={(url) => {
              setBranding((prev) => ({ ...prev, headerLogoUrl: url }));
            }}
            onLogoRemoved={() => {
              setBranding((prev) => ({ ...prev, headerLogoUrl: null }));
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <LogoUploadSection
            type="login"
            currentLogoUrl={branding?.loginLogoUrl}
            useHeaderLogoForLogin={branding?.useHeaderLogoForLogin || false}
            onLogoUploaded={(url) => {
              setBranding((prev) => ({ ...prev, loginLogoUrl: url }));
            }}
            onLogoRemoved={() => {
              setBranding((prev) => ({ ...prev, loginLogoUrl: null }));
            }}
            onUseHeaderLogoChange={(useHeader) => {
              setBranding((prev) => ({ ...prev, useHeaderLogoForLogin: useHeader }));
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <LogoUploadSection
            type="favicon"
            currentLogoUrl={branding?.faviconUrl}
            onLogoUploaded={(url) => {
              setBranding((prev) => ({ ...prev, faviconUrl: url }));
              // Update favicon in document head
              updateFavicon(url);
            }}
            onLogoRemoved={() => {
              setBranding((prev) => ({ ...prev, faviconUrl: null }));
              // Reset to default favicon
              resetFavicon();
            }}
          />
        </Column>
      </Grid>

      {/* Color Configuration Sections */}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <ColorPickerSection
            label={intl.formatMessage({ id: "site.branding.primary.color" })}
            description={intl.formatMessage({ id: "site.branding.primary.color.description" })}
            value={branding?.primaryColor || "#1d4ed8"}
            onChange={(color) => {
              setBranding((prev) => ({ ...prev, primaryColor: color }));
              // Apply color immediately for preview
              document.documentElement.style.setProperty('--cds-interactive-01', color);
              document.documentElement.style.setProperty('--site-branding-primary', color);
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <ColorPickerSection
            label={intl.formatMessage({ id: "site.branding.secondary.color" })}
            description={intl.formatMessage({ id: "site.branding.secondary.color.description" })}
            value={branding?.secondaryColor || "#64748b"}
            onChange={(color) => {
              setBranding((prev) => ({ ...prev, secondaryColor: color }));
              // Apply color immediately for preview
              document.documentElement.style.setProperty('--cds-interactive-02', color);
              document.documentElement.style.setProperty('--site-branding-secondary', color);
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <ColorPickerSection
            label={intl.formatMessage({ id: "site.branding.accent.color" })}
            description={intl.formatMessage({ id: "site.branding.accent.color.description" })}
            value={branding?.accentColor || "#0891b2"}
            onChange={(color) => {
              setBranding((prev) => ({ ...prev, accentColor: color }));
              // Apply color immediately for preview
              document.documentElement.style.setProperty('--cds-support-01', color);
              document.documentElement.style.setProperty('--site-branding-accent', color);
            }}
          />
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Button 
              onClick={handleSave} 
              disabled={!hasUnsavedChanges || isSaving}
              style={{ marginRight: "1rem" }}
            >
              {isSaving ? (
                <InlineLoading description={intl.formatMessage({ id: "loading.description" })} />
              ) : (
                <FormattedMessage id="site.branding.save" />
              )}
            </Button>
            <Button 
              onClick={handleCancel}
              kind="secondary"
              style={{ marginRight: "1rem" }}
            >
              <FormattedMessage id="site.branding.cancel" />
            </Button>
            <Button 
              kind="danger" 
              onClick={handleReset}
            >
              <FormattedMessage id="site.branding.reset.to.defaults" />
            </Button>
            {hasUnsavedChanges && (
              <p style={{ marginTop: "1rem", fontStyle: "italic", color: "#da1e28" }}>
                <FormattedMessage id="site.branding.unsaved.changes.warning" />
              </p>
            )}
          </Section>
        </Column>
      </Grid>

      {/* Reset Confirmation Modal */}
      <Modal
        open={showResetConfirm}
        modalHeading={intl.formatMessage({ id: "site.branding.reset.to.defaults" })}
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

