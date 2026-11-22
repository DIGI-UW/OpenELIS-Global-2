import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, expect, test, vi } from "vitest";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/Layout";
import RejectModal from "./RejectModal";

const renderModal = (referral) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{ addNotification: vi.fn(), setNotificationVisible: vi.fn() }}
      >
        <RejectModal
          open
          referral={{ id: "1", labNumber: "DEV0126000001", ...referral }}
          onClose={vi.fn()}
          onSuccess={vi.fn()}
        />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const reasonTextarea = () => document.querySelector("#reject-reason-text");
const reasonSelect = () => document.querySelector("#reject-reason-code");

describe("RejectModal reason cap", () => {
  test("caps the reason at 500 characters in the markup, not just in intent", () => {
    renderModal({});
    // maxCount alone is inert in Carbon; only enableCounter emits maxlength.
    // Without it an over-length reason reaches the server and is silently
    // truncated into the VARCHAR(500) audit note.
    expect(reasonTextarea()).toHaveAttribute("maxlength", "500");
  });

  test("shows the operator a live counter against the limit", () => {
    renderModal({});
    expect(screen.getByText("0/500")).toBeInTheDocument();
  });
});

describe("RejectModal reason pre-fill from the peer laboratory", () => {
  test("preselects the peer's reason when its text names one of ours", () => {
    renderModal({ peerReason: "Hemolyzed" });
    expect(reasonSelect().value).toBe("hemolyzed");
  });

  test("matches regardless of case and spacing", () => {
    renderModal({ peerReason: "wrong sample type" });
    expect(reasonSelect().value).toBe("wrongSampleType");
  });

  test("matches a reason embedded in a longer sentence from the peer", () => {
    renderModal({ peerReason: "Specimen arrived clotted on 2026-09-16" });
    expect(reasonSelect().value).toBe("clotted");
  });

  test("keeps the default when the peer sent nothing", () => {
    renderModal({});
    expect(reasonSelect().value).toBe("insufficientVolume");
  });

  test("keeps the default rather than guessing at unrecognised peer wording", () => {
    // A wrong non-conformity on a rejection is worse than an unset one.
    renderModal({ peerReason: "sample unsuitable for analysis" });
    expect(reasonSelect().value).toBe("insufficientVolume");
  });
});
