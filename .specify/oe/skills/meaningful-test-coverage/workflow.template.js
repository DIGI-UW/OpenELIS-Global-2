// Meaningful-test-coverage workflow template (MOCKUP — genericized from the
// vector-test-coverage run). Pass args = { context, layers: [{key, focus}, ...] }
// where `context` describes the change + the bug being fixed (incl. the OLD buggy
// behavior so the inversion guard can be checked), and each layer names a level
// to cover. Invoke: Workflow({ scriptPath: ".../workflow.template.js", args })
export const meta = {
  name: 'meaningful-test-coverage',
  description: 'Design + adversarially verify no-theater tests at each layer of a change, then synthesize a prioritized plan',
  phases: [
    { title: 'Design', detail: 'one agent per layer: correct test level + concrete test code + "would it fail on old code?"' },
    { title: 'Verify', detail: 'adversarial skeptic per design: theater / over-mock / wrong-level / inversion-guard validity' },
    { title: 'Synthesize', detail: 'merge into a prioritized plan naming the single layer that guards the core bug' },
  ],
}

const CONTEXT = (args && args.context) || 'NO CONTEXT PASSED — describe the change and the OLD buggy behavior.';
const LAYERS = (args && args.layers) || [
  { key: 'integration-db', focus: 'the layer where a real-DB bug (HQL/SQL/mapping) can fail — BaseWebContextSensitiveTest' },
  { key: 'service-unit', focus: 'pure logic (math/branching/mapping) — Mockito; must disclaim what the mock hides' },
  { key: 'frontend-component', focus: 'rendering + degraded/empty states — vitest; assert real DOM for happy + degraded' },
  { key: 'e2e', focus: 'assembled story — Playwright; visible-state assertions only' },
];

const RULES = `
TEST-QUALITY RULES (Constitution V.6) — obey strictly:
- Right level for what can fail. DB/HQL bugs need an integration test; a mocked test asserting the same thing is THEATER.
- The inversion test: a guard must FAIL on the OLD buggy behavior. Answer "would this fail if the fix were reverted?" and be precise.
- No assert-on-mock-return. No over-mocking the unit under test. No verify-only tests that still pass if behavior is dropped.
- Honest disclaimers: a level that cannot cover something must say so in javadoc, not pretend.
OE infra: integration = BaseWebContextSensitiveTest (Testcontainers Postgres + Liquibase), run 'mvn -Dtest=X -Dsurefire.failIfNoSpecifiedTests=false test' (JDK21+Docker); service unit = Mockito; frontend = vitest + testing-library (NOT jest); E2E = Playwright npm run pw:test scripts.
`;

const DESIGN_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['layer','testLevel','rationale','testFilePath','keyAssertions','theaterRisksAvoided','fixtures','testCode','catchesInversion'],
  properties: {
    layer: { type: 'string' }, testLevel: { type: 'string', enum: ['unit','integration','component','e2e','none'] },
    rationale: { type: 'string' }, testFilePath: { type: 'string' },
    keyAssertions: { type: 'array', items: { type: 'string' } },
    theaterRisksAvoided: { type: 'array', items: { type: 'string' } },
    fixtures: { type: 'string' }, testCode: { type: 'string' },
    catchesInversion: { type: 'string', description: 'would this FAIL on the old code? or "N/A — wrong level for the inversion"' },
  },
};
const VERDICT_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['meaningful','correctLevel','overMocked','theaterDetected','inversionGuardValid','issues','requiredChanges','verdict'],
  properties: {
    meaningful: { type: 'boolean' }, correctLevel: { type: 'boolean' }, overMocked: { type: 'boolean' },
    theaterDetected: { type: 'boolean' }, inversionGuardValid: { type: 'string' },
    issues: { type: 'array', items: { type: 'string' } }, requiredChanges: { type: 'array', items: { type: 'string' } },
    verdict: { type: 'string', enum: ['solid','needs-revision','theater-reject'] },
  },
};
const SYNTH_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['summary','coverageByLayer','gaps','implementationOrder'],
  properties: {
    summary: { type: 'string' },
    coverageByLayer: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['layer','level','file','status'], properties: { layer:{type:'string'}, level:{type:'string'}, file:{type:'string'}, status:{type:'string'} } } },
    gaps: { type: 'array', items: { type: 'string' } },
    implementationOrder: { type: 'array', items: { type: 'string' } },
  },
};

phase('Design')
const perLayer = await pipeline(
  LAYERS,
  (layer) => agent(`${CONTEXT}\n${RULES}\n\n=== YOUR LAYER: ${layer.key} ===\n${layer.focus}\nRead the actual changed files + existing test conventions before designing. Produce the ONE meaningful test (or set) at the correct level, with concrete code. Answer catchesInversion honestly.`,
    { label: `design:${layer.key}`, phase: 'Design', schema: DESIGN_SCHEMA }),
  (design, layer) => agent(`${CONTEXT}\n${RULES}\n\nAdversarially REVIEW this test for layer "${layer.key}". Hunt for theater. Would it FAIL on the OLD code? Over-mocked? Asserts on mock returns? Default to skepticism.\n\nDESIGN:\n${JSON.stringify(design)}`,
    { label: `verify:${layer.key}`, phase: 'Verify', schema: VERDICT_SCHEMA }).then((verdict) => ({ layer: layer.key, design, verdict })),
);

phase('Synthesize')
const synthesis = await agent(`${CONTEXT}\n${RULES}\n\nMerge these per-layer designs + verdicts into a prioritized no-theater test plan. Name the single layer that genuinely guards the core bug. Flag every theater-reject/needs-revision with its fix. Give a concrete implementation order.\n\n${JSON.stringify(perLayer)}`,
  { label: 'synthesize', phase: 'Synthesize', schema: SYNTH_SCHEMA });

return { perLayer, synthesis };
