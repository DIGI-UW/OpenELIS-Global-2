import { execFileSync } from "child_process";
import { expect, type Page } from "@playwright/test";
import { resolveDbContainer } from "./db-container";
import {
  getCsrfToken,
  seedReviewedMicrobiologyCase,
} from "./seed-microbiology-data";
import { seedOrderForPatient, seedPatient } from "./seed-patient-order";
import { completeAnalysisChain } from "./seed-tat-data";

const PREFIX = "MGMTPERF";

function psql(sql: string): string[] {
  return execFileSync(
    "docker",
    [
      "exec",
      "-i",
      resolveDbContainer(),
      "psql",
      "-v",
      "ON_ERROR_STOP=1",
      "-U",
      "clinlims",
      "-d",
      "clinlims",
      "-tAc",
      sql,
    ],
    { encoding: "utf8", maxBuffer: 16 * 1024 * 1024 },
  )
    .trim()
    .split("\n")
    .filter(Boolean);
}

const TEMPLATE_TESTS = `
  SELECT test_id || ',' || sample_type_id FROM (
    SELECT DISTINCT ON (t.test_section_id) t.id AS test_id, stt.sample_type_id,
           EXISTS (SELECT 1 FROM clinlims.test_surveillance_mapping m
                   WHERE m.test_id = t.id AND m.is_active) AS mapped
    FROM clinlims.test t
    JOIN clinlims.sampletype_test stt ON stt.test_id = t.id
    JOIN clinlims.type_of_sample tos ON tos.id = stt.sample_type_id AND tos.is_active = 'Y'
    JOIN clinlims.test_section ts ON ts.id = t.test_section_id AND ts.is_active = 'Y'
    WHERE t.is_active = 'Y' AND t.orderable
      AND COALESCE(ts.domain, '') <> 'VECTOR'
      AND EXISTS (SELECT 1 FROM clinlims.test_result tr
                  WHERE tr.test_id = t.id AND tr.tst_rslt_type IN ('N', 'D'))
    ORDER BY t.test_section_id,
             EXISTS (SELECT 1 FROM clinlims.test_surveillance_mapping m
                     WHERE m.test_id = t.id AND m.is_active) DESC,
             t.id
  ) per_section
  ORDER BY mapped DESC, test_id
  LIMIT 8`;

const asId = (value: string) => {
  if (!/^\d+$/.test(value)) throw new Error(`Expected an id, got ${value}`);
  return value;
};

async function seedTemplateSamples(page: Page): Promise<string[]> {
  const tests = psql(TEMPLATE_TESTS).map((row) => row.split(",").map(asId));
  if (tests.length === 0) throw new Error("No orderable test to seed from");
  const patient = await seedPatient(page);
  const sampleIds: string[] = [];
  for (const [testId, sampleTypeId] of tests) {
    const labNo = await seedOrderForPatient(page, patient, {
      sampleTypeId,
      testIds: [testId],
    });
    await completeAnalysisChain(page, labNo);
    sampleIds.push(
      asId(
        psql(
          `SELECT id FROM clinlims.sample WHERE accession_number = '${labNo.replace(/[^A-Za-z0-9-]/g, "")}'`,
        )[0],
      ),
    );
  }
  return sampleIds;
}

// Clones spread over the last `days` days with 0 to 71 hours of extra TAT;
// every third analysis is credited to an analyzer.
const cloneSql = (
  templates: string[],
  from: number,
  to: number,
  days: number,
) => `
DO $seed$
DECLARE
  templates numeric[] := ARRAY[${templates.join(",")}];
  analyzers numeric[] := ARRAY(SELECT id FROM clinlims.analyzer ORDER BY id);
  src clinlims.sample;
  item clinlims.sample_item;
  an clinlims.analysis;
  received timestamp;
  shift interval;
  delay interval;
  new_sample numeric;
  new_item numeric;
  new_analysis numeric;
BEGIN
  FOR n IN ${from}..${to - 1} LOOP
    SELECT * INTO src FROM clinlims.sample
      WHERE id = templates[1 + n % array_length(templates, 1)];
    received := date_trunc('minute', now()) - make_interval(days => n % ${days})
      - make_interval(mins => 60 + (n * 37) % 600);
    shift := received - src.received_date;
    delay := LEAST(make_interval(hours => (n * 53) % 72),
      now() - received - (src.released_date - src.received_date));
    new_sample := nextval('clinlims.sample_seq');
    INSERT INTO clinlims.sample SELECT * FROM jsonb_populate_record(NULL::clinlims.sample,
      to_jsonb(src) || jsonb_build_object('id', new_sample,
        'accession_number', '${PREFIX}' || lpad(n::text, 7, '0'), 'barcode', NULL,
        'fhir_uuid', gen_random_uuid(), 'received_date', received,
        'entered_date', src.entered_date + shift, 'collection_date', src.collection_date + shift,
        'released_date', src.released_date + shift + delay, 'lastupdated', received));
    INSERT INTO clinlims.sample_human SELECT * FROM jsonb_populate_record(NULL::clinlims.sample_human,
      (SELECT to_jsonb(h) FROM clinlims.sample_human h WHERE h.samp_id = src.id)
      || jsonb_build_object('id', nextval('clinlims.sample_human_seq'), 'samp_id', new_sample))
      WHERE EXISTS (SELECT 1 FROM clinlims.sample_human h WHERE h.samp_id = src.id);
    INSERT INTO clinlims.sample_organization SELECT r.* FROM clinlims.sample_organization o,
      jsonb_populate_record(NULL::clinlims.sample_organization, to_jsonb(o)
        || jsonb_build_object('id', nextval('clinlims.sample_org_seq'), 'samp_id', new_sample)) r
      WHERE o.samp_id = src.id;
    INSERT INTO clinlims.sample_requester SELECT r.* FROM clinlims.sample_requester q,
      jsonb_populate_record(NULL::clinlims.sample_requester, to_jsonb(q)
        || jsonb_build_object('id', nextval('clinlims.sample_requester_seq'), 'sample_id', new_sample)) r
      WHERE q.sample_id = src.id;
    FOR item IN SELECT * FROM clinlims.sample_item WHERE samp_id = src.id LOOP
      new_item := nextval('clinlims.sample_item_seq');
      INSERT INTO clinlims.sample_item SELECT * FROM jsonb_populate_record(NULL::clinlims.sample_item,
        to_jsonb(item) || jsonb_build_object('id', new_item, 'samp_id', new_sample,
          'fhir_uuid', gen_random_uuid(), 'collection_date', item.collection_date + shift,
          'received_date', item.received_date + shift, 'lastupdated', received));
      FOR an IN SELECT * FROM clinlims.analysis WHERE sampitem_id = item.id LOOP
        new_analysis := nextval('clinlims.analysis_seq');
        INSERT INTO clinlims.analysis SELECT * FROM jsonb_populate_record(NULL::clinlims.analysis,
          to_jsonb(an) || jsonb_build_object('id', new_analysis, 'sampitem_id', new_item,
            'fhir_uuid', gen_random_uuid(), 'result_file_id', NULL,
            'analyzer_id', CASE WHEN n % 3 = 0 AND cardinality(analyzers) > 0
              THEN analyzers[1 + n % cardinality(analyzers)] END,
            'entry_date', an.entry_date + shift, 'started_date', an.started_date + shift,
            'completed_date', an.completed_date + shift + delay,
            'released_date', an.released_date + shift + delay,
            'lastupdated', received + delay));
        INSERT INTO clinlims.result SELECT r.* FROM clinlims.result res,
          jsonb_populate_record(NULL::clinlims.result, to_jsonb(res)
            || jsonb_build_object('id', nextval('clinlims.result_seq'), 'analysis_id', new_analysis,
              'fhir_uuid', gen_random_uuid(), 'lastupdated', received + delay)) r
          WHERE res.analysis_id = an.id AND res.parent_id IS NULL;
      END LOOP;
    END LOOP;
  END LOOP;
END
$seed$;
ANALYZE clinlims.sample; ANALYZE clinlims.sample_item; ANALYZE clinlims.analysis; ANALYZE clinlims.result;`;

export interface ManagementDashboardVolume {
  samplesLast30Days: number;
  analysesLast30Days: number;
  sectionsLast30Days: number;
  seededSamples: number;
}

// Disposable stacks only: the SQL clones of product-made orders are never removed.
export async function seedManagementDashboardVolume(
  page: Page,
  samples = 5000,
  days = 30,
): Promise<ManagementDashboardVolume> {
  const existing = Number(
    psql(
      `SELECT count(*) FROM clinlims.sample WHERE accession_number LIKE '${PREFIX}%'`,
    )[0],
  );
  if (existing < samples) {
    psql(cloneSql(await seedTemplateSamples(page), existing, samples, days));
  }
  const [inScope, analyses, sections] = psql(`
    SELECT count(DISTINCT s.id) || ',' || count(a.id) || ',' || count(DISTINCT a.test_sect_id)
    FROM clinlims.sample s
    JOIN clinlims.sample_item si ON si.samp_id = s.id
    JOIN clinlims.analysis a ON a.sampitem_id = si.id
    WHERE s.received_date >= current_date - ${days}`)[0]
    .split(",")
    .map(Number);
  return {
    samplesLast30Days: inScope,
    analysesLast30Days: analyses,
    sectionsLast30Days: sections,
    seededSamples: Math.max(existing, samples),
  };
}

// The antibiogram leaves out cases with no culture purpose.
export async function seedManagementDashboardMicrobiology(
  page: Page,
  isolates = 500,
): Promise<number> {
  const api = "/api/OpenELIS-Global/rest/microbiology/cases";
  const count = () =>
    Number(psql("SELECT count(*) FROM clinlims.micro_isolate")[0]);
  for (let missing = isolates - count(); missing > 0; missing -= 1) {
    const seeded = await seedReviewedMicrobiologyCase(page);
    const headers = { "X-CSRF-Token": await getCsrfToken(page) };
    const detail = await page.request.put(
      `${api}/${seeded.caseId}/order-detail`,
      {
        headers,
        data: {
          culturePurpose: "CLINICAL_DIAGNOSTIC",
          cultureMethodId: seeded.methodId,
          patientOrigin: "",
          admissionDate: null,
          numberOfSets: 1,
          clinicalHistory: "",
          antibioticExposure: false,
        },
      },
    );
    expect(detail.status(), await detail.text()).toBe(200);
    const release = await page.request.post(
      `${api}/${seeded.caseId}/release/final`,
      { headers, data: {} },
    );
    expect(release.status(), await release.text()).toBe(200);
  }
  // First-isolate selection still filters on this column until #4710 merges.
  psql(
    "UPDATE clinlims.micro_case SET workflow_type = 'BACTERIOLOGY' WHERE workflow_type IS NULL",
  );
  psql(
    "UPDATE clinlims.inventory_item SET low_stock_threshold = 30 WHERE low_stock_threshold <= 1",
  );
  return count();
}
