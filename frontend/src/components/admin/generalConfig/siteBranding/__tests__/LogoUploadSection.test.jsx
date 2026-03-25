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

jest.mock("../../../../utils/Utils", () => ({
  postToOpenElisServerFormData: jest.fn(),
}));

jest.mock("../../../../utils/BrandingUtils", () => ({
  removeLogo: jest.fn(),
}));

// ========== IMPORTS ==========

import React from "react";
import "@testing-library/jest-dom/extend-expect";
import {
  render,
  screen,
  fireEvent,
  wait,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import LogoUploadSection from "../LogoUploadSection";
import { postToOpenElisServerFormData } from "../../../../utils/Utils";
import { removeLogo } from "../../../../utils/BrandingUtils";
import messages from "../../../../../languages/en.json";

// ========== HELPER FUNCTIONS ==========

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
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
    renderWithIntl(<LogoUploadSection type="header" currentLogoUrl={null} />);

    // Use getByRole to specifically target the heading element
    expect(
      screen.getByRole("heading", { name: /header logo/i }),
    ).toBeInTheDocument();
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
      />,
    );

    const img = screen.getByAltText(/header logo/i);
    expect(img).toBeInTheDocument();
    // Component adds server base URL prefix and cache-busting parameter
    expect(img.getAttribute("src")).toContain(
      "/rest/site-branding/logo/header",
    );
  });

  /**
   * Test: Validates file format
   * Task Reference: T028
   */
  test("validates file format and shows error for invalid format", async () => {
    renderWithIntl(<LogoUploadSection type="header" currentLogoUrl={null} />);

    // Create a mock file with invalid format
    const file = new File(["test"], "test.txt", { type: "text/plain" });
    // Carbon FileUploader creates a hidden input[type="file"]
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    // Should show error message
    expect(
      await screen.findByText(/unsupported file format/i),
    ).toBeInTheDocument();
  });

  /**
   * Test: Validates file size
   * Task Reference: T028
   */
  test("validates file size and shows error for oversized file", async () => {
    renderWithIntl(<LogoUploadSection type="header" currentLogoUrl={null} />);

    // Create a mock file larger than 2MB
    const largeFile = new File(["x".repeat(3 * 1024 * 1024)], "large.png", {
      type: "image/png",
    });
    // Carbon FileUploader creates a hidden input[type="file"]
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [largeFile] } });

    // Should show error message
    expect(await screen.findByText(/file size exceeds/i)).toBeInTheDocument();
  });

  /**
   * Test: Uploads valid file successfully
   * Task Reference: T028
   */
  test("uploads valid file and calls onLogoUploaded callback", async () => {
    const onLogoUploaded = jest.fn();
    const ref = React.createRef();

    postToOpenElisServerFormData.mockImplementation(
      (url, formData, callback) => {
        callback(200);
      },
    );

    renderWithIntl(
      <LogoUploadSection
        ref={ref}
        type="header"
        currentLogoUrl={null}
        onLogoUploaded={onLogoUploaded}
      />,
    );

    // Create a valid file
    const file = new File(["test"], "logo.png", { type: "image/png" });
    // Carbon FileUploader creates a hidden input[type="file"]
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    // Trigger upload via ref (as parent component would do)
    await ref.current.uploadFile();

    // Should call callback
    await wait(() => {
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
      />,
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
      />,
    );

    // File uploader should not be visible when using header logo
    // Carbon FileUploader creates a hidden input[type="file"]
    const uploader = document.querySelector('input[type="file"]');
    expect(uploader).not.toBeInTheDocument();
  });

  /**
   * Test: Toggle "Use same logo as header" checkbox
   * Task Reference: T037
   */
  test("toggles useHeaderLogoForLogin when checkbox is clicked", async () => {
    const onUseHeaderLogoChange = jest.fn();

    renderWithIntl(
      <LogoUploadSection
        type="login"
        currentLogoUrl={null}
        useHeaderLogoForLogin={false}
        onUseHeaderLogoChange={onUseHeaderLogoChange}
      />,
    );

    const checkbox = screen.getByLabelText(/use same logo as header/i);
    await userEvent.click(checkbox);

    expect(onUseHeaderLogoChange).toHaveBeenCalledWith(true);
  });

  /**
   * Test: Upload failure handling
   */
  test("shows error when upload fails", async () => {
    const ref = React.createRef();

    postToOpenElisServerFormData.mockImplementation(
      (url, formData, callback) => {
        callback(500); // Server error
      },
    );

    renderWithIntl(
      <LogoUploadSection ref={ref} type="header" currentLogoUrl={null} />,
    );

    // Create a valid file
    const file = new File(["test"], "logo.png", { type: "image/png" });
    // Carbon FileUploader creates a hidden input[type="file"]
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [file] } });

    // Trigger upload via ref (as parent component would do)
    try {
      await ref.current.uploadFile();
    } catch (e) {
      // Expected to throw on failure
    }

    // Should show error message
    expect(await screen.findByText(/error saving/i)).toBeInTheDocument();
  });

  /**
   * Test: Logo removal - shows confirmation modal
   */
  test("shows confirmation modal when remove button clicked", async () => {
    renderWithIntl(
      <LogoUploadSection
        type="header"
        currentLogoUrl="/rest/site-branding/logo/header"
      />,
    );

    // Click remove button using test-id
    const removeButton = screen.getByTestId("remove-logo-button");
    await userEvent.click(removeButton);

    // Should show confirmation modal
    expect(await screen.findByText(/are you sure/i)).toBeInTheDocument();
  });

  /**
   * Test: Logo removal - successful removal
   */
  test("removes logo when confirmed", async () => {
    const onLogoRemoved = jest.fn();

    removeLogo.mockImplementation((type, callback) => {
      callback({ status: 200, ok: true, json: () => Promise.resolve({}) });
    });

    renderWithIntl(
      <LogoUploadSection
        type="header"
        currentLogoUrl="/rest/site-branding/logo/header"
        onLogoRemoved={onLogoRemoved}
      />,
    );

    // Click remove button using test-id
    const removeButton = screen.getByTestId("remove-logo-button");
    await userEvent.click(removeButton);

    // Wait for confirmation modal
    await screen.findByText(/are you sure/i);

    // Find the modal's confirm button - it has exact text "Remove" (not "Remove Logo")
    const removeButtons = screen.getAllByText(/^Remove$/);
    await userEvent.click(removeButtons[0]);

    // Should call onLogoRemoved callback
    await wait(() => {
      expect(removeLogo).toHaveBeenCalledWith("header", expect.any(Function));
      expect(onLogoRemoved).toHaveBeenCalled();
    });
  });

  /**
   * Test: Logo removal - cancel removal
   */
  test("cancels removal when cancel clicked in modal", async () => {
    renderWithIntl(
      <LogoUploadSection
        type="header"
        currentLogoUrl="/rest/site-branding/logo/header"
      />,
    );

    // Click remove button using test-id
    const removeButton = screen.getByTestId("remove-logo-button");
    await userEvent.click(removeButton);

    // Click cancel in modal - use text matching
    const cancelButton = await screen.findByText(/cancel/i);
    await userEvent.click(cancelButton);

    // Logo should still be displayed
    const img = screen.getByAltText(/header logo/i);
    expect(img).toBeInTheDocument();
  });

  /**
   * Test: Logo removal - failure handling
   */
  test("shows error when removal fails", async () => {
    removeLogo.mockImplementation((type, callback) => {
      callback({ status: 500, ok: false });
    });

    renderWithIntl(
      <LogoUploadSection
        type="header"
        currentLogoUrl="/rest/site-branding/logo/header"
      />,
    );

    // Click remove button using test-id
    const removeButton = screen.getByTestId("remove-logo-button");
    await userEvent.click(removeButton);

    // Wait for confirmation modal
    await screen.findByText(/are you sure/i);

    // Find the modal's confirm button - it has exact text "Remove" (not "Remove Logo")
    const removeButtons = screen.getAllByText(/^Remove$/);
    await userEvent.click(removeButtons[0]);

    // Should show error message (the specific error text from messages)
    expect(await screen.findByText(/error removing/i)).toBeInTheDocument();
  });

  /**
   * Test: Favicon type rendering
   */
  test("renders favicon upload section", () => {
    renderWithIntl(<LogoUploadSection type="favicon" currentLogoUrl={null} />);

    // Use getByRole to specifically target the heading element
    expect(
      screen.getByRole("heading", { name: /favicon/i }),
    ).toBeInTheDocument();
  });
});
