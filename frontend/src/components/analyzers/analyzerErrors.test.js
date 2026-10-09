import { createIntl } from "react-intl";
import messages from "../../languages/en.json";
import { analyzerErrorText, errorKeyFor } from "./analyzerErrors";

const intl = createIntl({ locale: "en", messages });

describe("analyzerErrorText", () => {
  it.each([
    "analyzer.mapping.error.changedSinceLoaded",
    "analyzer.mapping.error.recognitionChanged",
    "analyzer.mapping.error.confirmBeforeApply",
    "analyzer.mapping.error.testNotCurrent",
    "analyzer.mapping.error.resultNotCurrent",
    "analyzer.mapping.error.catalogNotCurrent",
    "analyzer.setup.error.profileKept",
    "analyzer.bridge.connection.unreachable",
    "analyzer.bridge.connection.httpStatus",
    "analyzer.bridge.connection.invalidEvidence",
    "analyzer.bridge.connection.invalidRequest",
    "analyzer.bridge.connection.notConfigured",
    "analyzer.bridge.connection.reconcileRequired",
    "analyzer.bridge.connection.referenceNotStored",
  ])("shows %s in words, never the server's text", (key) => {
    const text = analyzerErrorText(
      intl,
      { error: "RAW SERVER TEXT", messageKey: key },
      "analyzer.setup.instrument.saveError",
    );

    expect(text).toBe(messages[key]);
    expect(text).not.toContain("RAW SERVER TEXT");
  });

  it("fills a refusal's arguments into its message", () => {
    expect(
      analyzerErrorText(
        intl,
        {
          error: "ADOPT-C still has held results from revision 1",
          messageKey: "analyzer.adoption.error.heldResults",
          messageArgs: { record: "ADOPT-C", revision: 1 },
        },
        "analyzerType.adoption.error.save",
      ),
    ).toBe(
      "ADOPT-C still has held results from revision 1. Resolve them before adopting.",
    );
    expect(
      analyzerErrorText(
        intl,
        {
          error: "BOUND test row HIV LOG must name the component it lands on",
          messageKey: "analyzer.mapping.error.componentRequired",
          messageArgs: { record: "HIV LOG" },
        },
        "analyzer.setup.instrument.saveError",
      ),
    ).toBe(
      "HIV LOG lands on a component of its test. Choose that component, or leave the record unresolved.",
    );
  });

  it("names a Bridge connection failure and an activation blocker by their keys", () => {
    expect(
      errorKeyFor(
        { connectionErrorKey: "analyzer.bridge.connection.unreachable" },
        messages,
        null,
      ),
    ).toBe("analyzer.bridge.connection.unreachable");
    expect(
      errorKeyFor(
        { blockers: [{ code: "analyzer.activation.blocker.connection" }] },
        messages,
        null,
      ),
    ).toBe("analyzer.activation.blocker.connection");
  });

  it("falls back to the screen's own message for an unkeyed or unknown refusal", () => {
    expect(
      analyzerErrorText(
        intl,
        { error: "Analyzer request is required", messageKey: "no.such.key" },
        "analyzer.setup.instrument.saveError",
      ),
    ).toBe(messages["analyzer.setup.instrument.saveError"]);
    expect(analyzerErrorText(intl, { message: "boom" }, null)).toBe("");
  });
});
