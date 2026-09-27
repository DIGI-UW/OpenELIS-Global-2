import React from "react";
import { render, cleanup, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { FormattedMessage, IntlProvider } from "react-intl";
import en from "../en.json";
import { languageMessages } from "../index";
import { NO_OVERRIDE, applyOverride } from "../translationOverride";

/**
 * The merged messages as react-intl actually consumes them.
 *
 * The loader's own tests check what comes back from the mounted files; this
 * checks the thing that matters to a user — that an overridden id renders the
 * deployment's wording, that every id the deployment said nothing about still
 * renders its shipped wording, and that a raw message id never reaches the
 * screen. It runs against the real bundles rather than a fixture, so it also
 * pins the assumption the whole design rests on: a bundle is a flat map of
 * message id to string.
 */

/** Mirrors the composition App.jsx performs for IntlProvider. */
const renderWith = (locale, override, ids) =>
  render(
    <IntlProvider
      locale={locale}
      defaultLocale="en"
      messages={applyOverride(languageMessages[locale], override)}
    >
      <ul>
        {ids.map((id) => (
          <li key={id} data-testid={id}>
            <FormattedMessage id={id} />
          </li>
        ))}
      </ul>
    </IntlProvider>,
  );

const SAVE = "label.button.save";
const SUBMIT = "label.button.submit";

describe("a deployment's wording, as rendered", () => {
  beforeEach(() => {
    cleanup();
  });

  it("ships the bundled wording when there is no override", () => {
    renderWith("en", NO_OVERRIDE, [SAVE, SUBMIT]);

    expect(screen.getByTestId(SAVE)).toHaveTextContent(en[SAVE]);
    expect(screen.getByTestId(SUBMIT)).toHaveTextContent(en[SUBMIT]);
  });

  it("renders the deployment's wording for the ids it names", () => {
    renderWith("en", { [SAVE]: "Keep" }, [SAVE]);

    expect(screen.getByTestId(SAVE)).toHaveTextContent("Keep");
  });

  it("leaves every other id at its shipped wording", () => {
    renderWith("en", { [SAVE]: "Keep" }, [SAVE, SUBMIT]);

    expect(screen.getByTestId(SUBMIT)).toHaveTextContent(en[SUBMIT]);
  });

  it("overrides a non-English locale without disturbing its English fallback", () => {
    // French inherits English for anything Transifex has not caught up with, so
    // an override must sit on top of that rather than replacing the layer.
    renderWith("fr", { [SAVE]: "Garder" }, [SAVE, SUBMIT]);

    expect(screen.getByTestId(SAVE)).toHaveTextContent("Garder");
    expect(
      screen.getByTestId(SUBMIT).textContent,
      "an id the override did not name still resolves to a real string",
    ).not.toBe(SUBMIT);
  });

  it("never puts a raw message id on screen for an id the override omits", () => {
    renderWith("en", { [SAVE]: "Keep" }, [SUBMIT]);

    expect(screen.getByTestId(SUBMIT).textContent).not.toBe(SUBMIT);
  });

  it("treats the bundles as a flat map of id to string", () => {
    // The whole override mechanism is a per-key spread; a nested bundle would
    // silently break that.
    const nonString = Object.keys(en).filter(
      (key) => typeof en[key] !== "string",
    );

    expect(nonString).toEqual([]);
  });
});
