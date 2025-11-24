# research.md — Patient ID Card Scanning and Management

## Unknowns (from spec)
- Document retention period for soft-deleted/archived documents
- Multi-page document handling (front/back as single multi-page PDF vs separate documents)
- OCR extraction: in-scope or future enhancement

---

### Decision 1: Document retention policy

Decision: Retain soft-deleted and archived ID documents for 7 years by default.

Rationale:
- Medical records and associated identification are often subject to multi-year retention policies; 7 years is a conservative default balancing auditability and storage cost.
- Product teams can override per-country policy via configuration; implement retention as a configurable property (e.g., `documents.retention.years`) and provide an administrative purge job.

Alternatives considered:
- 1 year: cheaper storage costs but may not satisfy some regulatory needs.
- Indefinite retention: highest auditability but increases storage and compliance burden.

Implementation notes:
- Soft-delete flag and `deletedAt` timestamp stored on `DocumentVersion` / `IDDocument` rows.
- Scheduled purge job (configurable) will permanently remove binaries after retention window and record purge actions in audit logs.

---

### Decision 2: Multi-page document handling

Decision: Support both multi-page PDFs and single-image front/back uploads. Default capture flow (camera) will upload front and back as separate `IDDocument` versions linked by a common `documentGroupId` when the user indicates they belong together. Multi-page PDFs will be stored as a single `DocumentVersion` with appropriate contentType (`application/pdf`).

Rationale:
- Many capture scenarios (mobile/tablet camera) naturally produce two images (front/back). Storing as separate versions gives simpler preview and replacement semantics.
- Some countries or workflows prefer a single multi-page PDF (scanned by an office scanner). We must support that as well to be interoperable.
- FHIR `DocumentReference` supports attachments that are PDFs — storing multi-page PDFs aligns with the FHIR model.

Alternatives considered:
- Enforce single representation (convert front/back images into multi-page PDF on the client): adds client complexity and may fail on low-power devices.
- Always split multi-page PDFs into pages stored as separate DocumentVersions: increases complexity for reassembly and for archival.

Implementation notes:
- Add optional `documentGroupId` to link related documents (front/back) in UI and API.
- Support listing by group and viewing grouped thumbnails; viewing a PDF uses PDF viewer with pagination.

---

### Decision 3: OCR extraction

Decision: OCR is OUT OF-SCOPE for initial delivery. Provide hooks and design for future OCR integration but do not perform OCR in MVP.

Rationale:
- OCR introduces complexity (accuracy, language support, PII extraction risks) and is best delivered as a separate feature with explicit testing and security review.
- MVP should focus on secure capture, storage, FHIR mapping, and audit trail.

Alternatives considered:
- Integrate OCR (Tesseract or cloud OCR) in MVP — rejected due to scope and privacy implications.
- Provide optional, configurable OCR pipeline as a background job in later phase.

Implementation notes:
- Design `DocumentVersion` metadata to include optional `ocrStatus`, `ocrExtractedFields` (JSON), and `ocrVersion` to allow future OCR results to be stored without schema changes.

---

## Summary of Research Decisions
- Retention: default 7 years (configurable)
- Multi-page: support both; default: separate images for camera captures, single multi-page PDF for scans; link via `documentGroupId` when needed
- OCR: deferred to future; provide schema hooks for later integration

