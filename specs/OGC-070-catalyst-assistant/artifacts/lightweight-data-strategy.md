# Research: Lightweight Data Strategy for Catalyst

**Feature**: OGC-070 Catalyst Assistant **Date**: 2026-01-28 **Status**: Draft /
Research

## Executive Summary

This document evaluates lightweight alternatives to Debezium (CDC) for creating
an AI-ready read model for OpenELIS, and defines the role of vector embeddings.

**Key Decision**: For the MVP and near-term, we will use **On-Demand
Flattening** (Virtual Read Model) via MCP tools. For the long-term persistent
read model, we prioritize **Hibernate Listeners** or **Transactional Outbox**
over Debezium to minimize infrastructure complexity.

---

## 1. Flattening Strategy: Alternatives to Debezium

**Constraint**: Avoid heavy infrastructure (Kafka/Debezium) where possible.

### Option A: Hibernate Entity Listeners (Recommended for Phase 2)

- **Mechanism**: Use JPA/Hibernate `@PostPersist`, `@PostUpdate`, `@PostRemove`
  listeners on core entities (`Sample`, `Analysis`, `Result`).
- **Flow**:
  1.  User saves Sample.
  2.  Hibernate Listener fires.
  3.  Listener extracts relevant data, flattens it to JSON.
  4.  Listener pushes JSON to the Read Store (Elasticsearch/VectorDB)
      asynchronously (e.g., via a simple in-memory queue or DB table).
- **Pros**:
  - No external services (runs inside OpenELIS JVM).
  - Access to full entity state/context immediately.
- **Cons**:
  - Adds slight overhead to write transactions.
  - Requires careful error handling to avoid breaking the main transaction.

### Option B: Transactional Outbox Pattern (Robust Alternative)

- **Mechanism**:
  1.  In the _same_ transaction as the entity save, write a record to an
      `outbox` table (`event_type`, `payload`, `status`).
  2.  A separate background thread (poller) reads `outbox`, pushes to Read
      Store, and marks as processed.
- **Pros**:
  - Guaranteed consistency (atomicity).
  - Decouples processing from user request.
- **Cons**:
  - Adds write load to the primary DB.
  - Poller implementation required.

### Option C: PostgreSQL Materialized Views (Analytics Only)

- **Mechanism**: Define complex joins as a view; refresh periodically.
- **Pros**: Native DB feature, zero code.
- **Cons**: Data is stale between refreshes; expensive to refresh (locks or high
  load). Not suitable for real-time agents.

### Option D: On-Demand / Virtual (MVP / LocalPHI)

- **Mechanism**: The Agent calls a tool (e.g., `get_sample_summary`) which
  executes an optimized SQL query _at that moment_ to fetch and flatten data.
- **Pros**: Zero storage overhead, always fresh, no infra.
- **Cons**: Higher latency per query; complex SQL maintenance.
- **Verdict**: **Use this for MVP.**

---

## 2. The Role of Vector Search

How do vectors complement the Text-to-SQL approach?

### A. Schema RAG (Current Plan)

- **What**: Embed table/column descriptions.
- **Why**: Helps the LLM find the _right tables_ to query in a large schema.
- **Status**: **Core to Catalyst MVP.**

### B. Semantic Row Search (Future Enhancement)

- **What**: Create a text summary for each Sample/Patient (e.g., "Male, 45,
  Malaria positive, fever symptoms") and embed it.
- **Why**: Enables fuzzy/semantic queries that SQL handles poorly.
  - _User_: "Find samples with tropical disease indicators."
  - _SQL_: Hard (needs list of specific tests/results).
  - _Vector_: Easy (semantic similarity to "tropical disease").
- **Implementation**: The "Flattener" (Listener/Outbox) generates this text
  summary and sends it to the Vector DB.

### C. Hybrid Search (The Goal)

- **Strategy**:
  1.  Use **SQL** for precise filters (Date > 2024, Status = 'Completed').
  2.  Use **Vectors** for narrative/fuzzy matching (Notes ~= "clotted").
  3.  Combine results.

---

## 3. Recommendation Roadmap

1.  **Phase 1 (MVP)**:
    - **Flattening**: On-Demand (SQL tools).
    - **Vectors**: Schema RAG only.
2.  **Phase 2 (Persistent Read Model)**:
    - **Flattening**: Hibernate Listeners (push to `catalyst_read_store`).
    - **Vectors**: Embed "Sample Summaries" for semantic search.
3.  **Phase 3 (Scale)**:
    - **Flattening**: Transactional Outbox (if listeners impact perf).
    - **Vectors**: Full Hybrid Search.
