import { test, expect, Page } from "../../../helpers/test-base";
import { SiteInformationPage } from "../../../fixtures/esig-admin";

const API = "/api/OpenELIS-Global/rest/esig";

const CERTIFICATION_TEXT =
  "I understand that by providing my login credentials, I am creating a legally " +
  "binding electronic signature that carries the same weight as my handwritten " +
  "signature. I certify that I am the sole owner of these credentials and that " +
  "I will not share them with anyone else.";

// ── CSRF helper ─────────────────────────────────────────────────

/** Extract CSRF token from the page context's storageState. */
async function getCsrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
}

/** Build headers with CSRF token for authenticated API calls. */
async function csrfHeaders(page: Page): Promise<Record<string, string>> {
  return { "X-CSRF-Token": await getCsrfToken(page) };
}

// ── Helpers ──────────────────────────────────────────────────────

/** Sign a record and return the response body. Fails the test on non-200. */
async function sign(
  page: Page,
  opts: {
    username: string;
    password: string;
    meaning: string;
    recordType: string;
    recordId: number;
    rejectionReason?: string;
  },
) {
  const headers = await csrfHeaders(page);
  const res = await page.request.post(`${API}/sign`, {
    headers,
    data: {
      username: opts.username,
      password: opts.password,
      signatureMeaning: opts.meaning,
      recordType: opts.recordType,
      recordId: opts.recordId,
      rejectionReason: opts.rejectionReason,
    },
  });
  expect(res.status(), `sign(${opts.meaning}) should return 200`).toBe(200);
  return res.json();
}

/** Query all signatures for a record and return the array. */
async function querySignatures(
  page: Page,
  recordType: string,
  recordId: number,
) {
  const res = await page.request.get(
    `${API}/signatures?recordType=${recordType}&recordId=${recordId}`,
  );
  expect(res.status()).toBe(200);
  return res.json();
}

/** Certify user; fails test on non-200. */
async function certify(page: Page, username: string, password: string) {
  const headers = await csrfHeaders(page);
  const res = await page.request.post(`${API}/certify`, {
    headers,
    data: { username, password, certificationText: CERTIFICATION_TEXT },
  });
  expect(res.status(), "certify should return 200").toBe(200);
  return res.json();
}

/** Revoke certification (best-effort, ignores errors). */
async function revokeCertification(page: Page, username: string) {
  const headers = await csrfHeaders(page);
  await page.request.delete(`${API}/admin/certifications/${username}`, {
    headers,
  });
}

// ── Tests ────────────────────────────────────────────────────────

/**
 * Electronic Signature E2E — 21 CFR Part 11
 */
// Serial: tests build state sequentially (revoke → certify → sign → check session)
test.describe
  .serial("Electronic Signature — 21 CFR Part 11 (API contract)", () => {
  const username = process.env.TEST_USER || "admin";
  const password = process.env.TEST_PASS || "adminADMIN!";

  // Unique per run so signatures from prior runs don't pollute queries
  const runId = Date.now();
  const REC_TYPE = "RESULT";

  // Ensure e-sig is enabled before running API tests.
  // The Liquibase default is 'false', so toggle it via the admin UI.
  test.beforeAll(async ({ browser }) => {
    const ctx = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    const page = await ctx.newPage();
    const siteInfo = new SiteInformationPage(page);
    await siteInfo.goto();
    await siteInfo.setBooleanSetting("electronicSignatureEnabled", true);
    await ctx.close();
  });

  // Always restore certification after this suite, even if a test fails,
  // so that other specs (esig-result-validation) are not affected.
  test.afterAll(async ({ browser }) => {
    const ctx = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    const page = await ctx.newPage();
    await page.goto("/", { waitUntil: "domcontentloaded", timeout: 15_000 });
    const headers = await csrfHeaders(page);
    // Best-effort: re-certify if decertified
    const check = await page.request.get(`${API}/certified/${username}`);
    if (!(await check.json()).certified) {
      await page.request.post(`${API}/certify`, {
        headers,
        data: {
          username,
          password,
          certificationText: CERTIFICATION_TEXT,
        },
      });
    }
    await ctx.close();
  });

  // ════════════════════════════════════════════
  // §11.100(c) — Certification gate
  // ════════════════════════════════════════════

  test("Uncertified user cannot sign — certification is a hard gate", async ({
    page,
  }) => {
    const headers = await csrfHeaders(page);
    // Ensure clean: revoke any prior certification
    await revokeCertification(page, username);

    // Confirm the user is actually decertified (read-back, not trust)
    const certCheck = await page.request.get(`${API}/certified/${username}`);
    const certBody = await certCheck.json();
    expect(certBody.certified, "user should be decertified").toBe(false);

    // Attempt to sign — must fail because uncertified
    const signRes = await page.request.post(`${API}/sign`, {
      headers,
      data: {
        username,
        password,
        signatureMeaning: "AUTHORED",
        recordType: REC_TYPE,
        recordId: runId,
      },
    });
    expect(signRes.status()).toBe(400);

    // Verify NO signature was persisted — the gate must actually block
    const sigs = await querySignatures(page, REC_TYPE, runId);
    expect(sigs, "no signatures should exist for a blocked attempt").toEqual(
      [],
    );
  });

  test("Certification requires valid credentials — wrong password is rejected and state unchanged", async ({
    page,
  }) => {
    const headers = await csrfHeaders(page);
    const res = await page.request.post(`${API}/certify`, {
      headers,
      data: {
        username,
        password: "wrongPassword123!",
        certificationText: CERTIFICATION_TEXT,
      },
    });
    expect(res.status()).toBe(400);

    // Confirm user is STILL not certified after failed attempt
    const check = await page.request.get(`${API}/certified/${username}`);
    const body = await check.json();
    expect(body.certified, "failed certification must not change state").toBe(
      false,
    );
  });

  test("Successful certification is persisted and queryable", async ({
    page,
  }) => {
    const cert = await certify(page, username, password);

    // Verify the response has meaningful data, not just truthy fields
    expect(cert.certificationId).toBeGreaterThan(0);
    expect(new Date(cert.certifiedAt).getTime()).not.toBeNaN();

    // Cross-verify: GET /certified must agree
    const check = await page.request.get(`${API}/certified/${username}`);
    const body = await check.json();
    expect(body.certified).toBe(true);

    // Cross-verify: admin listing must include this exact certification
    const adminRes = await page.request.get(`${API}/admin/certifications`);
    const certs = await adminRes.json();
    const ours = certs.find(
      (c: { certificationId: number }) =>
        c.certificationId === cert.certificationId,
    );
    expect(ours, "certification must appear in admin listing").toBeTruthy();
  });

  test("Double certification is rejected — cannot certify twice", async ({
    page,
  }) => {
    const headers = await csrfHeaders(page);
    const res = await page.request.post(`${API}/certify`, {
      headers,
      data: { username, password, certificationText: CERTIFICATION_TEXT },
    });
    // Must reject because user is already certified
    expect(res.status()).toBe(400);
  });

  // ════════════════════════════════════════════
  // Signature execution + audit trail integrity
  // ════════════════════════════════════════════

  test("AUTHORED signature is persisted with correct signer, meaning, and record binding", async ({
    page,
  }) => {
    const recId = runId + 1;
    const sig = await sign(page, {
      username,
      password,
      meaning: "AUTHORED",
      recordType: REC_TYPE,
      recordId: recId,
    });

    // Verify response has real values, not just field presence
    expect(sig.signatureId).toBeGreaterThan(0);
    expect(sig.signatureMeaning).toBe("AUTHORED");
    expect(sig.recordType).toBe(REC_TYPE);
    expect(sig.recordId).toBe(recId);
    expect(sig.authMethod).toBe("LOCAL");
    expect(sig.signerNamePrinted).toBeTruthy();
    expect(sig.sessionSigningSequence).toBeGreaterThanOrEqual(1);

    // signedAt must be a recent server timestamp (within last 30s)
    const signedAt = new Date(sig.signedAt).getTime();
    const now = Date.now();
    expect(signedAt).toBeGreaterThan(now - 30_000);
    expect(signedAt).toBeLessThanOrEqual(now + 5_000); // small clock skew tolerance

    // Cross-verify: query the record and find this exact signature
    const sigs = await querySignatures(page, REC_TYPE, recId);
    expect(sigs.length).toBe(1);
    expect(sigs[0].signatureId).toBe(sig.signatureId);
    expect(sigs[0].signatureMeaning).toBe("AUTHORED");
    expect(sigs[0].signerNamePrinted).toBe(sig.signerNamePrinted);
  });

  test("Wrong password does not produce a signature — verify via query, not just status code", async ({
    page,
  }) => {
    const headers = await csrfHeaders(page);
    const recId = runId + 2;
    const res = await page.request.post(`${API}/sign`, {
      headers,
      data: {
        username,
        password: "wrongPassword123!",
        signatureMeaning: "AUTHORED",
        recordType: REC_TYPE,
        recordId: recId,
      },
    });
    expect(res.status()).toBe(400);

    // The real test: was anything actually written?
    const sigs = await querySignatures(page, REC_TYPE, recId);
    expect(sigs, "failed sign must not persist any record").toEqual([]);
  });

  // ════════════════════════════════════════════
  // §11.200 — Session signing sequence
  // ════════════════════════════════════════════

  test("Session sequence increments monotonically across multiple signatures", async ({
    page,
  }) => {
    const recId = runId + 3;

    const sig1 = await sign(page, {
      username,
      password,
      meaning: "AUTHORED",
      recordType: REC_TYPE,
      recordId: recId,
    });
    const seq1 = sig1.sessionSigningSequence;

    const sig2 = await sign(page, {
      username,
      password,
      meaning: "VALIDATED_AND_RELEASED",
      recordType: REC_TYPE,
      recordId: recId,
    });
    const seq2 = sig2.sessionSigningSequence;

    const sig3 = await sign(page, {
      username,
      password,
      meaning: "AUTHORED",
      recordType: REC_TYPE,
      recordId: recId + 100, // different record, same session
    });
    const seq3 = sig3.sessionSigningSequence;

    // Sequences must be strictly increasing
    expect(seq2, "second signature sequence must exceed first").toBeGreaterThan(
      seq1,
    );
    expect(seq3, "third signature sequence must exceed second").toBeGreaterThan(
      seq2,
    );

    // Cross-verify: query returns both signatures for this record in order
    const sigs = await querySignatures(page, REC_TYPE, recId);
    expect(sigs.length).toBe(2);
    // Ordered by signed_at ASC — first should be AUTHORED, second VALIDATED
    expect(sigs[0].signatureMeaning).toBe("AUTHORED");
    expect(sigs[1].signatureMeaning).toBe("VALIDATED_AND_RELEASED");
  });

  test("Session status reflects actual signing activity", async ({ page }) => {
    const res = await page.request.get(`${API}/session-status/${username}`);
    const body = await res.json();

    expect(body.sessionActive).toBe(true);
    // We've signed multiple times by now; count must reflect that
    expect(body.signingCount).toBeGreaterThanOrEqual(3);
  });

  // ════════════════════════════════════════════
  // Rejection — reason is mandatory and persisted
  // ════════════════════════════════════════════

  test("REJECTED without reason is blocked and produces no record", async ({
    page,
  }) => {
    const headers = await csrfHeaders(page);
    const recId = runId + 4;
    const res = await page.request.post(`${API}/sign`, {
      headers,
      data: {
        username,
        password,
        signatureMeaning: "REJECTED",
        recordType: REC_TYPE,
        recordId: recId,
      },
    });
    expect(res.status()).toBe(400);

    // Verify nothing was written
    const sigs = await querySignatures(page, REC_TYPE, recId);
    expect(sigs).toEqual([]);
  });

  test("REJECTED with reason persists the exact rejection text", async ({
    page,
  }) => {
    const recId = runId + 5;
    const reason = "QC deviation: CV% exceeds 15% threshold on replicate #3";

    const sig = await sign(page, {
      username,
      password,
      meaning: "REJECTED",
      recordType: REC_TYPE,
      recordId: recId,
      rejectionReason: reason,
    });

    expect(sig.signatureMeaning).toBe("REJECTED");
    expect(sig.rejectionReason).toBe(reason);

    // Cross-verify: query returns the rejection with exact reason
    const sigs = await querySignatures(page, REC_TYPE, recId);
    expect(sigs.length).toBe(1);
    expect(sigs[0].rejectionReason).toBe(reason);
    expect(sigs[0].signatureMeaning).toBe("REJECTED");
  });

  // ════════════════════════════════════════════
  // Audit trail — signatures are append-only
  // ════════════════════════════════════════════

  test("Multiple signatures on same record accumulate — none are lost or overwritten", async ({
    page,
  }) => {
    const recId = runId + 6;

    // Create three signatures on the same record
    await sign(page, {
      username,
      password,
      meaning: "AUTHORED",
      recordType: REC_TYPE,
      recordId: recId,
    });
    await sign(page, {
      username,
      password,
      meaning: "REJECTED",
      recordType: REC_TYPE,
      recordId: recId,
      rejectionReason: "Initial batch failed potency spec",
    });
    await sign(page, {
      username,
      password,
      meaning: "AUTHORED",
      recordType: REC_TYPE,
      recordId: recId,
    });

    // All three must be present — no overwriting
    const sigs = await querySignatures(page, REC_TYPE, recId);
    expect(sigs.length).toBe(3);

    // Verify chronological order and correct meanings
    expect(sigs[0].signatureMeaning).toBe("AUTHORED");
    expect(sigs[1].signatureMeaning).toBe("REJECTED");
    expect(sigs[1].rejectionReason).toBe("Initial batch failed potency spec");
    expect(sigs[2].signatureMeaning).toBe("AUTHORED");

    // Each must have a distinct signatureId
    const ids = sigs.map((s: { signatureId: number }) => s.signatureId);
    expect(new Set(ids).size, "each signature must have a unique ID").toBe(3);

    // Timestamps must be non-decreasing
    for (let i = 1; i < sigs.length; i++) {
      expect(new Date(sigs[i].signedAt).getTime()).toBeGreaterThanOrEqual(
        new Date(sigs[i - 1].signedAt).getTime(),
      );
    }
  });

  test("Signatures for different records are isolated — no cross-contamination", async ({
    page,
  }) => {
    const recA = runId + 7;
    const recB = runId + 8;

    await sign(page, {
      username,
      password,
      meaning: "AUTHORED",
      recordType: REC_TYPE,
      recordId: recA,
    });
    await sign(page, {
      username,
      password,
      meaning: "VALIDATED_AND_RELEASED",
      recordType: REC_TYPE,
      recordId: recB,
    });

    const sigsA = await querySignatures(page, REC_TYPE, recA);
    const sigsB = await querySignatures(page, REC_TYPE, recB);

    expect(sigsA.length).toBe(1);
    expect(sigsA[0].signatureMeaning).toBe("AUTHORED");
    expect(sigsA[0].recordId).toBe(recA);

    expect(sigsB.length).toBe(1);
    expect(sigsB[0].signatureMeaning).toBe("VALIDATED_AND_RELEASED");
    expect(sigsB[0].recordId).toBe(recB);
  });

  // ════════════════════════════════════════════
  // Certification revocation — re-gates signing
  // ════════════════════════════════════════════

  test("Revoked certification blocks signing and revocation is queryable", async ({
    page,
  }) => {
    const headers = await csrfHeaders(page);
    // Revoke
    const revokeRes = await page.request.delete(
      `${API}/admin/certifications/${username}`,
      { headers },
    );
    expect(revokeRes.status()).toBe(200);

    // Cross-verify: certified endpoint agrees
    const certCheck = await page.request.get(`${API}/certified/${username}`);
    expect((await certCheck.json()).certified).toBe(false);

    // Attempt to sign — must fail
    const recId = runId + 9;
    const signRes = await page.request.post(`${API}/sign`, {
      headers,
      data: {
        username,
        password,
        signatureMeaning: "AUTHORED",
        recordType: REC_TYPE,
        recordId: recId,
      },
    });
    expect(signRes.status()).toBe(400);

    // Verify no signature leaked through
    const sigs = await querySignatures(page, REC_TYPE, recId);
    expect(sigs).toEqual([]);

    // Re-certify to leave system in clean state
    await certify(page, username, password);
    const recheck = await page.request.get(`${API}/certified/${username}`);
    expect((await recheck.json()).certified).toBe(true);
  });
});
