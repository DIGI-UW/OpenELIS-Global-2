import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import PageLoading from "./PageLoading";

const messages = {
  "label.loading": "Loading...",
  "coldStorage.loadingDevices": "Loading devices...",
};

function renderWithIntl(ui: React.ReactElement) {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );
}

describe("PageLoading", () => {
  it("renders the Carbon Loading component with default message", () => {
    const { container } = renderWithIntl(<PageLoading />);
    // Carbon Loading renders with cds--loading class
    expect(container.querySelector(".cds--loading")).toBeInTheDocument();
    // Default i18n key "label.loading" = "Loading..."
    expect(screen.getByTitle("Loading...")).toBeInTheDocument();
  });

  it("accepts a custom messageId prop", () => {
    renderWithIntl(<PageLoading messageId="coldStorage.loadingDevices" />);
    expect(screen.getByTitle("Loading devices...")).toBeInTheDocument();
  });
});
