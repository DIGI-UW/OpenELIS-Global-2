import { Page, expect } from "@playwright/test";

/** API context path — the JSESSIONID is scoped to the /api/OpenELIS-Global webapp. */
const API_PREFIX = "/api/OpenELIS-Global";

/** CSRF token saved by auth.setup into storageState localStorage (key "CSRF"). */
async function getCsrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
}

export interface VectorSite {
  id?: string;
  code: string;
  name: string;
}

/**
 * Create a vector sampling site via the admin REST API (POST
 * /rest/admin/vector/sampling-sites) so an add-order E2E has a site to select.
 * Reference/transactional test data is seeded via the API, not SQL fixtures.
 * The caller should pass a unique code per run to avoid collisions.
 */
export async function seedVectorSite(
  page: Page,
  site: VectorSite,
): Promise<VectorSite> {
  const csrfToken = await getCsrfToken(page);
  const response = await page.request.post(
    `${API_PREFIX}/rest/admin/vector/sampling-sites`,
    {
      data: { code: site.code, name: site.name },
      headers: { "X-CSRF-Token": csrfToken },
    },
  );
  if (!response.ok()) {
    const text = await response.text().catch(() => "");
    throw new Error(
      `seedVectorSite: POST ${site.code} → HTTP ${response.status()} ${text.slice(0, 200)}`,
    );
  }
  const body = await response.json();
  return { id: body.id, code: site.code, name: site.name };
}

/**
 * Add a free-text requesting organization inline on the ENV/Vector order form.
 * ENV/Vector orders reject on save unless a requesting organization or a
 * requestor contact is present. The RequesterSection "+ Add new organization"
 * affordance sets newRequesterName; the backend creates the Organization at
 * save time. Pass a unique name so the debounced org search finds no match and
 * the create affordance appears.
 */
export async function addRequestingOrganization(
  page: Page,
  orgName: string,
): Promise<void> {
  await page.locator("#siteName").fill(orgName);
  // Debounced auto-search (300ms) returns no match for a fresh name, revealing
  // the inline "+ Add new organization" button.
  await page.getByRole("button", { name: /Add new organization/i }).click();
  await expect(
    page.locator(".selected-entity-card", { hasText: orgName }),
  ).toBeVisible({ timeout: 10_000 });
}

/** GET a JSON endpoint (session cookie carries auth); throws on non-2xx. */
async function apiGet<T = unknown>(page: Page, path: string): Promise<T> {
  const res = await page.request.get(`${API_PREFIX}${path}`);
  if (!res.ok()) {
    throw new Error(
      `GET ${path} → HTTP ${res.status()} ${(await res.text()).slice(0, 300)}`,
    );
  }
  return res.json() as Promise<T>;
}

/** POST JSON with the CSRF token; throws on non-2xx. */
async function apiPost(page: Page, path: string, data: unknown): Promise<void> {
  const csrfToken = await getCsrfToken(page);
  const res = await page.request.post(`${API_PREFIX}${path}`, {
    data: data as object,
    headers: { "X-CSRF-Token": csrfToken },
  });
  if (!res.ok()) {
    throw new Error(
      `POST ${path} → HTTP ${res.status()} ${(await res.text()).slice(0, 300)}`,
    );
  }
}

/**
 * Seed a POSITIVE Anopheles pool through the real workflow (no SQL): order a
 * Mosquito collection with both pathogen tests via the UI, identify its
 * specimens as Anopheles (CONFIRMED), then enter POSITIVE results via
 * LogbookResults. This produces the data the dashboard's MIR, positivity, and
 * sporozoite panels compute from — the catalog ships the significance
 * classification (configuration/test-results), this seeds the transactional
 * results that reference it.
 */
export async function seedVectorPositivity(
  page: Page,
): Promise<{ labNo: string; siteName: string }> {
  const stamp = Date.now().toString().slice(-6);
  const siteCode = `E2E-POS-${stamp}`;
  const siteName = `E2E Positivity Site ${stamp}`;
  await seedVectorSite(page, { code: siteCode, name: siteName });

  // 1. Order a Mosquito collection with both pathogen tests (creates analyses).
  await page.goto("/order/vector/enter");
  await page.locator(".generate-link").click();
  await expect(page.locator("#labNumber")).not.toHaveValue("", {
    timeout: 15_000,
  });
  const labNo = await page.locator("#labNumber").inputValue();

  await page.locator("#vec-site-search").fill(siteName);
  await page
    .locator(".search-results tr", { hasText: siteCode })
    .getByRole("button", { name: "Select" })
    .click();
  await page.locator("#sampleType-0").selectOption({ label: "Mosquito" });

  // Order both pathogen tests. Filter the test list, then click the (Carbon
  // visually-hidden) checkbox via its label.
  for (const testTerm of ["Malaria", "CSP"]) {
    await page.locator("#testSearch-0").fill(testTerm);
    await page.locator('label[for^="test-0-"]').first().click();
  }
  await page.locator("#collectedVolume-0").fill("8");
  // A requesting organization (or requestor) is required to save an ENV/Vector order.
  await addRequestingOrganization(page, `E2E Positivity Org ${stamp}`);
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    page.getByText("Sample Order Entry has been saved successfully").first(),
  ).toBeVisible({ timeout: 20_000 });

  // 2. Identify the pool's specimens as Anopheles (CONFIRMED) — MIR is by species.
  const species = await apiGet<Array<{ id: number; genus: string }>>(
    page,
    "/rest/admin/vector/species",
  );
  const anopheles = species.find(
    (s) => (s.genus || "").toLowerCase() === "anopheles",
  );
  if (!anopheles) {
    throw new Error(
      "seedVectorPositivity: no Anopheles species in the catalog",
    );
  }
  const worklist = await apiGet<
    Array<{ sampleId: number; vectorPoolId: number; accessionNumber: string }>
  >(page, "/rest/vector/identification/worklist");
  const row = worklist.find((r) => r.accessionNumber === labNo);
  if (!row) {
    throw new Error(`seedVectorPositivity: no worklist row for ${labNo}`);
  }
  // getSpecimensForLot is keyed by the vector pool id (as the UI passes it).
  const specimens = await apiGet<Array<{ sampleItemId: number }>>(
    page,
    `/rest/vector/identification/lots/${row.vectorPoolId}/specimens`,
  );
  await apiPost(page, "/rest/vector/identification/specimens/bulk-identify", {
    sampleItemIds: specimens.map((s) => s.sampleItemId),
    vectorSpeciesId: anopheles.id,
    identificationMethod: "MORPHOLOGICAL",
    confidence: "CONFIRMED",
  });

  // 3. Enter POSITIVE results for both pathogen analyses via LogbookResults.
  await enterResult(page, labNo, true);

  return { labNo, siteName };
}

/**
 * For an existing vector order (by lab number), identify its pool's specimens as
 * the given genus (CONFIRMED) and enter a POSITIVE result for its pathogen
 * analyses. Lets a demo/test that creates an order through the UI drive it on to
 * species/MIR/positivity without re-implementing the REST workflow. Mirrors
 * seedVectorPositivity's identify + result logic.
 */
export async function identifyAndResolvePositive(
  page: Page,
  labNumber: string,
  genus = "anopheles",
): Promise<void> {
  const species = await apiGet<Array<{ id: number; genus: string }>>(
    page,
    "/rest/admin/vector/species",
  );
  const match = species.find(
    (s) => (s.genus || "").toLowerCase() === genus.toLowerCase(),
  );
  if (!match) {
    throw new Error(
      `identifyAndResolvePositive: no ${genus} species in catalog`,
    );
  }
  const worklist = await apiGet<
    Array<{ vectorPoolId: number; accessionNumber: string }>
  >(page, "/rest/vector/identification/worklist");
  const row = worklist.find((r) => r.accessionNumber === labNumber);
  if (!row) {
    throw new Error(
      `identifyAndResolvePositive: no worklist row for ${labNumber}`,
    );
  }
  const specimens = await apiGet<Array<{ sampleItemId: number }>>(
    page,
    `/rest/vector/identification/lots/${row.vectorPoolId}/specimens`,
  );
  await apiPost(page, "/rest/vector/identification/specimens/bulk-identify", {
    sampleItemIds: specimens.map((s) => s.sampleItemId),
    vectorSpeciesId: match.id,
    identificationMethod: "MORPHOLOGICAL",
    confidence: "CONFIRMED",
  });

  await enterResult(page, labNumber, true);
}

/** N most-recent Mondays (ISO yyyy-mm-dd), oldest first — backdated collection weeks. */
function recentMondays(n: number): string[] {
  const base = new Date();
  base.setDate(base.getDate() - ((base.getDay() + 6) % 7)); // back to this week's Monday
  const out: string[] = [];
  for (let i = n - 1; i >= 0; i--) {
    const w = new Date(base);
    w.setDate(base.getDate() - i * 7);
    out.push(w.toISOString().slice(0, 10));
  }
  return out;
}

interface Lane {
  site: string;
  genus: string;
  epithet: string;
  testTerm: string;
  baseQty: number;
  traps: number; // 0 = record no trap effort (demonstrates the density degrade)
  nights: number;
  posEvery: number;
}

/**
 * Create a vector collection via the REST API — modeled on seed-tat-data's
 * createSampleOrder (same /rest/SamplePatientEntry endpoint + accession-gen).
 * A vector order is the clinical order minus the patient (orderEntryOnly:true,
 * patientUpdateStatus NO_ACTION) plus environmentalFields (site + trap-effort)
 * and a Mosquito sample tag with collectionLocationId. Backdatable via the
 * sample `date` attribute. Returns the lab number.
 */
export async function createVectorOrder(
  page: Page,
  o: {
    siteId: number | string;
    siteName: string;
    siteCode: string;
    dateIso: string;
    quantity: number;
    traps: number;
    nights: number;
    testName: string;
    requesterName?: string;
  },
): Promise<string> {
  // Browser session context for the CSRF fetches (as createSampleOrder does).
  await page.goto("/order/vector/enter", {
    waitUntil: "domcontentloaded",
    timeout: 15_000,
  });

  // The order endpoints parse dates with a locale-dependent DateFormat.SHORT
  // (keyed off DEFAULT_DATE_LOCALE, or the JVM default locale when unset), not
  // ISO. Rather than assume a locale, ask the server how it renders today and
  // reorder any ISO date into that same field order — the server is the oracle.
  const { currentDate = "" } = await apiGet<{ currentDate?: string }>(
    page,
    "/rest/SamplePatientEntry",
  );
  const todayIso = new Date().toISOString().slice(0, 10);
  const tomorrowIso = new Date(Date.now() + 86_400_000)
    .toISOString()
    .slice(0, 10);
  const sep = currentDate.includes("/") ? "/" : "-";
  const srv = currentDate.split(sep);
  const [ty, tm, td] = todayIso.split("-");
  const at = { y: srv.indexOf(ty), m: srv.indexOf(tm), d: srv.indexOf(td) };
  const fmt = (iso: string) => {
    const [y, m, d] = iso.split("-");
    if (at.y < 0 || at.m < 0 || at.d < 0 || at.m === at.d) {
      return `${d}/${m}/${y}`; // server unreachable or today's day==month: day-first
    }
    const out: string[] = [];
    out[at.d] = d;
    out[at.m] = m;
    out[at.y] = y;
    return out.join(sep);
  };

  // Look up the Mosquito sample-type id + the orderable pathogen test id.
  const sampleTypes = await apiGet<Array<{ id: string; value: string }>>(
    page,
    "/rest/vector-sample-types",
  );
  const mosquito = sampleTypes.find((s) => s.value === "Mosquito");
  if (!mosquito) throw new Error("createVectorOrder: no Mosquito sample type");
  const stt = await apiGet<{
    tests?: Array<{ id: string; name?: string; value?: string }>;
  }>(page, `/rest/sample-type-tests?sampleType=${mosquito.id}`);
  const test = (stt.tests || []).find((t) =>
    (t.name || t.value || "").toLowerCase().includes(o.testName.toLowerCase()),
  );
  if (!test) {
    throw new Error(
      `createVectorOrder: no orderable test matching '${o.testName}' for Mosquito`,
    );
  }

  const csrf = await getCsrfToken(page);
  const genRes = await page.request.get(
    `${API_PREFIX}/rest/SampleEntryGenerateScanProvider`,
    { headers: { "X-CSRF-Token": csrf } },
  );
  const labNo = (JSON.parse(await genRes.text()).body as string) || "";
  if (!labNo) throw new Error("createVectorOrder: accession generation failed");

  const sampleXML =
    `<?xml version="1.0" encoding="utf-8"?><samples requiredBy=''>` +
    `<sample sampleID='1' typeId='${mosquito.id}' sampleItemId='' date='${fmt(o.dateIso)}' time='08:00' ` +
    `collector='' collectionConditions='' quantity='${o.quantity}' uom='' receivedDate='${fmt(todayIso)}' receivedTime='08:00' ` +
    `tests='${test.id}' testSectionMap='' testSampleTypeMap='' panels='' rejected='false' rejectReasonId='' ` +
    `initialConditionIds='' storageLocationId='' storageLocationType='' storagePositionCoordinate='' ` +
    `gpsLatitude='' gpsLongitude='' gpsAccuracy='' gpsCaptureMethod='' container='' locationDetails='' ` +
    `labPerformedSampling='false' collectionLocationId='${o.siteId}' qcType='' qcParentSampleIndex='' qcExpectedValue=''/></samples>`;

  const form = {
    orderEntryOnly: true,
    useReferral: false,
    rememberSiteAndRequester: false,
    referralItems: [],
    sampleTypes: null,
    patientUpdateStatus: "NO_ACTION",
    sampleXML,
    patientProperties: { patientPK: "", patientUpdateStatus: "NO_ACTION" },
    sampleOrderItems: {
      labNo,
      requestDate: fmt(todayIso),
      receivedDateForDisplay: fmt(todayIso),
      receivedTime: "08:00",
      nextVisitDate: fmt(tomorrowIso),
      priority: "ROUTINE",
      referringSiteId: "",
      providerId: "",
      programId: "",
      // ENV/Vector orders require a requesting organization or requestor
      // contact. A free-text newRequesterName is created as an Organization on
      // save (mirrors the RequesterSection "+ Add new organization" affordance).
      newRequesterName: o.requesterName || "Vector Surveillance Programme",
      orderTypes: [],
      referringSiteList: [],
      referringSiteDepartmentList: [],
      providersList: [],
      paymentOptions: [],
      programList: [],
      priorityList: [],
      testLocationCodeList: [],
      modified: true,
      sampleId: "",
      environmentalFields: {
        workflowType: "vector",
        vecCollectionSiteId: String(o.siteId),
        vecCollectionSiteName: o.siteName,
        vecCollectionSiteCode: o.siteCode,
        vecCollectionDate: o.dateIso,
        vecTrapCount: o.traps > 0 ? String(o.traps) : "",
        vecTrapNights: o.nights > 0 ? String(o.nights) : "",
      },
    },
    initialSampleConditionList: [],
    testSectionList: [],
  };

  const res = await page.request.post(`${API_PREFIX}/rest/SamplePatientEntry`, {
    data: form,
    headers: { "X-CSRF-Token": csrf },
  });
  if (!res.ok()) {
    throw new Error(
      `createVectorOrder: POST ${labNo} → HTTP ${res.status()} ${(await res.text()).slice(0, 250)}`,
    );
  }
  return labNo;
}

/** Identify a collection's pool specimens as a specific species (falls back to genus). */
async function identifyAs(
  page: Page,
  labNo: string,
  genus: string,
  epithet: string,
): Promise<void> {
  const species = await apiGet<
    Array<{ id: number; genus: string; species?: string }>
  >(page, "/rest/admin/vector/species");
  const g = genus.toLowerCase();
  const sp =
    species.find(
      (s) =>
        (s.genus || "").toLowerCase() === g &&
        (s.species || "").toLowerCase() === epithet.toLowerCase(),
    ) || species.find((s) => (s.genus || "").toLowerCase() === g);
  if (!sp) throw new Error(`seed: no ${genus} species in catalog`);
  const worklist = await apiGet<
    Array<{ vectorPoolId: number; accessionNumber: string }>
  >(page, "/rest/vector/identification/worklist");
  const row = worklist.find((r) => r.accessionNumber === labNo);
  if (!row) throw new Error(`seed: no worklist row for ${labNo}`);
  const specimens = await apiGet<Array<{ sampleItemId: number }>>(
    page,
    `/rest/vector/identification/lots/${row.vectorPoolId}/specimens`,
  );
  await apiPost(page, "/rest/vector/identification/specimens/bulk-identify", {
    sampleItemIds: specimens.map((s) => s.sampleItemId),
    vectorSpeciesId: sp.id,
    identificationMethod: "MORPHOLOGICAL",
    confidence: "CONFIRMED",
  });
}

/** Enter a positive or negative pathogen result, selecting by significance-appropriate value. */
async function enterResult(
  page: Page,
  labNo: string,
  positive: boolean,
): Promise<void> {
  const logbook = await apiGet<{
    testResult?: Array<Record<string, unknown>>;
  }>(page, `/rest/LogbookResults?labNumber=${labNo}`);
  const body = JSON.parse(JSON.stringify(logbook));
  const isPos = (v: string) => /^(detected|positive)$/i.test((v || "").trim());
  const isNeg = (v: string) =>
    /^(not detected|negative)$/i.test((v || "").trim());
  for (const item of body.testResult ?? []) {
    const dict = (item.dictionaryResults ?? []) as Array<{
      id: string;
      value: string;
    }>;
    const opt = positive
      ? dict.find((d) => isPos(d.value))
      : dict.find((d) => isNeg(d.value));
    if (!opt) continue;
    item.reportable = item.reportable === "N" ? false : true;
    item.resultValue = opt.id;
    item.shadowResultValue = opt.id;
    item.isModified = true;
    delete item.result;
  }
  await apiPost(page, "/rest/LogbookResults", body);
}

/**
 * Seed a representative vector-surveillance dataset entirely through the real
 * paths (NO SQL): sites via the admin API, collections via the order-entry REST
 * endpoint (backdated over N weeks, with trap-effort), species identification +
 * pathogen results via REST. Populates every dashboard panel (density, species, MIR,
 * positivity, sporozoite, QC) against the config-imported catalog. Run pointed
 * at any instance via BASE_URL. Denpasar records no trap effort → density degrade.
 */
export async function seedVectorSurveillanceDataset(
  page: Page,
  opts: { weeks?: number } = {},
): Promise<{
  ordersCreated: number;
  skippedLanes: string[];
  densityRows: number;
}> {
  const weeks = recentMondays(opts.weeks ?? 5);
  const sites = [
    { code: "IDN-KPG", name: "Kupang" },
    { code: "IDN-JYP", name: "Jayapura" },
    { code: "IDN-JKT", name: "Jakarta Utara" },
    { code: "IDN-SBY", name: "Surabaya" },
    { code: "IDN-DPS", name: "Denpasar" },
  ];
  const lanes: Lane[] = [
    {
      site: "Kupang",
      genus: "anopheles",
      epithet: "sundaicus",
      testTerm: "Malaria",
      baseQty: 12,
      traps: 4,
      nights: 2,
      posEvery: 3,
    },
    {
      site: "Kupang",
      genus: "anopheles",
      epithet: "sundaicus",
      testTerm: "CSP",
      baseQty: 12,
      traps: 4,
      nights: 2,
      posEvery: 5,
    },
    {
      site: "Jayapura",
      genus: "anopheles",
      epithet: "farauti",
      testTerm: "Malaria",
      baseQty: 10,
      traps: 3,
      nights: 2,
      posEvery: 3,
    },
    {
      site: "Jayapura",
      genus: "anopheles",
      epithet: "maculatus",
      testTerm: "CSP",
      baseQty: 8,
      traps: 3,
      nights: 2,
      posEvery: 4,
    },
    {
      site: "Jakarta Utara",
      genus: "aedes",
      epithet: "aegypti",
      testTerm: "Dengue",
      baseQty: 14,
      traps: 5,
      nights: 2,
      posEvery: 3,
    },
    {
      site: "Surabaya",
      genus: "aedes",
      epithet: "albopictus",
      testTerm: "Dengue",
      baseQty: 11,
      traps: 4,
      nights: 2,
      posEvery: 3,
    },
    {
      site: "Denpasar",
      genus: "culex",
      epithet: "quinquefasciatus",
      testTerm: "Japanese",
      baseQty: 9,
      traps: 0,
      nights: 0,
      posEvery: 4,
    },
  ];
  // List-first, create-only-if-absent so the seed is idempotent (a rebuilt
  // demo or a prior partial run may already hold some sites). Every lane needs
  // its site id for the collection's collectionLocationId.
  const existing = await apiGet<
    Array<{ id: string; code: string; name: string }>
  >(page, "/rest/admin/vector/sampling-sites");
  const byCode = new Map(existing.map((e) => [e.code, e]));
  const siteByName = new Map<string, VectorSite>();
  for (const s of sites) {
    const found = byCode.get(s.code);
    const resolved = found ?? (await seedVectorSite(page, s));
    siteByName.set(s.name, { id: resolved.id, code: s.code, name: s.name });
  }

  // Which pathogen tests are orderable for Mosquito on THIS instance? A lane
  // whose test isn't configured is skipped (loudly), not failed — the classpath
  // ships Malaria+CSP; the distro config adds Dengue+JEV. This keeps the seed
  // portable and honest (degrade cleanly, never fabricate a lane's data).
  const sampleTypes = await apiGet<Array<{ id: string; value: string }>>(
    page,
    "/rest/vector-sample-types",
  );
  const mosquitoId = sampleTypes.find((s) => s.value === "Mosquito")?.id;
  const availableTests = mosquitoId
    ? ((
        await apiGet<{ tests?: Array<{ name?: string; value?: string }> }>(
          page,
          `/rest/sample-type-tests?sampleType=${mosquitoId}`,
        )
      ).tests ?? [])
    : [];
  const hasTest = (term: string) =>
    availableTests.some((t) =>
      (t.name || t.value || "").toLowerCase().includes(term.toLowerCase()),
    );

  let ordersCreated = 0;
  const skippedLanes: string[] = [];
  for (const lane of lanes) {
    if (!hasTest(lane.testTerm)) {
      console.warn(
        `seedVectorSurveillanceDataset: SKIPPING ${lane.site}/${lane.testTerm}` +
          ` — no orderable '${lane.testTerm}' test for Mosquito on this instance`,
      );
      skippedLanes.push(`${lane.site}/${lane.testTerm}`);
      continue;
    }
    const site = siteByName.get(lane.site)!;
    for (let w = 0; w < weeks.length; w++) {
      const positive = w % lane.posEvery === 0;
      const labNo = await createVectorOrder(page, {
        siteId: site.id!,
        siteName: site.name,
        siteCode: site.code,
        dateIso: weeks[w],
        quantity: lane.baseQty + (w % 3),
        traps: lane.traps,
        nights: lane.nights,
        testName: lane.testTerm,
      });
      await identifyAs(page, labNo, lane.genus, lane.epithet);
      await enterResult(page, labNo, positive);
      ordersCreated++;
    }
  }

  // Verify the seed populated the dashboard. A helper may read the backend
  // (the demo spec may not, per the pw-demo-no-backend-access lint rule), so
  // the real end-to-end check lives here: density rows prove collections +
  // trap-effort landed and are computable by the surveillance report.
  const indices = await apiGet<{ collectionDensity?: unknown[] }>(
    page,
    "/rest/reports/vector-surveillance/indices",
  );
  const densityRows = indices.collectionDensity?.length ?? 0;

  return { ordersCreated, skippedLanes, densityRows };
}
