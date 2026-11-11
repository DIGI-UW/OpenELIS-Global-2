import { focusFirstOf } from "./focus";

// The case view and the microtomy section pin the initiator and the field
// someone is typing in through the screen; these pin the two edges they do not.
describe("focusFirstOf", () => {
  const flush = () => new Promise((resolve) => setTimeout(resolve, 0));

  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("moves focus when nothing holds it", async () => {
    document.body.innerHTML =
      '<button id="initiator"></button><input id="target" />';

    focusFirstOf("#target", {
      onlyFrom: [document.getElementById("initiator")],
    });
    await flush();

    expect(document.activeElement).toBe(document.getElementById("target"));
  });

  it("passes over a disabled target to the next one", async () => {
    document.body.innerHTML =
      '<input id="first" disabled /><input id="second" />';

    focusFirstOf(["#first", "#second"]);
    await flush();

    expect(document.activeElement).toBe(document.getElementById("second"));
  });
});
