# Feature Specification: Catalyst - LLM-Powered Lab Data Assistant

**Feature Branch**: `spec/OGC-070-catalyst-assistant`  
**Created**: 2026-01-20  
**Status**: Draft  
**Jira Issue**: [OGC-70](https://uwdigi.atlassian.net/browse/OGC-70)

## Clarifications

### Session 2026-01-29

- Q: The MedGemma methodology alignment plan introduces a "LocalPHI mode" where
  local LLMs can access actual patient/row data. Should this be in MVP scope or
  Phase 2? → A: LocalPHI mode is Phase 2 scope. MVP focuses on CloudSafe
  (schema-only) workflow. For MVP, security guardrails for non-orchestrator LLMs
  are placeholder/lax (not working with real patient data). Orchestrator
  security is rudimentary framework only. **CRITICAL WARNING**: Full security
  implementation MUST be completed before ANY actual patient data is used in
  LocalPHI mode. This is explicitly deferred, not forgotten.
- Q: Should MVP security requirements (FR-018 PHI detection, FR-021 RBAC, FR-016
  confirmation tokens) be fully implemented or placeholder? → A: Security
  **architectural components/hooks** MUST be in place for MVP (interfaces,
  extension points, configuration flags). However, **implementation is
  rudimentary/placeholder** until Phase 2 (patient data stage). Example: PHI
  detection interface exists but patterns are basic; RBAC check method exists
  but may be permissive; confirmation token flow exists but validation is
  minimal. Full hardened implementation required before patient data use.
- Q: Should the spec mandate flat JSON (not FHIR) for AI data tools per the
  MedGemma plan's "FHIR verbosity tax" finding? → A: FHIR constraint not
  directly applicable — Catalyst uses SQL against base OpenELIS data model, not
  FHIR endpoints. However, any data returned to LLMs (current or future) MUST be
  **compact and LLM-friendly** (minimize token usage). This is a general
  principle, not FHIR-specific.
- Q: Should the spec adopt "CloudSafe" and "LocalPHI" mode terminology from the
  methodology plan? → A: Yes. Adopt terminology now for consistent naming:
  **CloudSafe mode** (MVP) = schema-only context, cloud providers allowed;
  **LocalPHI mode** (Phase 2) = patient/row data via MCP tools, local providers
  only. See Terminology section below.

### Session 2026-01-27

- Q: FR-009 specifies row estimation to warn users about queries
  returning >10,000 rows, but states "M0.0-M0.2 uses placeholder (returns 0)".
  This means warnings will never trigger in early milestones, creating
  misleading UX. How should we handle row estimation in the MVP? → A: Defer
  FR-009 entirely to M2 - Remove from M0 scope, implement only when DB access
  available (cleaner UX).
- Q: NFR-001 specifies "Llama 3.1 8B / Gemma 2 9B for Orchestrator" but doesn't
  indicate which should be the primary default configuration. Which model should
  be documented as the default? → A: **Deferred** - Both Llama 3.1 8B and Gemma
  2 9B are equal candidates for Orchestrator. External analysis (Gemini/MedGemma
  research) suggests Gemma 2 9B may have superior RAG performance, but final
  selection MUST be based on empirical evaluation harness results during M0.2.
  Document both; decide after validation with golden query dataset.
- Q: FR-018 requires detecting "likely PHI/identifiers" in user queries to
  prevent sending sensitive data to external LLM providers. What approach should
  MVP use for PHI detection? → A: Simple regex/keyword matching for MRN, DOB
  patterns, patient identifiers, with configurable disable option (deployments
  using only local models may turn off PHI detection).
- Q: NFR-001 requires evaluating both Tier A (12GB VRAM) and Tier B (40GB+ VRAM)
  configurations. Can MVP pass M0.2 sign-off without Tier B evaluation if
  hardware unavailable? → A: Defer Tier B to post-MVP - Remove from M0.2,
  validate after MVP deployment when hardware accessible.
- Q: FR-022 golden query dataset needs proper infrastructure for validation.
  Should this be simple hardcoded queries or structured dataset compatible with
  LLM evaluation frameworks? → A: Build comprehensive golden query dataset (26+
  robust, useful queries based on lit review + web research) in structured
  format compatible with LLM validation frameworks/toolkits (e.g., ragas,
  promptfoo, langfuse). Dataset must include ALL metadata fields these toolkits
  expect: query text, expected tables, system prompts, model parameters,
  expected SQL patterns, validation criteria, expected results. Store in
  `projects/catalyst/tests/fixtures/golden_queries.json`. This allows plugging
  into professional evaluation tooling for proper query management, prompt
  versioning, and model comparison over time. UI example queries (FR-014) are
  separate concern, deferred to post-MVP.

### Session 2026-01-21

- Q: How do we prevent Catalyst from becoming a high-privilege reporting
  backdoor that bypasses existing OpenELIS permission and data-partitioning
  rules? → A: MVP restricts endpoint access to users with privileged roles
  (`Global Administrator` or `Reports`). These users already have cross-cutting
  data access in existing OpenELIS workflows. Per-user row-level filtering
  deferred to Phase 2.

### Session 2026-01-20

- Q: How does Catalyst enforce RBAC (FR-013) - via per-user DB roles,
  post-filtering, or blocked tables only? → A: Blocked tables only for MVP; full
  row-level RBAC deferred to Phase 2. Endpoint-level access restricted to
  privileged roles (FR-021).
- Q: Must the MVP support specific providers like Google Gemini and LM Studio? →
  A: Yes, MVP MUST support Gemini (Cloud) and LM Studio (Local via
  OpenAI-compatible API). These 2 providers are sufficient for MVP validation.
- Q: How to handle large schema context for MVP? → A: RAG/MCP-based approach
  required to support full clinical schema (filtering relevant tables via
  embeddings).
- Q: What architecture for Schema RAG in MVP? → A: Pilot Standalone MCP Server
  (Option C) to validate standards-based architecture early, despite higher
  initial overhead.
- Q: Language for MCP Server? → A: Python (Official SDK) to leverage rich AI/RAG
  ecosystem and rapid tooling.
- Q: Should MVP architecture be designed for agent extraction? → A: MVP should
  prototype a **simple multi-agent team** based on med-agent-hub patterns and
  A2A spec: RouterAgent + specialist agents (SchemaAgent, SQLGenAgent).
  Single-agent fallback for simpler deployments/dev/testing.

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Natural Language Query to SQL Results (Priority: P1)

A laboratory manager wants to quickly answer data questions without writing SQL.
They open the Catalyst sidebar in OpenELIS Global, type a question in plain
language, review a proposed query generated by Catalyst, and then run it to see
results displayed in a table format.

**Why this priority**: This is the core value proposition of Catalyst. Without
this capability, the feature provides no value. It validates the fundamental
concept that LLMs can generate accurate SQL from natural language queries
against the OpenELIS schema.

**Independent Test**: Can be fully tested by sending a natural language query
through the chat interface and verifying that:

1. The system generates a proposed query (including a SQL preview) and returns a
   confirmation token
2. The user reviews the SQL and confirms execution (using the confirmation
   token)
3. The query executes successfully against the OpenELIS database only after
   confirmation validation
4. Results are returned and displayed to the user

This delivers immediate value: users can answer data questions without SQL
knowledge.

**Acceptance Scenarios**:

1. **Given** a user is logged into OpenELIS Global with appropriate permissions,
   **When** they submit "How many samples were entered today?" in the Catalyst
   sidebar, **Then** the system generates SQL, validates it (status: VALIDATED),
   and displays a proposed query (including a SQL preview) with a confirmation
   token, **And When** the user confirms execution (using the confirmation
   token), **Then** the system validates the token (status: ACCEPTED), executes
   the query, and displays a table showing the count of samples entered on the
   current date (status: EXECUTED).

2. **Given** a user submits a query about test results, **When** they submit
   "Show all HIV test results from last week", **Then** the system proposes a
   query that joins the appropriate tables and can be run by the user to return
   formatted results.

3. **Given** a user submits an ambiguous query, **When** they submit "samples",
   **Then** the system asks a clarification question before proposing a query.

4. **Given** a user submits a query that requires aggregation, **When** they
   submit "What is the average turnaround time for malaria tests?", **Then** the
   system proposes a query with appropriate aggregation and can be run by the
   user to return the calculated result.

---

### User Story 2 - Privacy-First Architecture (Priority: P1)

A privacy officer or system administrator needs assurance that Catalyst never
exposes patient data to external/cloud AI services (CloudSafe mode). For local
deployments, future LocalPHI mode may allow controlled patient data access with
strict guardrails (Phase 2). The system must guarantee that only database schema
metadata is used as AI context for cloud providers, never actual patient records
or test results.

**Why this priority**: This is a non-negotiable security and compliance
requirement. Violation of this principle could result in HIPAA violations,
SLIPTA non-compliance, and loss of user trust. This must be enforced from day
one.

**Independent Test**: Can be fully tested by reviewing the system's audit
records for AI-assisted query generation in CloudSafe mode and confirming they
contain only schema metadata and the user's question (no patient identifiers,
test results, or other PHI), and by confirming execution uses read-only database
access.

This delivers security assurance and regulatory compliance.

**Acceptance Scenarios**:

1. **Given** the system generates a proposed query in CloudSafe mode, **When**
   it prepares AI context, **Then** the context contains only database schema
   information (table names, column names, data types, relationships) and the
   user's natural language question, with no patient data values.

2. **Given** a user queries for information that could involve patient
   identifiers or results in CloudSafe mode, **When** the system processes the
   request, **Then** patient identifiers and test results are never included in
   AI context sent to cloud/external providers.

3. **Given** the system executes generated SQL, **When** it connects to the
   database, **Then** it uses a read-only database connection that cannot modify
   data.

4. **Given** a user includes identifiers or other PHI in their question (e.g.,
   MRN, patient name, accession number), **When** the configured AI provider is
   externally-hosted, **Then** the system MUST NOT send the request to that
   external provider, **And** the user is either routed to a local provider (if
   configured and available) or prompted to remove PHI from the question and
   retry.

**Note**: These scenarios apply to CloudSafe mode (MVP). LocalPHI mode (Phase 2)
will have separate acceptance criteria for controlled local data access.

---

### User Story 3 - Cloud and Local LLM Support (Priority: P2)

A developer wants rapid iteration in development environments and
privacy-preserving deployments in production environments. The system must
support switching between externally-hosted AI providers and on-premises AI
providers without code changes.

**Why this priority**: This enables flexible deployment strategies. Cloud APIs
allow fast iteration during development, while local models ensure privacy
compliance in production. This is essential for the MVP to be deployable in
various environments.

**Independent Test**: Can be fully tested by:

1. Configuring the system to use a cloud LLM provider and verifying queries work
2. Reconfiguring to use a local LLM provider and verifying queries work
3. Confirming that the same queries produce similar results regardless of
   provider

This delivers deployment flexibility and vendor independence.

**Acceptance Scenarios**:

1. **Given** the system is configured to use an externally-hosted AI provider,
   **When** a user submits a query, **Then** the system uses that provider to
   generate a proposed query.

2. **Given** the system is configured to use an on-premises AI provider,
   **When** a user submits a query, **Then** the system generates a proposed
   query without sending data outside the deployment environment.

3. **Given** the system switches between providers, **When** the same query is
   submitted, **Then** both providers generate functionally equivalent SQL (may
   differ in syntax but produce same results).

---

### User Story 4 - Results Export (Priority: P3)

A user wants to export query results for further analysis or reporting. After
receiving results in the chat interface, they can download the data in a
standard format.

**Why this priority**: While not essential for MVP validation, export capability
significantly increases the feature's utility. Users often need to share results
or perform additional analysis outside the system.

**Independent Test**: Can be fully tested by:

1. Executing a query that returns results
2. Clicking an export button
3. Verifying the downloaded file contains the correct data in the specified
   format

This delivers enhanced usability and workflow integration.

**Acceptance Scenarios**:

1. **Given** a user has received query results in the chat interface, **When**
   they click the export button, **Then** they can download results as CSV or
   JSON.

2. **Given** results contain multiple rows, **When** the user exports to CSV,
   **Then** the file includes headers and all data rows in a format compatible
   with Excel or other tools.

---

### Edge Cases

- What happens when the LLM generates invalid SQL syntax?

  - System should detect SQL syntax errors and either retry with a corrected
    prompt or return a user-friendly error message asking the user to rephrase.

- What happens when a query would return more than 10,000 rows?

  - System should either limit results to the first 10,000 rows with a warning,
    or require the user to add more specific filters before execution.

- What happens when the LLM generates SQL that accesses blocked tables (e.g.,
  sys_user, login_user)?

  - System should reject the query and inform the user that access to that data
    is restricted.

- What happens when the database connection fails during query execution?

  - System should return a user-friendly error message indicating the database
    is temporarily unavailable, without exposing technical details.

- What happens when the LLM service (Catalyst Gateway or provider) is
  unavailable?

  - System should detect service unavailability (connection timeout, HTTP 503,
    network error) and return a user-friendly error message: "The AI assistant
    is temporarily unavailable. Please try again in a few moments." All failures
    are logged for monitoring (FR-010 audit trail). No automatic retry in MVP;
    user must resubmit query.

- What happens when a user submits a query in a language other than English?

  - **MVP Scope**: The MVP supports English natural language queries only.
    Queries in other languages may not be processed correctly.
  - **Long-term Target**: The system is designed to natively support any
    language that OpenELIS supports (en, fr, ar, es, hi, pt, sw), consistent
    with Constitution Principle VII (Internationalization First). This will be
    implemented in a future phase.

- What happens when the LLM service (cloud or local) is unavailable?

  - System should return a clear error message indicating the AI service is
    unavailable and suggest retrying later.

- What happens when a query requires data from tables that don't exist in the
  schema?

  - System should inform the user that the requested data is not available in
    the database schema.

- What happens when a user query is ambiguous (e.g., "samples" without
  specifying sample/patient/date context)?

  - **Ambiguity Detection Criteria**: A query is considered ambiguous when:
    - (a) Multiple tables match the query intent (e.g., "samples" could refer to
      `sample`, `sample_item`, or `sample_patient`)
    - (b) Required filtering context is missing (e.g., date range, patient
      identifier, test type)
    - (c) SchemaAgent RAG retrieval returns >5 relevant tables with similar
      relevance scores (within 10% of top score)
  - **Clarification Flow**: RouterAgent MUST detect ambiguity and prompt the
    user for clarification before delegating to SchemaAgent. The clarification
    prompt should ask specific questions (e.g., "Do you mean samples collected
    today, or all samples? Do you need patient information included?"). Once
    clarified, the query proceeds through the normal Router → SchemaAgent →
    SQLGenAgent flow.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST provide a chat interface (sidebar) integrated into
  OpenELIS Global where users can submit natural language queries.

- **FR-002**: System MUST convert natural language queries into valid SQL
  statements using an LLM (Large Language Model).

- **FR-003**: System MUST provide relevant database schema metadata to the LLM
  using a RAG (Retrieval-Augmented Generation) or MCP (Model Context Protocol)
  approach to support the full clinical schema without exceeding context limits.

- **FR-004**: In **CloudSafe mode** (MVP), system MUST ensure that LLM prompts
  contain ONLY schema metadata and the user's query text, with NO patient data,
  test results, or other PHI (Protected Health Information). This requirement
  applies unconditionally to all cloud/external providers. LocalPHI mode
  (Phase 2) defines separate rules for local-only providers.

- **FR-005**: System MUST execute generated SQL queries against the OpenELIS
  database using a read-only connection.

- **FR-006**: System MUST display query results in a table format within the
  chat interface.

- **FR-007**: System MUST support multiple AI model providers (externally-hosted
  and on-premises) configurable at deployment time without code changes.
  Required providers for MVP: Google Gemini (Cloud), and LM Studio (Local via
  OpenAI-compatible API supporting OpenAI-compatible models).

- **FR-008**: System MUST validate generated SQL before execution to prevent
  access to blocked tables (e.g., sys_user, login_user, user_role).

- **FR-009**: System MUST estimate the number of rows a query will return before
  execution and warn users if the estimate exceeds 10,000 rows.

  **Implementation (defense-in-depth)**:

  - M0.0-M0.2: Placeholder returns 0 (no estimation)
  - M1+: MCP `validate_sql` tool performs EXPLAIN-based estimation (agent
    pre-validation)
  - M2+: Java `SQLGuardrails` re-validates estimation (defense-in-depth)

  Both layers use PostgreSQL EXPLAIN to estimate row count.

- **FR-010**: System MUST log all generated SQL queries and their execution
  results for audit purposes, including user ID and timestamp.

- **FR-011**: System MUST handle errors gracefully, providing user-friendly
  error messages without exposing technical implementation details.

- **FR-012**: Users MUST be able to export query results as CSV or JSON format.

- **FR-013**: System MUST enforce access control via blocked table list (e.g.,
  sys_user, login_user, user_role). Full row-level RBAC integration with
  OpenELIS permissions is deferred to Phase 2.

- **FR-014**: System MUST provide example queries or prompts to help users
  understand how to phrase their questions effectively. Examples are served from
  the frontend (no backend endpoint required). Minimum 5 example queries:

  1. Count: "How many samples were entered today?"
  2. JOIN: "Show all HIV test results from last week"
  3. Aggregation: "What is the average turnaround time for malaria tests?"
  4. Date filter: "List samples collected in January 2026"
  5. Status: "How many tests are pending validation?"

- **FR-015**: System MUST support queries that require JOINs across multiple
  tables, aggregations (COUNT, SUM, AVG), and date filtering.

- **FR-016**: System MUST present SQL to user for review after VALIDATED state.
  Execution requires user confirmation (ACCEPTED state) via server-validated
  token matching the generated SQL. Workflow: SUBMITTED → VALIDATED → ACCEPTED →
  EXECUTED.

  **Note**: FR-016 architectural components (token generation interface,
  validation hooks) are in place for MVP. **Implementation is placeholder** in
  M0-M4; M5 implements framework interface.

- **FR-017**: (Reserved - placeholder for LocalPHI mode row-level access control
  in Phase 2)

- **FR-018**: System MUST detect likely PHI/identifiers in user-submitted
  questions using **regex/keyword matching** for common patterns (MRN formats,
  DOB patterns, "patient name", accession numbers). **Default PHI detection
  patterns** are included in the codebase (see plan.md Security section for
  examples like `\b\d{6,10}\b` for MRN-like numbers, `\b\d{2}/\d{2}/\d{4}\b` for
  DOB patterns). If the configured AI provider is externally-hosted, the system
  MUST NOT send the question to that provider. The system must either (a) route
  the request to an on-premises provider (if configured and healthy) or (b)
  block the request and prompt the user to remove PHI and retry. **PHI detection
  is configurable** - deployments using only local LLM providers may disable
  this safeguard via configuration.

  **Note**: FR-018 architectural components (interface, configuration flags,
  extension points) are implemented in M5. **MVP (M0-M2) includes placeholder
  implementation only** - basic regex patterns, permissive defaults. Full
  hardened implementation required before patient data use (Phase 2+).

- **FR-019**: Audit records for query generation and execution MUST include
  enough metadata to verify compliance without storing sensitive context. At
  minimum, audit MUST capture: selected provider type (externally-hosted vs
  on-premises), provider identifier, whether PHI/identifier gating triggered
  (FR-018), and which schema tables were provided as context (by name). Audit
  records MUST NOT store raw schema dumps/DDL sent to the model and MUST NOT
  store PHI values.

- **FR-020**: System MUST implement a simple multi-agent team using A2A
  (Agent2Agent) protocol patterns based on med-agent-hub concepts:

  - (a) **RouterAgent**: Orchestrates query flow, delegates to specialists
  - (b) **SchemaAgent**: RAG-based schema retrieval via MCP tools
  - (c) **SQLGenAgent**: Text-to-SQL generation using configured LLM
  - (d) Each agent MUST have an Agent Card for discovery per A2A specification
  - (e) System MUST support single-agent fallback mode for simpler deployments

  **Note**: M0.0 validates the core A2A + MCP architecture with Router + single
  CatalystAgent + MCP skeleton. M0.2 splits CatalystAgent into SchemaAgent +
  SQLGenAgent. Full 3-agent team validated by end of M0.2.

- **FR-021**: System MUST restrict access to the Catalyst query endpoint
  (`/rest/catalyst/query`) to users with privileged roles. For MVP, access is
  limited to users with `Global Administrator` or `Reports` roles. This ensures
  Catalyst does not become a backdoor bypassing existing OpenELIS permission and
  data-partitioning rules. Users without these roles MUST receive a 403
  Forbidden response.

  **Note**: This leverages OpenELIS's existing RBAC infrastructure
  (`UserRoleService.userInRole()`). Per-user row-level filtering within queries
  is deferred to Phase 2.

- **FR-022**: System MUST include an evaluation harness for validating LLM
  workflow quality. The harness MUST support:

  - (a) **Golden query dataset**: 26+ comprehensive, robust OpenELIS-focused
    queries stored in **structured JSON format**
    (`projects/catalyst/tests/fixtures/golden_queries.json`) based on
    **literature review + web research** for text-to-SQL validation best
    practices. Dataset MUST be **compatible with LLM validation
    frameworks/toolkits** (e.g., ragas, promptfoo, langfuse) and include ALL
    required metadata fields: query text, expected tables/columns, system
    prompts, model parameters, expected SQL patterns, validation criteria,
    expected results/row counts. This structured format enables integration with
    professional evaluation tooling for query management, prompt versioning, and
    model comparison over time. Covers: counts, joins, aggregations, date
    filters, ambiguity handling, and PHI-like inputs (see research.md Section 13
    for initial set).
  - (b) **Deterministic validation**: SELECT-only guard, blocked table
    detection, single-statement check, schema grounding verification
  - (c) **Retrieval metrics**: Recall@K and HitRate@K for schema retrieval
    (target: Recall@5 >= 80%, HitRate@5 >= 90%)
  - (d) **Model comparison scorecard**: Standardized evaluation across candidate
    LLMs using the balanced scorecard (research.md Section 15)

  **Note**: The golden query dataset itself is comprehensive from the start.
  Execution accuracy validation (running SQL against seeded DB, comparing
  results) is deferred until M2+ when read-only database access is available.

### Non-Functional Requirements

- **NFR-001**: Local LLM deployment MUST support **Tier A** (12GB VRAM)
  configuration for MVP:

  - **Tier A** (12GB VRAM) - **MVP Scope**: **Orchestrator: Primary Gemma 2 9B,
    Fallback Llama 3.1 8B** (validated per 2026-01-29 clarification); CodeLlama
    13B for SQLGen (Q4_K_M quantization)
  - **Tier B** (40GB+ VRAM) - **Post-MVP**: Same Orchestrator candidates,
    CodeLlama 34B / Llama 3.1 70B for SQLGen (deferred to post-MVP when hardware
    available)

  Tier A model selection MUST be validated against the evaluation harness
  (FR-022) using the balanced scorecard (research.md Section 15). **Note**:
  External research (Gemini/MedGemma analysis) suggests Gemma 2 9B may excel at
  RAG tasks; this hypothesis is validated empirically, not assumed.

### Constitution Compliance Requirements (OpenELIS Global)

_Derived from `.specify/memory/constitution.md` - include only relevant
principles for this feature:_

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks (Bootstrap, Tailwind, etc.)

- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded English text). Minimum translations required: English (en) and
  French (fr).

- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form)

  - **Valueholders MUST use JPA/Hibernate annotations** (NO XML mapping files)
  - Services own transaction boundaries (@Transactional in services ONLY, never
    controllers)
  - Controllers delegate to services, never call DAOs directly

- **CR-004**: Database changes MUST use Liquibase changesets (NO direct
  DDL/DML). If new tables are needed for report storage or audit logging, they
  must be created via Liquibase.

- **CR-005**: External data integration MUST use FHIR R4 + IHE profiles (if
  applicable). For MVP, this may not apply, but future integration with external
  systems must follow FHIR standards.

- **CR-006**: Configuration-driven variation for country-specific requirements
  (NO code branching). LLM provider selection and guardrail settings (including
  PHI detection enable/disable) must be configurable, not hardcoded.

- **CR-007**: Security: MVP authorization is enforced at two levels:

  - **Endpoint-level**: Access to `/rest/catalyst/query` restricted to
    privileged roles (`Global Administrator`, `Reports`) via `UserRoleService`
    (FR-021)
  - **Table-level**: Blocked-table list prevents queries to sensitive tables
    (FR-013)
  - Per-user row-level filtering is deferred to Phase 2
  - Audit trail (sys_user_id + lastupdated for all queries), input validation
    (sanitize user queries to prevent injection attacks).

- **CR-008**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal). MVP must include at least one E2E test proving the full chat → SQL →
  results flow.

- **CR-009**: Java platform: Java 21 LTS, Jakarta EE 9 (jakarta._, NOT javax._),
  Spring Framework 6.2.2 (Traditional MVC, NOT Spring Boot).

### Key Entities _(include if feature involves data)_

- **CatalystQuery**: Represents a user's natural language query submission.
  Contains: query text, user ID, timestamp, generated SQL (after processing),
  execution status, result row count.

- **CatalystAgentCards**: A2A-compliant agent descriptors for discovery. MVP
  includes three agents, each with its own Agent Card:

  - **RouterAgent**: Orchestrates query flow, delegates to specialists
  - **SchemaAgent**: RAG-based schema retrieval via MCP tools
  - **SQLGenAgent**: Text-to-SQL generation using configured LLM Each card
    contains: agent name, description, supported skills, input/output schemas,
    authentication requirements, endpoint URL. Router card published at
    `/.well-known/agent.json` per A2A specification.

- **CatalystReport**: (Future Phase 3) Represents a saved query result set.
  Contains: report name, query text, SQL used, result data snapshot, created
  date, created by user ID, sharing permissions.

- **SchemaMetadata**: Represents database schema information provided to LLM.
  Contains: table name, column name, data type, nullable flag, foreign key
  relationships, business descriptions (human-readable table/column
  descriptions).

## Success Criteria _(mandatory)_

### Functional MVP Success Criteria

- **SC-001**: Users can submit a natural language query in English, review the
  generated SQL, and (after confirmation) execute it successfully via the UI to
  receive results.

- **SC-002**: System supports switching between cloud (Gemini) and local (LM
  Studio) LLM providers without code changes, with the same workflow functioning
  for both provider types.

- **SC-003**: Audit records exist for all query generation and execution
  operations, containing required metadata (provider type, provider identifier,
  PHI gating status, tables used) without storing PHI values or raw schema
  dumps.

- **SC-004**: System blocks all attempts to query restricted tables (sys_user,
  login_user, user_role) with clear error messages.

- **SC-005**: Zero instances of patient data or PHI appearing in
  **cloud/external** LLM API requests (verified via audit logs). This criterion
  applies to CloudSafe mode; LocalPHI mode (Phase 2) has separate validation.

- **SC-006**: 100% of questions flagged as containing PHI/identifiers are
  prevented from being sent to externally-hosted AI providers (verified via
  audit logs).

- **SC-007**: System provides error messages that:

  - Include suggested query reformulation when SQL generation fails
  - Do NOT expose technical implementation details (stack traces, raw SQL
    errors)
  - Are internationalized (en/fr minimum per Constitution VII)

- **SC-008**: Users can export query results in CSV or JSON format.

- **SC-009**: MVP delivers a working prototype that can answer multiple types of
  laboratory data questions (sample counts, test result queries, turnaround time
  analysis, date range filtering, aggregation queries) as demonstrated in
  end-to-end tests.

- **SC-010**: Evaluation harness demonstrates Recall@5 >= 80% for schema
  retrieval across the 26-question golden query set.

- **SC-011**: At least one Tier A model configuration (Orchestrator + SQLGen)
  passes all deterministic guards (SELECT-only, blocked tables, single
  statement) on 100% of non-ambiguous queries.

**Note**: Performance metrics, SQL accuracy thresholds, and evaluation
benchmarks (e.g., response time targets, SQL generation success rates, query
accuracy percentages) are deferred to future phases. MVP focuses on functional
validation that the core workflow operates correctly.

## Terminology

- **CloudSafe mode**: (MVP) The default operating mode where LLM context
  contains **only schema metadata** (table names, column names, types,
  relationships) and user query text. No patient data, test results, or PHI is
  sent to the LLM. Cloud providers (e.g., Gemini) are permitted. This is the
  only mode implemented in MVP.

- **LocalPHI mode**: (Phase 2) A future operating mode based on MedGemma EHR
  Navigator patterns where local LLMs may access **patient/row data via MCP
  tools** using a manifest→plan→fetch→filter→synthesize workflow. Cloud
  providers are **blocked**; only local/on-prem LLMs (e.g., LM Studio, Ollama)
  permitted. Requires full security hardening before use. See
  `plans/medgemma-methodology-alignment.md`.

- **MCP (Model Context Protocol)**: Standardized protocol for LLM tool access.
  Used by Catalyst agents to retrieve schema metadata and (in LocalPHI mode)
  patient data.

- **A2A (Agent2Agent)**: Protocol for multi-agent coordination. Catalyst uses
  A2A for Router→Schema→SQLGen agent delegation.

## Assumptions & Constraints

### Assumptions

- Users have basic familiarity with natural language querying (similar to search
  engines or chatbots).
- The OpenELIS database schema is relatively stable - schema changes will be
  infrequent enough that schema metadata embeddings/context can be refreshed on
  a predictable cadence (e.g., on deploy or nightly) without impacting users.
- LLM models (cloud or local) are available and accessible when users submit
  queries.
- Users understand that Catalyst generates SQL and may need to refine queries
  based on results.
- The MVP focuses on read-only queries - no data modification capabilities are
  needed.
- **MVP Language Support**: The MVP supports English natural language queries
  only. The long-term target is to natively support any language that OpenELIS
  supports (en, fr, ar, es, hi, pt, sw), consistent with Constitution Principle
  VII (Internationalization First). Note that UI strings must still be
  internationalized (en + fr minimum) via React Intl, even though query
  understanding is English-first in the MVP.

### Constraints

- **Privacy Constraint (CloudSafe mode)**: In CloudSafe mode (MVP), LLM prompts
  MUST contain only schema metadata and user query text - no patient data, test
  results, or PHI. This applies to ALL cloud/external providers unconditionally.
  LocalPHI mode (Phase 2) relaxes this for local-only LLMs with full security
  hardening. See Terminology section.
- **Read-Only Constraint**: All database queries must execute with read-only
  permissions. No INSERT, UPDATE, DELETE, or DDL operations are permitted.
- **Performance Constraint**: Queries returning more than 10,000 rows may be
  limited or require additional filtering to prevent system overload.
- **Schema Constraint**: Catalyst can only query data that exists in the
  OpenELIS database schema. It cannot access external data sources or generate
  data that doesn't exist.
- **Access Control Constraint (MVP)**: Catalyst authorization is enforced at two
  levels: (1) **Endpoint-level** - access restricted to users with
  `Global Administrator` or `Reports` roles (FR-021), and (2) **Table-level** -
  blocked-table list prevents queries to sensitive tables (FR-013). Per-user
  row-level RBAC (filtering query results based on user's facility/patient
  access) is deferred to Phase 2.
- **Technology Constraint**: Must use Carbon Design System for UI (Constitution
  Principle II), React Intl for internationalization (Principle VII), and follow
  5-layer architecture (Principle IV).

### Out of Scope (MVP)

- **LocalPHI Mode** (Phase 2) - MedGemma-style workflow where local LLMs access
  patient/row data via MCP tools (manifest→plan→fetch→filter→synthesize). MVP
  uses CloudSafe mode only (schema metadata, no patient data to LLM). See
  `plans/medgemma-methodology-alignment.md` for architecture. **CRITICAL**: Full
  security guardrails MUST be implemented before ANY patient data is used.
- Report storage, scheduling, and sharing (Phase 3)
- Dashboard widgets and visualizations (Phase 3)
- **Complex multi-agent orchestration** (Phase 2) - MVP implements a simple
  3-agent team (Router, Schema, SQLGen); advanced orchestration patterns,
  external agent federation, and dynamic agent discovery deferred
- **Tier B (40GB+ VRAM) model evaluation** (Post-MVP) - MVP validates Tier A
  (12GB VRAM) configuration only; Tier B evaluation deferred until appropriate
  hardware available
- **Polished UI example queries** (Post-MVP) - MVP may use placeholder examples;
  final user-tested examples deferred pending user research (FR-014)
- PDF or Excel export formats (Phase 3)
- Wizard mode for query building (Phase 3)
- Row-level RBAC integration with OpenELIS user permissions (Phase 2) - MVP
  restricts endpoint access to privileged roles and uses blocked table list;
  per-user/facility data filtering is deferred
- Natural language query refinement suggestions (future enhancement)
- Support for complex analytical queries requiring multiple steps (future
  enhancement)
- Advanced schema RAG improvements beyond MCP-based schema filtering (future
  enhancement)
