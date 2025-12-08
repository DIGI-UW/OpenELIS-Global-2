/**
 * Branding Styles Component
 * 
 * Injects CSS custom properties for branding colors
 * 
 * Task Reference: T053
 */

import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { getBranding } from "../../../../services/siteBrandingService";
// CSS is imported globally in index.js to ensure it applies application-wide

function BrandingStyles() {
  const location = useLocation();

  useEffect(() => {
    const applyBrandingColors = (branding) => {
      if (!branding) return;

      // Get root element - this persists across all pages
      const root = document.documentElement;

      // Task Reference: T053 - Inject CSS custom properties for primary color
      // Setting on :root ensures they apply application-wide
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

    const loadBranding = () => {
      // Load branding configuration and apply colors
      getBranding((response) => {
        if (response) {
          applyBrandingColors(response);
        }
      });
    };

    // Load branding on mount and whenever route changes
    // This ensures colors are applied even if they were somehow reset
    loadBranding();

    // Listen for branding update events to refresh colors
    const handleBrandingUpdate = () => {
      loadBranding();
    };
    window.addEventListener('branding-updated', handleBrandingUpdate);
    
    return () => {
      window.removeEventListener('branding-updated', handleBrandingUpdate);
    };
  }, [location.pathname]); // Re-run when route changes to ensure colors persist

  // This component doesn't render anything
  return null;
}

export default BrandingStyles;

