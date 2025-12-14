# Research: Pharmaceuticals Laboratory Workflow

## Decisions & Rationale

- **Reuse NotebookService/Page templates (OGC-51)**  
  **Decision**: Extend existing notebook/page architecture for pharma assays
  (HPLC potency, TLC ID, dissolution) and review flows.  
  **Rationale**: Reduces new surface area; preserves review/OOS/CAPA patterns
  already proven.  
  **Alternatives considered**: New bespoke assay forms — rejected (duplicative,
  higher risk).

- **Reuse Storage services for hierarchical locations**  
  **Decision**: Use existing room/device/rack/box/position models and APIs.  
  **Rationale**: Matches required hierarchy; avoids new schema.  
  **Alternatives considered**: New pharma-specific storage layer — rejected
  (duplicate functionality).

- **Labeling/barcode generation**  
  **Decision**: Use existing barcode/QR generation patterns from storage/sample
  features; add pharma-specific label templates (sample + aliquot).  
  **Rationale**: Keeps scanner compatibility; minimal changes.  
  **Alternatives considered**: New label engine — rejected (scope/time).

- **Chain-of-custody and audit**  
  **Decision**: Reuse existing audit trail; add ChainOfCustodyEvent records tied
  to samples/aliquots for retrieval/shipping.  
  **Rationale**: Constitution requires auditability; minimal additions.  
  **Alternatives considered**: External tracking tool — rejected (integration
  overhead).

- **FHIR exposure**  
  **Decision**: No new external FHIR resources unless regulatory sharing is
  required; keep internal LMIS-only for pharma QC/assay.  
  **Rationale**: Pharma QC not typically FHIR-exposed; avoid unnecessary
  mappings.  
  **Alternatives considered**: Immediate FHIR mapping — deferred until
  requirement emerges.

- **Performance/freshness targets**  
  **Decision**: Dashboard freshness ≤15 minutes via existing reporting/event
  tables; accession/label ≤2 minutes; retrieval log ≤5 minutes.  
  **Rationale**: Matches operational expectations; no special scaling required
  now.  
  **Alternatives considered**: Real-time streaming — not needed for current
  scope.

- **Testing approach**  
  **Decision**: JUnit4 + Mockito for services, BaseWebContextSensitiveTest for
  controllers/DAO, Jest/RTL for pages, Cypress per feature file (not full
  suite).  
  **Rationale**: Constitution-mandated stack.  
  **Alternatives considered**: JUnit5/Spring Boot slices — not permitted.

## Unknowns Resolved

No open NEEDS CLARIFICATION items remain. Performance and FHIR exposure
decisions scoped above; revisit if external data-sharing requirements appear.
