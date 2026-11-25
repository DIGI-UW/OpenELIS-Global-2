import fs from "node:fs";
import path from "node:path";
import { afterEach, beforeEach, expect, it, vi } from "vitest";

const root = path.resolve(process.cwd(), "..");
const read = (file) => fs.readFileSync(path.join(root, file), "utf8");
const pages = "src/main/webapp/pages/";
const scripts = "src/main/resources/static/scripts/";
const payload = `O'Brien <img src=x onerror="window.injected=true"> & </script>`;

// Execute the actual legacy function, without running unrelated page startup code.
function functionSource(file, name) {
  const source = read(file);
  const start = source.indexOf(`function ${name}(`);
  if (start < 0) throw new Error(`Missing ${name}`);
  const open = source.indexOf("{", start);
  let depth = 0,
    quote = null,
    lineComment = false,
    blockComment = false;
  for (let i = open; i < source.length; i++) {
    const c = source[i],
      next = source[i + 1];
    if (lineComment) {
      if (c === "\n") lineComment = false;
      continue;
    }
    if (blockComment) {
      if (c === "*" && next === "/") {
        blockComment = false;
        i++;
      }
      continue;
    }
    if (quote) {
      if (c === "\\") i++;
      else if (c === quote) quote = null;
      continue;
    }
    if (c === "/" && next === "/") {
      lineComment = true;
      i++;
      continue;
    }
    if (c === "/" && next === "*") {
      blockComment = true;
      i++;
      continue;
    }
    if (c === "'" || c === '"' || c === "`") {
      quote = c;
      continue;
    }
    if (c === "{") depth++;
    if (c === "}" && --depth === 0) return source.slice(start, i + 1);
  }
  throw new Error(`Unclosed ${name}`);
}
let $;
beforeEach(() => {
  document.body.replaceChildren();
  new Function("window", "document", read(scripts + "jquery-1.8.0.min.js"))(
    window,
    document,
  );
  $ = window.jQuery;
});
afterEach(() => {
  document.body.replaceChildren();
  vi.restoreAllMocks();
});

it("renders electronic-order names as text and keeps radio callbacks intact", () => {
  document.body.innerHTML = '<div id="crossPanels"></div>';
  const panel = vi.fn(),
    test = vi.fn();
  const harness = new Function(
    "document",
    "panel",
    "test",
    read(scripts + "laborder.js") +
      `
    crossPanelSelected=panel; crossTestSelected=test;
    sampleTypeOrder=0; crossSampleTypeOrderMap={0:'7'};
    crossSampleTypeMap={'7':{name:${JSON.stringify(payload)}}};
    return populateCrossPanelsAndTests;
  `,
  )(document, panel, test);
  const typeMap = { [payload]: "t" };
  harness(
    [{ id: "9", name: payload, typeMap }],
    [{ name: payload, typeMap }],
    "29/10/2026",
  );
  expect(document.querySelectorAll("img,script")).toHaveLength(0);
  expect(document.getElementById("crossPanels").textContent).toContain(payload);
  const radios = document.querySelectorAll('input[type="radio"]');
  expect(radios).toHaveLength(2);
  radios[0].click();
  radios[1].click();
  expect(panel).toHaveBeenCalledWith("9", payload, "7", payload, "29/10/2026");
  expect(test).toHaveBeenCalledWith(payload, "7", payload, "29/10/2026");
});

it.each(["testAdd", "testModify", "testActivation"])(
  "keeps %s sortable names as text beside the drag handle",
  (name) => {
    const create = new Function(
      "jQuery",
      "document",
      functionSource(pages + `testConfiguration/${name}.jsp`, "createLI") +
        ";return createLI;",
    )($, document);
    const item =
      name === "testActivation"
        ? create(payload, 12, true)
        : create(12, payload, true);
    document.body.appendChild(item[0]);
    expect(item.text()).toBe(payload);
    expect(item.find("span")).toHaveLength(1);
    expect(item.find("img,script")).toHaveLength(0);
  },
);

it.each([
  "patient/patientEnhancedSearch.jsp",
  "patient/patientSearch.jsp",
  "barcode/printBarcode.jsp",
  "samplebatchentry/routine/patientSearch.jsp",
])("%s preserves patient text and selection", (file) => {
  const select = vi.fn();
  const create = new Function(
    "document",
    "nonNullString",
    "selectPatient",
    `var localDB=false,supportMothersName=true,supportSTNumber=true,supportSubjectNumber=true,supportNationalID=true;` +
      functionSource(pages + file, "createRow") +
      ";return createRow;",
  )(document, (v) => v ?? "", select);
  const table = document.createElement("table");
  document.body.appendChild(table);
  create(
    table,
    payload,
    payload,
    "F",
    "01/01/1990",
    payload,
    payload,
    payload,
    payload,
    payload,
    payload,
  );
  expect(table.querySelectorAll("img,script")).toHaveLength(0);
  expect(table.textContent).toContain(payload);
  table.querySelector('input[type="radio"]').click();
  expect(select).toHaveBeenCalledWith(payload);
});

it("corrective-action search rows keep XML-decoded values as text", () => {
  const create = new Function(
    "document",
    functionSource(pages + "qaevent/correctiveAction.jsp", "addNCERow") +
      ";return addNCERow;",
  )(document);
  const xml = new DOMParser().parseFromString(
    "<nce><date/><ncenumber/><unit/><color/></nce>",
    "application/xml",
  );
  for (const node of xml.documentElement.children) node.textContent = payload;
  const row = create(xml.documentElement);
  document.body.appendChild(row);
  expect(row.querySelectorAll("img,script")).toHaveLength(0);
  expect(row.querySelector("a").textContent).toBe(payload);
  expect(
    new URL(row.querySelector("a").href).searchParams.get("nceNumber"),
  ).toBe(payload);
});

it("autocomplete option labels cannot create markup", () => {
  new Function("window", "document", "jQuery", read(scripts + "jquery-ui.js"))(
    window,
    document,
    $,
  );
  new Function(
    "jQuery",
    "autoCompleteWidth",
    read(scripts + "customAutocomplete.js"),
  )($, 250);
  const select = $(
    '<select combobox-id="site"><option value="1"></option></select>',
  ).appendTo(document.body);
  select.find("option").text(payload);
  select.combobox();
  const widget = $("#site").data("ui-autocomplete");
  const list = $("<ul></ul>").appendTo(document.body);
  widget._renderItem(list, { label: payload, value: payload });
  expect(list.text()).toBe(payload);
  expect(list.find("img,script")).toHaveLength(0);
});

it.each(["testAdd", "testModify"])(
  "%s confirmation preserves panel and unit names as text",
  (name) => {
    document.body.innerHTML =
      '<select id="panelSelection" multiple><option selected></option></select><select id="uomSelection"><option selected></option></select><div id="panelRO"></div><div id="uomRO"></div>';
    $("#panelSelection option, #uomSelection option").text(payload);
    const confirm = new Function(
      "jQuery",
      "document",
      functionSource(
        pages + `testConfiguration/${name}.jsp`,
        "setStep1ReadOnlyFields",
      ) + ";return setStep1ReadOnlyFields;",
    )($, document);
    confirm();
    expect($("#panelRO").text()).toBe(payload);
    expect($("#panelRO br")).toHaveLength(1);
    expect($("#uomRO").text()).toBe(payload);
    expect(document.querySelectorAll("img,script")).toHaveLength(0);
  },
);

it("dictionary rename displays catalogue values as text", () => {
  document.body.innerHTML =
    '<div class="english"></div><div class="french"></div>';
  const edit = new Function(
    "jQuery",
    "options",
    "localization",
    functionSource(
      pages + "testConfiguration/resultSelectListRename.jsp",
      "editName",
    ) + ";return editName;",
  )($, { one: payload }, { one: "" });
  edit("one");
  expect($(".english").text()).toBe(payload);
  expect($(".french").text()).toBe(payload);
  expect(document.querySelectorAll("img,script")).toHaveLength(0);
});

it("referral order identifiers remain one URL parameter and close callbacks remain bound", () => {
  document.body.innerHTML = '<div id="iframecontainer_9"></div>';
  const close = vi.fn();
  const source = functionSource(
    pages + "fhirReferral/referral.jsp",
    "enterOrder",
  ).replace(/<spring:message[^>]*\/>/g, "Exit");
  const enter = new Function(
    "document",
    "closeOrder",
    source + ";return enterOrder;",
  )(document, close);
  enter("9", payload + "&other=value#fragment");
  const frame = document.querySelector("iframe");
  expect(new URL(frame.src).searchParams.get("ID")).toBe(
    payload + "&other=value#fragment",
  );
  expect(new URL(frame.src).searchParams.get("other")).toBeNull();
  document.querySelector("button").click();
  expect(close).toHaveBeenCalledWith("9");
});

it("referral choices display catalogue names as text", () => {
  document.body.innerHTML =
    '<table><tbody><tr id="referralRow_3"></tr></tbody></table><input id="testDate_3" value="29/10/2026">';
  const source = functionSource(
    pages + "result/resultListView.jsp",
    "createReferralOption",
  )
    .replace(/<c:(?:forEach|if)\b[^>]*>|<\/c:(?:forEach|if)>/g, "")
    .replace(
      /\$\{enc:js\((?:referralReason|referralOrganization)\.id\)\}/g,
      "9",
    )
    .replace(
      /\$\{enc:js\((?:referralReason|referralOrganization)\.value\)\}/g,
      () => JSON.stringify(payload).slice(1, -1).replace(/'/g, "\\u0027"),
    );
  const create = new Function(
    "document",
    "jQuery",
    source + ";return createReferralOption;",
  )(document, $);
  create("1", "2", "17", payload, 3);
  expect(document.querySelectorAll("img,script")).toHaveLength(0);
  const selects = document.querySelectorAll("select");
  expect(selects).toHaveLength(3);
  for (const select of selects) expect(select.textContent).toContain(payload);
  expect(selects[2].value).toBe("17");
  expect(document.getElementById("sendDate_3").value).toBe("29/10/2026");
});
