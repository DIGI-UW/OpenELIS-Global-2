/**
 * The signature ceremony can be completed from the keyboard, and the modal
 * carries the class that lets it escape a host modal's transformed container.
 */
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../languages/en.json";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import ESignatureModal from "./ESignatureModal";
import {
  certifyUser,
  executeSignature,
  getSessionStatus,
  isUserCertified,
  SignatureMeaning,
} from "./api";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    isUserCertified: vi.fn(),
    getSessionStatus: vi.fn(),
    executeSignature: vi.fn(),
    certifyUser: vi.fn(),
  };
});

const renderModal = (onSuccess = vi.fn(), onClose = vi.fn()) =>
  render(
    <UserSessionDetailsContext.Provider
      value={{ userSessionDetails: { loginName: "admin", userId: "1" } }}
    >
      <IntlProvider locale="en" messages={messages}>
        <ESignatureModal
          open
          onClose={onClose}
          onSuccess={onSuccess}
          meaning={SignatureMeaning.VALIDATED_AND_RELEASED}
          context="Release 2 clear result(s)"
          recordType="VALIDATION_BATCH"
          recordId={7}
        />
      </IntlProvider>
    </UserSessionDetailsContext.Provider>,
  );

describe("ESignatureModal", () => {
  beforeEach(() => {
    isUserCertified.mockReset().mockResolvedValue({ certified: true });
    getSessionStatus
      .mockReset()
      .mockResolvedValue({ sessionActive: false, signingCount: 0 });
    executeSignature.mockReset().mockResolvedValue({ id: 1 });
  });

  it("signs when Enter is pressed in the password field", async () => {
    const onSuccess = vi.fn();
    renderModal(onSuccess);

    const password = await screen.findByLabelText("Password");
    fireEvent.change(password, { target: { value: "adminADMIN!" } });
    fireEvent.keyDown(password, { key: "Enter", code: "Enter" });

    await waitFor(() =>
      expect(executeSignature).toHaveBeenCalledWith(
        expect.objectContaining({
          username: "admin",
          password: "adminADMIN!",
          recordType: "VALIDATION_BATCH",
          recordId: 7,
        }),
      ),
    );
    await waitFor(() => expect(onSuccess).toHaveBeenCalled());
  });

  it("after certifying, Enter signs with the session username already filled in", async () => {
    isUserCertified.mockResolvedValue({ certified: false });
    certifyUser.mockResolvedValue({});
    const onSuccess = vi.fn();
    renderModal(onSuccess);

    fireEvent.click(await screen.findByRole("checkbox"));
    const certPassword = screen.getByLabelText("Password");
    fireEvent.change(certPassword, { target: { value: "adminADMIN!" } });
    fireEvent.keyDown(certPassword, { key: "Enter", code: "Enter" });

    const username = await screen.findByLabelText("Username");
    expect(username).toHaveValue("admin");
    const password = screen.getByLabelText("Password");
    fireEvent.change(password, { target: { value: "adminADMIN!" } });
    fireEvent.keyDown(password, { key: "Enter", code: "Enter" });

    await waitFor(() =>
      expect(executeSignature).toHaveBeenCalledWith(
        expect.objectContaining({ username: "admin" }),
      ),
    );
  });

  it("marks its root so a host modal drops its transform while it is open", async () => {
    renderModal();

    await screen.findByLabelText("Password");
    const root = document.querySelector(".esignature-modal");
    expect(root).not.toBeNull();
    expect(root.classList.contains("is-visible")).toBe(true);
  });
});
