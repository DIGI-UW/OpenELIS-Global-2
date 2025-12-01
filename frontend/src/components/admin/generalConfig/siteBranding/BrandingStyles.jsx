/**
 * Branding Styles Component
 * 
 * Injects CSS custom properties for branding colors
 * 
 * Task Reference: T053
 */

import { useEffect } from "react";
import { getBranding } from "../../../services/siteBrandingService";

function BrandingStyles() {
  useEffect(() => {
    const applyBrandingColors = (branding) => {
      if (!branding) return;

      // Get root element
      const root = document.documentElement;

      // Task Reference: T053 - Inject CSS custom properties for primary color
      if (branding.primaryColor) {
        root.style.setProperty('--cds-interactive-01', branding.primaryColor);
        root.style.setProperty('--site-branding-primary', branding.primaryColor);
      }

      // Task Reference: T058 - Inject CSS custom properties for secondary and accent colors
      if (branding.secondaryColor) {
        root.style.setProperty('--cds-interactive-02', branding.secondaryColor);
        root.style.setProperty('--site-branding-secondary', branding.secondaryColor);
      }

      if (branding.accentColor) {
        root.style.setProperty('--cds-support-01', branding.accentColor);
        root.style.setProperty('--site-branding-accent', branding.accentColor);
      }
    };

    // Load branding configuration and apply colors
    getBranding((response) => {
      if (response) {
        applyBrandingColors(response);
      }
    });
  }, []);

  // This component doesn't render anything
  return null;
}

export default BrandingStyles;

