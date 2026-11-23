/**
 * OGC-1611 P4 (FR-B24 to B26, G8): proposed samples on Prepare Samples.
 */
import { describe, expect, it } from "vitest";
import {
  applyProposals,
  proposalRequest,
  proposalSignature,
  isProposable,
  containerName,
} from "./containerProposals";

const SST = {
  id: 11,
  name: "SST 5 mL",
  capColourHex: "#FFD700",
};
const CITRATE = { id: 12, name: "Citrate 2.7 mL", capColourHex: "#87CEEB" };
const EDTA = { id: 13, name: "K2EDTA 4 mL", capColourHex: "#B57EDC" };

const group = (container, testIds, testNames, fields = {}) => ({
  containerTypeId: container.id,
  name: container.name,
  capColourHex: container.capColourHex,
  count: 1,
  testIds,
  testNames,
  panelIds: [],
  ...fields,
});

const view = (key, proposed, fields = {}) => ({
  key: String(key),
  proposed,
  unproposedTestIds: [],
  choices: [SST, CITRATE],
  containerRequired: proposed.length > 0,
  ...fields,
});

const requested = (fields = {}) => ({
  sampleTypeRequestId: 501,
  clientKey: "key-1",
  sampleItemId: "",
  sampleTypeId: "4",
  sampleTypeName: "Blood",
  collectionDate: "2026-11-12",
  collectionTime: "08:15",
  quantity: "4",
  tests: [
    { id: "1", name: "Glucose" },
    { id: "2", name: "PT/INR" },
    { id: "3", name: "APTT" },
  ],
  panels: [{ id: "90", name: "Coagulation Screen" }],
  ...fields,
});

describe("proposalRequest", () => {
  it("asks about every typed sample by position, with its tests and panels", () => {
    expect(
      proposalRequest([
        requested(),
        { sampleTypeId: "", tests: [] },
        { sampleTypeId: 7, tests: [{ id: 5 }], panels: [] },
      ]),
    ).toEqual([
      {
        key: "0",
        sampleTypeId: "4",
        testIds: ["1", "2", "3"],
        panelIds: ["90"],
      },
      { key: "2", sampleTypeId: "7", testIds: ["5"], panelIds: [] },
    ]);
  });

  it("changes its signature only when a type, test or panel changes", () => {
    const before = proposalSignature([requested()]);
    expect(proposalSignature([requested({ quantity: "9" })])).toBe(before);
    expect(
      proposalSignature([
        requested({ tests: [...requested().tests, { id: "4" }] }),
      ]),
    ).not.toBe(before);
  });
});

describe("applyProposals", () => {
  it("keeps the request on the first container and adds the others at the end", () => {
    const samples = [requested(), requested({ sampleTypeRequestId: 502 })];
    const outcome = applyProposals(samples, [
      view(0, [
        group(SST, ["1"], ["Glucose"]),
        group(CITRATE, ["2", "3"], ["PT/INR", "APTT"], {
          panelIds: ["90"],
          count: 2,
        }),
      ]),
    ]);

    expect(outcome.changed).toBe(true);
    expect(outcome.samples).toHaveLength(3);
    const [first, second, split] = outcome.samples;
    expect(first.sampleTypeRequestId).toBe(501);
    expect(first.clientKey).toBe("key-1");
    expect(first.containerTypeId).toBe("11");
    expect(first.proposed).toBe(true);
    expect(first.proposedFor).toEqual(["Glucose"]);
    expect(first.tests.map((test) => test.id)).toEqual(["1"]);
    expect(first.panels).toEqual([]);
    expect(second).toBe(samples[1]);

    expect(split.sampleTypeRequestId).toBe("");
    expect(split.clientKey).toBeUndefined();
    expect(split.sampleItemId).toBe("");
    expect(split.quantity).toBe("");
    expect(split.collectionDate).toBe("2026-11-12");
    expect(split.containerTypeId).toBe("12");
    expect(split.proposedSplit).toBe(true);
    expect(split.proposedCount).toBe(2);
    expect(split.tests.map((test) => test.id)).toEqual(["2", "3"]);
    expect(split.panels.map((panel) => panel.id)).toEqual(["90"]);
    expect(split.containerChoices).toEqual([SST, CITRATE]);
    expect(split.containerRequired).toBe(true);
    expect(outcome.added).toEqual([
      { name: "Citrate 2.7 mL", count: 2, testNames: ["PT/INR", "APTT"] },
    ]);
  });

  it("never puts tests without a proposal in a container drawn for tests needing their own", () => {
    const samples = [
      requested({
        proposed: true,
        containerTypeId: "11",
        tests: [
          { id: "2", name: "PT/INR" },
          { id: "3", name: "APTT" },
          { id: "8", name: "HDL" },
        ],
      }),
    ];
    const outcome = applyProposals(samples, [
      view(
        0,
        [
          group(CITRATE, ["2", "3"], ["PT/INR", "APTT"], {
            panelIds: ["90"],
            ownContainer: true,
          }),
        ],
        { unproposedTestIds: ["8"] },
      ),
    ]);

    const [remaining, own] = outcome.samples;
    expect(remaining.sampleTypeRequestId).toBe(501);
    expect(remaining.tests.map((test) => test.id)).toEqual(["8"]);
    expect(remaining.containerTypeId).toBe("");
    expect(remaining.proposed).toBe(false);
    expect(remaining.containerRequired).toBe(false);
    expect(own.tests.map((test) => test.id)).toEqual(["2", "3"]);
    expect(own.containerTypeId).toBe("12");
    expect(own.proposedSplit).toBe(true);

    const again = applyProposals(outcome.samples, [
      view(0, [], { unproposedTestIds: ["8"], containerRequired: false }),
      view(1, [
        group(CITRATE, ["2", "3"], ["PT/INR", "APTT"], {
          panelIds: ["90"],
          ownContainer: true,
        }),
      ]),
    ]);
    expect(again.changed).toBe(false);
  });

  it("keeps the request on a container drawn for tests needing their own when nothing is left over", () => {
    const outcome = applyProposals(
      [requested({ tests: [{ id: "2", name: "PT/INR" }] })],
      [view(0, [group(CITRATE, ["2"], ["PT/INR"], { ownContainer: true })])],
    );

    expect(outcome.samples).toHaveLength(1);
    expect(outcome.samples[0].containerTypeId).toBe("12");
  });

  it("puts a test added later into the tube its panel already has", () => {
    const shared = requested({
      tests: [
        { id: "1", name: "Glucose" },
        { id: "3", name: "APTT" },
      ],
    });
    const panelTube = requested({
      sampleTypeRequestId: "",
      clientKey: undefined,
      proposed: true,
      proposedSplit: true,
      proposedOwn: true,
      containerTypeId: "12",
      proposedFor: ["PT/INR"],
      tests: [{ id: "2", name: "PT/INR" }],
      panels: [{ id: "90", name: "Coagulation Screen" }],
    });
    const outcome = applyProposals(
      [shared, panelTube],
      [
        view(0, [
          group(SST, ["1"], ["Glucose"]),
          group(CITRATE, ["3"], ["APTT"], {
            panelIds: ["90"],
            ownContainer: true,
          }),
        ]),
        view(1, [
          group(CITRATE, ["2"], ["PT/INR"], {
            panelIds: ["90"],
            ownContainer: true,
          }),
        ]),
      ],
    );

    expect(outcome.samples).toHaveLength(2);
    expect(outcome.samples[0].tests.map((test) => test.id)).toEqual(["1"]);
    expect(outcome.samples[1].tests.map((test) => test.id)).toEqual(["2", "3"]);
    expect(outcome.samples[1].proposedFor).toEqual(["PT/INR", "APTT"]);
    expect(outcome.added).toEqual([]);
    expect(outcome.joined).toEqual([
      { name: "Citrate 2.7 mL", testNames: ["APTT"] },
    ]);
  });

  it("never joins a test needing its own container outside a panel to another tube", () => {
    const own = requested({
      sampleTypeRequestId: "",
      proposed: true,
      proposedSplit: true,
      proposedOwn: true,
      containerTypeId: "12",
      tests: [{ id: "2", name: "PT/INR" }],
      panels: [],
    });
    const outcome = applyProposals(
      [requested({ tests: [{ id: "1" }, { id: "3" }], panels: [] }), own],
      [
        view(0, [
          group(SST, ["1"], ["Glucose"]),
          group(CITRATE, ["3"], ["APTT"], { ownContainer: true }),
        ]),
        view(1, [group(CITRATE, ["2"], ["PT/INR"], { ownContainer: true })]),
      ],
    );

    expect(outcome.samples).toHaveLength(3);
    expect(outcome.samples[2].tests.map((test) => test.id)).toEqual(["3"]);
  });

  it("is stable once applied", () => {
    const first = applyProposals(
      [requested()],
      [
        view(0, [
          group(SST, ["1"], ["Glucose"]),
          group(CITRATE, ["2", "3"], ["PT/INR", "APTT"], { panelIds: ["90"] }),
        ]),
      ],
    );
    const again = applyProposals(first.samples, [
      view(0, [group(SST, ["1"], ["Glucose"])]),
      view(1, [
        group(CITRATE, ["2", "3"], ["PT/INR", "APTT"], { panelIds: ["90"] }),
      ]),
    ]);

    expect(again.changed).toBe(false);
    expect(again.added).toEqual([]);
  });

  it("never changes a saved or edited sample's container or tests", () => {
    const saved = requested({ sampleItemId: "77", containerTypeId: "99" });
    const edited = requested({ containerEdited: true, containerTypeId: "12" });
    const outcome = applyProposals(
      [saved, edited],
      [
        view(0, [group(SST, ["1"], ["Glucose"]), group(EDTA, ["2"], ["X"])]),
        view(1, [group(SST, ["1"], ["Glucose"]), group(EDTA, ["2"], ["X"])]),
      ],
    );

    expect(outcome.samples).toHaveLength(2);
    expect(outcome.samples[0].containerTypeId).toBe("99");
    expect(outcome.samples[0].tests).toHaveLength(3);
    expect(outcome.samples[0].containerChoices).toEqual([SST, CITRATE]);
    expect(outcome.samples[1].containerTypeId).toBe("12");
    expect(outcome.samples[1].tests).toHaveLength(3);
    expect(isProposable(saved)).toBe(false);
    expect(isProposable(edited)).toBe(false);
  });

  it("clears an edited unsaved sample's container that its type no longer holds", () => {
    const edited = requested({ containerEdited: true, containerTypeId: "13" });
    const outcome = applyProposals([edited], [view(0, [])]);

    expect(outcome.samples[0].containerTypeId).toBe("");
  });

  it("removes an untouched proposed sample whose tests were all removed", () => {
    const split = requested({
      sampleTypeRequestId: "",
      proposed: true,
      proposedSplit: true,
      proposedName: "Citrate 2.7 mL",
      containerTypeId: "12",
      tests: [],
      panels: [],
    });
    const outcome = applyProposals([requested(), split], [view(1, [])]);

    expect(outcome.samples).toHaveLength(1);
    expect(outcome.removed).toEqual([{ name: "Citrate 2.7 mL", count: 1 }]);
  });

  it("proposes nothing and requires nothing for tests the catalog names no container for", () => {
    const outcome = applyProposals(
      [requested({ proposed: true, containerTypeId: "11" })],
      [
        view(0, [], {
          unproposedTestIds: ["1", "2", "3"],
          containerRequired: false,
        }),
      ],
    );

    expect(outcome.samples[0].containerTypeId).toBe("");
    expect(outcome.samples[0].proposed).toBe(false);
    expect(outcome.samples[0].containerRequired).toBe(false);
    expect(outcome.samples[0].tests).toHaveLength(3);
  });

  it("leaves a sample the proposal was not asked about alone", () => {
    const untyped = { sampleTypeId: "", tests: [] };
    expect(applyProposals([untyped], []).samples[0]).toBe(untyped);
  });
});

describe("containerName", () => {
  it("names the sample's container from its choices", () => {
    expect(
      containerName({ containerTypeId: "12", containerChoices: [CITRATE] }),
    ).toBe("Citrate 2.7 mL");
    expect(containerName({ containerTypeId: "", containerChoices: [] })).toBe(
      "",
    );
  });
});
