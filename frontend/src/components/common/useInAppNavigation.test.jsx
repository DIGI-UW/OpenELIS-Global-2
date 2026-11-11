import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { MemoryRouter, Route } from "react-router-dom";
import useInAppNavigation from "./useInAppNavigation";

const Probe = () => {
  const navigate = useInAppNavigation();
  return (
    <>
      <a href="/Target" onClick={navigate("/Target")}>
        link
      </a>
      <button type="button" onClick={() => navigate("/FromCode?x=1")()}>
        code
      </button>
    </>
  );
};

const renderProbe = () => {
  const seen = {};
  render(
    <MemoryRouter initialEntries={["/Start"]}>
      <Probe />
      <Route
        path="*"
        render={({ location }) => {
          seen.location = location;
          return null;
        }}
      />
    </MemoryRouter>,
  );
  return seen;
};

describe("useInAppNavigation", () => {
  it("routes a plain click inside the app and stops the page load", () => {
    const seen = renderProbe();

    const proceeded = fireEvent.click(screen.getByText("link"));

    expect(proceeded).toBe(false);
    expect(seen.location.pathname).toBe("/Target");
  });

  it.each([
    ["ctrl", { ctrlKey: true }],
    ["cmd", { metaKey: true }],
    ["shift", { shiftKey: true }],
    ["middle button", { button: 1 }],
  ])("leaves a %s click to the browser", (_name, init) => {
    const seen = renderProbe();

    const proceeded = fireEvent.click(screen.getByText("link"), init);

    expect(proceeded).toBe(true);
    expect(seen.location.pathname).toBe("/Start");
  });

  it("navigates when called from code without an event", () => {
    const seen = renderProbe();

    fireEvent.click(screen.getByText("code"));

    expect(seen.location.pathname).toBe("/FromCode");
    expect(seen.location.search).toBe("?x=1");
  });
});
