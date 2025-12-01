/**
 * Unit tests for LogoUploadSection component
 * 
 * References:
 * - Testing Roadmap: .specify/guides/testing-roadmap.md
 * - Jest Best Practices: .specify/guides/jest-best-practices.md
 * 
 * Task Reference: T028
 */

// ========== MOCKS (MUST be before imports - Jest hoisting) ==========

jest.mock("../../../../components/utils/Utils", () => ({
  postToOpenElisServerFormData: jest.fn(),
}));

// ========== IMPORTS ==========

import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import LogoUploadSection from "../LogoUploadSection";
import { postToOpenElisServerFormData } from "../../../../components/utils/Utils";
import messages from "../../../../languages/en.json";

// ========== HELPER FUNCTIONS ==========

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};

// ========== TEST SUITE ==========

describe("LogoUploadSection", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test: Component renders with file uploader
   * Task Reference: T028
   */
  test("renders file uploader for logo upload", () => {
    renderWithIntl(
      <LogoUploadSection type="header" currentLogoUrl={null} />
    );

    expect(screen.getByText(/header logo/i)).toBeInTheDocument();
  });

  /**
   * Test: Displays current logo preview if exists
   * Task Reference: T028
   */
  test("displays current logo preview when logoUrl provided", () => {
    renderWithIntl(
      <LogoUploadSection 
        type="header" 
        currentLogoUrl="/rest/site-branding/logo/header" 
      />
    );

    const img = screen.getByAltText(/header logo/i);
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute("src", "/rest/site-branding/logo/header");
  });

  /**
   * Test: Validates file format
   * Task Reference: T028
   */
  test("validates file format and shows error for invalid format", async () => {
    const user = userEvent.setup();
    renderWithIntl(
      <LogoUploadSection type="header" currentLogoUrl={null} />
    );

    // Create a mock file with invalid format
    const file = new File(["test"], "test.txt", { type: "text/plain" });
    const input = screen.getByLabelText(/upload logo/i);
    
    await user.upload(input, file);

    // Should show error message
    await waitFor(() => {
      expect(screen.getByText(/unsupported file format/i)).toBeInTheDocument();
    });
  });

  /**
   * Test: Validates file size
   * Task Reference: T028
   */
  test("validates file size and shows error for oversized file", async () => {
    const user = userEvent.setup();
    renderWithIntl(
      <LogoUploadSection type="header" currentLogoUrl={null} />
    );

    // Create a mock file larger than 2MB
    const largeFile = new File(["x".repeat(3 * 1024 * 1024)], "large.png", { 
      type: "image/png" 
    });
    const input = screen.getByLabelText(/upload logo/i);
    
    await user.upload(input, largeFile);

    // Should show error message
    await waitFor(() => {
      expect(screen.getByText(/file size exceeds/i)).toBeInTheDocument();
    });
  });

  /**
   * Test: Uploads valid file successfully
   * Task Reference: T028
   */
  test("uploads valid file and calls onLogoUploaded callback", async () => {
    const user = userEvent.setup();
    const onLogoUploaded = jest.fn();
    
    postToOpenElisServerFormData.mockImplementation((url, formData, callback) => {
      callback(200);
    });

    renderWithIntl(
      <LogoUploadSection 
        type="header" 
        currentLogoUrl={null}
        onLogoUploaded={onLogoUploaded}
      />
    );

    // Create a valid file
    const file = new File(["test"], "logo.png", { type: "image/png" });
    const input = screen.getByLabelText(/upload logo/i);
    
    await user.upload(input, file);

    // Click upload button
    const uploadButton = screen.getByText(/upload logo/i);
    await user.click(uploadButton);

    // Should call callback
    await waitFor(() => {
      expect(postToOpenElisServerFormData).toHaveBeenCalled();
      expect(onLogoUploaded).toHaveBeenCalled();
    });
  });

  /**
   * Test: Login logo upload with "Use same logo as header" checkbox
   * Task Reference: T037
   */
  test("displays checkbox for using header logo on login page", () => {
    renderWithIntl(
      <LogoUploadSection 
        type="login" 
        currentLogoUrl={null}
        useHeaderLogoForLogin={false}
        onUseHeaderLogoChange={jest.fn()}
      />
    );

    // Should show checkbox for "Use same logo as header"
    expect(screen.getByText(/use same logo as header/i)).toBeInTheDocument();
  });

  /**
   * Test: When "Use same logo as header" is checked, hide login logo upload
   * Task Reference: T037
   */
  test("hides login logo upload when useHeaderLogoForLogin is true", () => {
    renderWithIntl(
      <LogoUploadSection 
        type="login" 
        currentLogoUrl={null}
        useHeaderLogoForLogin={true}
        onUseHeaderLogoChange={jest.fn()}
      />
    );

    // File uploader should not be visible when using header logo
    const uploader = screen.queryByLabelText(/upload logo/i);
    expect(uploader).not.toBeInTheDocument();
  });

  /**
   * Test: Toggle "Use same logo as header" checkbox
   * Task Reference: T037
   */
  test("toggles useHeaderLogoForLogin when checkbox is clicked", async () => {
    const user = userEvent.setup();
    const onUseHeaderLogoChange = jest.fn();

    renderWithIntl(
      <LogoUploadSection 
        type="login" 
        currentLogoUrl={null}
        useHeaderLogoForLogin={false}
        onUseHeaderLogoChange={onUseHeaderLogoChange}
      />
    );

    const checkbox = screen.getByLabelText(/use same logo as header/i);
    await user.click(checkbox);

    expect(onUseHeaderLogoChange).toHaveBeenCalledWith(true);
  });
});

