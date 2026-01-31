# Comprehensive FHIR Facade Layer for OpenELIS Global

## Project Overview

| **Attribute**        | **Details**                                                                                                            |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **Project Duration** | 350 Hours                                                                                                              |
| **Technologies**     | Java 21, Spring Framework 6.2.2, HAPI FHIR R4 (6.6.2), REST                                                            |
| **Goal**             | Create a native FHIR facade for OpenELIS enabling direct FHIR transactions without external HAPI JPA server dependency |

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current Architecture Analysis](#current-architecture-analysis)
3. [Problem Statement](#problem-statement)
4. [Proposed Architecture](#proposed-architecture)
5. [FHIR Resources Mapping](#fhir-resources-mapping)
6. [Implementation Strategy](#implementation-strategy)
7. [Constraints & Challenges](#constraints--challenges)
8. [Migration Strategy](#migration-strategy)
9. [Project Timeline](#project-timeline)
10. [Success Criteria](#success-criteria)

---

## Executive Summary

OpenELIS Global currently implements FHIR interoperability through a **parallel
HAPI FHIR JPA server** architecture. While functional, this creates significant
challenges around data synchronization, operational complexity, and potential
data consistency issues.

This proposal outlines the creation of a **native FHIR facade layer** that
exposes OpenELIS data directly as FHIR resources.

### GSOC Scope vs Long-Term Vision

| Aspect              | GSOC 2026 Scope                                                                   | Long-Term Vision                       |
| ------------------- | --------------------------------------------------------------------------------- | -------------------------------------- |
| **Architecture**    | Facade runs **in parallel** with HAPI JPA server                                  | Facade **replaces** HAPI JPA server    |
| **FHIR Coverage**   | Priority resources (Patient, ServiceRequest, Task, DiagnosticReport, Observation) | All FHIR resources currently supported |
| **Traffic Routing** | Configurable routing: facade vs HAPI per resource type                            | 100% traffic to facade                 |
| **HAPI Server**     | **Retained** as fallback and for unsupported operations                           | **Removed** from deployment            |
| **Data Flow**       | Dual-write during transition (OpenELIS → both servers)                            | Single source (OpenELIS only)          |

> **GSOC Deliverable**: A working FHIR facade for **priority resources** that
> can handle core lab workflows, running alongside the existing HAPI server with
> configurable traffic routing.

---

## Current Architecture Analysis

### Current System Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        CURRENT ARCHITECTURE                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌──────────────┐         ┌──────────────────────────────────────────────────┐ │
│   │   External   │         │            OpenELIS Global Application           │ │
│   │   Systems    │         │  ┌────────────────────────────────────────────┐  │ │
│   │  (OpenMRS,   │◄───────►│  │         REST Controllers                   │  │ │
│   │   OpenHIM,   │  FHIR   │  │  /rest/fhir/{resourceType}                 │  │ │
│   │    HIE)      │         │  └────────────────┬───────────────────────────┘  │ │
│   └──────────────┘         │                   │                              │ │
│          │                 │  ┌────────────────▼───────────────────────────┐  │ │
│          │                 │  │         FHIR Transform Service             │  │ │
│          │                 │  │  - transformToFhirPatient()                │  │ │
│          │                 │  │  - transformToServiceRequest()             │  │ │
│          │                 │  │  - transformToObservation()                │  │ │
│          │                 │  │  - transformToDiagnosticReport()           │  │ │
│          │                 │  └────────────────┬───────────────────────────┘  │ │
│          │                 │                   │                              │ │
│          │                 │  ┌────────────────▼───────────────────────────┐  │ │
│          │                 │  │         FHIR Persistence Service           │  │ │
│          │                 │  │  - createFhirResourceInFhirStore()         │  │ │
│          │                 │  │  - updateFhirResourceInFhirStore()         │  │ │
│          │                 │  │  - Uses IGenericClient to talk to HAPI     │  │ │
│          │                 │  └────────────────┬───────────────────────────┘  │ │
│          │                 │                   │                              │ │
│          │                 │  ┌────────────────▼───────────────────────────┐  │ │
│          │                 │  │         OpenELIS Services                  │  │ │
│          │                 │  │  - PatientService                          │  │ │
│          │                 │  │  - SampleService                           │  │ │
│          │                 │  │  - AnalysisService                         │  │ │
│          │                 │  │  - ResultService                           │  │ │
│          │                 │  └────────────────┬───────────────────────────┘  │ │
│          │                 │                   │                              │ │
│          │                 │  ┌────────────────▼───────────────────────────┐  │ │
│          │                 │  │              DAOs                          │  │ │
│          │                 │  └────────────────┬───────────────────────────┘  │ │
│          │                 └───────────────────┼──────────────────────────────┘ │
│          │                                     │                                │
│          │    HTTP/FHIR                        │ JDBC/JPA                       │
│          │    REST Calls                       │                                │
│          ▼                                     ▼                                │
│   ┌──────────────────────┐            ┌──────────────────────┐                 │
│   │   HAPI FHIR JPA      │            │    PostgreSQL        │                 │
│   │   Server Container   │            │    Database          │                 │
│   │   (fhir.openelis.org)│            │    (clinlims)        │                 │
│   │                      │            │                      │                 │
│   │   Port: 8443/8444    │            │  ┌────────────────┐  │                 │
│   │                      │◄──────────►│  │  FHIR Schema   │  │                 │
│   │   ┌───────────────┐  │   JDBC     │  │  (HFJ_* tables)│  │                 │
│   │   │ HAPI JPA      │  │            │  └────────────────┘  │                 │
│   │   │ Server        │  │            │                      │                 │
│   │   │ (Tomcat)      │  │            │  ┌────────────────┐  │                 │
│   │   └───────────────┘  │            │  │ OpenELIS       │  │                 │
│   └──────────────────────┘            │  │ Schema         │  │                 │
│                                       │  │ (sample,       │  │                 │
│                                       │  │  patient,      │  │                 │
│                                       │  │  result, etc.) │  │                 │
│                                       │  └────────────────┘  │                 │
│                                       └──────────────────────┘                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Current Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     CURRENT DATA SYNCHRONIZATION FLOW                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   1. DATA ENTRY (Create Patient, Order, Result)                                 │
│   ──────────────────────────────────────────────                                │
│                                                                                  │
│   ┌─────────────┐    ┌───────────────┐    ┌───────────────┐    ┌─────────────┐ │
│   │ UI/REST     │───►│ Controller    │───►│ Service       │───►│ DAO         │ │
│   │ Request     │    │ (validate)    │    │ (business     │    │ (persist)   │ │
│   └─────────────┘    └───────────────┘    │  logic)       │    └──────┬──────┘ │
│                                           └───────┬───────┘           │        │
│                                                   │                   │        │
│                                                   ▼                   ▼        │
│                                           ┌───────────────┐   ┌─────────────┐  │
│                                           │ FhirTransform │   │ PostgreSQL  │  │
│                                           │ Service       │   │ (OpenELIS   │  │
│                                           │ (async)       │   │  tables)    │  │
│                                           └───────┬───────┘   └─────────────┘  │
│                                                   │                            │
│                                                   ▼                            │
│                                           ┌───────────────┐                    │
│                                           │ FhirPersist   │                    │
│                                           │ Service       │                    │
│                                           └───────┬───────┘                    │
│                                                   │                            │
│                                    HTTP Transaction Bundle                     │
│                                                   │                            │
│                                                   ▼                            │
│                                           ┌───────────────┐   ┌─────────────┐  │
│                                           │ HAPI FHIR     │──►│ PostgreSQL  │  │
│                                           │ JPA Server    │   │ (FHIR       │  │
│                                           │               │   │  tables)    │  │
│                                           └───────────────┘   └─────────────┘  │
│                                                                                │
│   2. EXTERNAL FHIR QUERY                                                       │
│   ──────────────────────                                                       │
│                                                                                │
│   ┌─────────────┐    ┌───────────────┐    ┌───────────────┐    ┌─────────────┐ │
│   │ External    │───►│ OpenELIS     │───►│ HAPI FHIR     │───►│ FHIR Tables │ │
│   │ System      │    │ Proxy        │    │ JPA Server    │    │ (HFJ_*)     │ │
│   └─────────────┘    └───────────────┘    └───────────────┘    └─────────────┘ │
│                                                                                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Current Key Components

| Component                  | File/Location                                               | Purpose                                       |
| -------------------------- | ----------------------------------------------------------- | --------------------------------------------- |
| **FhirConfig**             | `dataexchange/fhir/FhirConfig.java`                         | Configuration for FHIR endpoints, credentials |
| **FhirUtil**               | `dataexchange/fhir/FhirUtil.java`                           | FHIR client factory, parser utilities         |
| **FhirPersistanceService** | `dataexchange/fhir/service/FhirPersistanceServiceImpl.java` | CRUD operations to external HAPI server       |
| **FhirTransformService**   | `dataexchange/fhir/service/FhirTransformServiceImpl.java`   | Entity ↔ FHIR resource transformation         |
| **FhirApiWorkflowService** | `dataexchange/fhir/service/FhirApiWorkFlowServiceImpl.java` | Task polling, order import, result sync       |
| **HAPI FHIR Container**    | `fhir.openelis.org:8443`                                    | Separate Tomcat container running HAPI JPA    |

### Current FHIR Resources Supported

Based on code analysis, the following FHIR R4 resources are currently
transformed and synchronized:

| FHIR Resource             | OpenELIS Entity             | Transform Direction |
| ------------------------- | --------------------------- | ------------------- |
| **Patient**               | `Patient`, `Person`         | Bidirectional       |
| **Practitioner**          | `Provider`                  | Bidirectional       |
| **Organization**          | `Organization`              | Bidirectional       |
| **Specimen**              | `SampleItem`                | OpenELIS → FHIR     |
| **ServiceRequest**        | `Analysis`, `Sample`        | Bidirectional       |
| **DiagnosticReport**      | `Analysis` (finalized)      | OpenELIS → FHIR     |
| **Observation**           | `Result`                    | OpenELIS → FHIR     |
| **Task**                  | `Sample`, `ElectronicOrder` | Bidirectional       |
| **Location**              | `StorageLocation`           | OpenELIS → FHIR     |
| **QuestionnaireResponse** | Questionnaire data          | Inbound processing  |

---

## Problem Statement

### Current Challenges

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     PROBLEMS WITH CURRENT ARCHITECTURE                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   1. DATA SYNCHRONIZATION ISSUES                                                │
│   ──────────────────────────────────                                            │
│                                                                                  │
│   ┌──────────────────┐                      ┌──────────────────┐                │
│   │  OpenELIS DB     │    ≠ SYNC ≠         │  HAPI FHIR DB    │                │
│   │  (Source of      │◄────────────────────►│  (May be stale   │                │
│   │   Truth)         │                      │   or missing     │                │
│   │                  │                      │   data)          │                │
│   └──────────────────┘                      └──────────────────┘                │
│                                                                                  │
│   • Async sync can fail silently                                                │
│   • Network issues cause data loss                                              │
│   • FHIR server restart = potential data gap                                    │
│   • No transactional guarantee across both DBs                                  │
│                                                                                  │
│   2. OPERATIONAL COMPLEXITY                                                      │
│   ─────────────────────────                                                     │
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────┐           │
│   │  Containers to Manage:                                          │           │
│   │                                                                 │           │
│   │  • openelisglobal-webapp (Tomcat + OpenELIS WAR)               │           │
│   │  • external-fhir-api (HAPI FHIR JPA Tomcat)                    │           │
│   │  • openelisglobal-database (PostgreSQL)                        │           │
│   │  • openelisglobal-proxy (Nginx)                                │           │
│   │  • openelisglobal-front-end (React)                            │           │
│   │  • oe-certs (Certificate generation)                           │           │
│   │                                                                 │           │
│   │  = 6 containers minimum, HAPI adds ~500MB memory footprint     │           │
│   └─────────────────────────────────────────────────────────────────┘           │
│                                                                                  │
│   3. RESOURCE DUPLICATION                                                        │
│   ───────────────────────                                                       │
│                                                                                  │
│   ┌───────────────────────────────────────────────────────────────────────────┐ │
│   │  Database Tables Duplication:                                             │ │
│   │                                                                           │ │
│   │  OpenELIS Schema:          │    HAPI FHIR Schema (HFJ_*):                │ │
│   │  ─────────────────         │    ────────────────────────                 │ │
│   │  • patient                 │    • HFJ_RESOURCE                           │ │
│   │  • sample                  │    • HFJ_RES_VER                            │ │
│   │  • analysis                │    • HFJ_SPIDX_* (search indexes)           │ │
│   │  • result                  │    • HFJ_IDX_* (composite indexes)          │ │
│   │  • provider                │    • HFJ_RES_LINK                           │ │
│   │  • organization            │    • HFJ_HISTORY_TAG                        │ │
│   │                            │    • HFJ_RES_TAG                            │ │
│   │                            │    • ...~50+ tables                         │ │
│   └───────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│   4. QUERY LIMITATIONS                                                           │
│   ────────────────────                                                          │
│                                                                                  │
│   • FHIR queries hit HAPI server (stale data risk)                              │
│   • Cannot do FHIR + OpenELIS joins                                             │
│   • Limited search parameter support                                            │
│   • Performance overhead of HTTP calls for local data                           │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Proposed Architecture

### GSOC Architecture: Parallel Operation Mode

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                   GSOC ARCHITECTURE: PARALLEL OPERATION MODE                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌──────────────┐                                                              │
│   │   External   │                                                              │
│   │   Systems    │                                                              │
│   └──────┬───────┘                                                              │
│          │                                                                       │
│          ▼                                                                       │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                         Nginx Proxy                                     │   │
│   │  ┌─────────────────────────────────────────────────────────────────┐    │   │
│   │  │                    FHIR Router (NEW)                            │    │   │
│   │  │                                                                 │    │   │
│   │  │   Route by Resource Type + Operation:                           │    │   │
│   │  │   ┌─────────────────┬─────────────────┬─────────────────────┐   │    │   │
│   │  │   │ Resource        │ Facade Ready?   │ Route To            │   │    │   │
│   │  │   ├─────────────────┼─────────────────┼─────────────────────┤   │    │   │
│   │  │   │ Patient         │ ✅ Yes          │ OpenELIS Facade     │   │    │   │
│   │  │   │ ServiceRequest  │ ✅ Yes          │ OpenELIS Facade     │   │    │   │
│   │  │   │ Task            │ ✅ Yes          │ OpenELIS Facade     │   │    │   │
│   │  │   │ DiagnosticReport│ ✅ Yes          │ OpenELIS Facade     │   │    │   │
│   │  │   │ Observation     │ ✅ Yes          │ OpenELIS Facade     │   │    │   │
│   │  │   │ Specimen        │ ⏳ Phase 2      │ HAPI JPA Server     │   │    │   │
│   │  │   │ Practitioner    │ ⏳ Phase 2      │ HAPI JPA Server     │   │    │   │
│   │  │   │ Organization    │ ⏳ Phase 2      │ HAPI JPA Server     │   │    │   │
│   │  │   │ Location        │ ⏳ Post-GSOC    │ HAPI JPA Server     │   │    │   │
│   │  │   │ Bundle/Transaction│ ⏳ Post-GSOC  │ HAPI JPA Server     │   │    │   │
│   │  │   └─────────────────┴─────────────────┴─────────────────────┘   │    │   │
│   │  └─────────────────────────────────────────────────────────────────┘    │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│          │                                           │                          │
│          │ Facade-Ready Resources                    │ Fallback Resources       │
│          ▼                                           ▼                          │
│   ┌─────────────────────────┐                 ┌─────────────────────────┐       │
│   │  OpenELIS Application   │                 │  HAPI FHIR JPA Server   │       │
│   │  ┌───────────────────┐  │                 │  (Existing - Retained)  │       │
│   │  │  FHIR Facade      │  │                 │                         │       │
│   │  │  /api/fhir/*      │  │                 │  fhir.openelis.org:8443 │       │
│   │  └─────────┬─────────┘  │                 │                         │       │
│   │            │            │                 └────────────┬────────────┘       │
│   │  ┌─────────▼─────────┐  │                              │                    │
│   │  │  OpenELIS Services│  │                              │                    │
│   │  └─────────┬─────────┘  │                              │                    │
│   │            │            │                              │                    │
│   └────────────┼────────────┘                              │                    │
│                │                                           │                    │
│                ▼                                           ▼                    │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                       PostgreSQL Database                               │   │
│   │   ┌─────────────────────────────┐   ┌─────────────────────────────┐     │   │
│   │   │  OpenELIS Schema            │   │  HAPI FHIR Schema           │     │   │
│   │   │  (Source of Truth)          │   │  (HFJ_* tables - retained)  │     │   │
│   │   └─────────────────────────────┘   └─────────────────────────────┘     │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Long-Term Target Architecture (Post-GSOC)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        PROPOSED FHIR FACADE ARCHITECTURE                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌──────────────┐                                                              │
│   │   External   │                                                              │
│   │   Systems    │                                                              │
│   │  (OpenMRS,   │                                                              │
│   │   OpenHIM,   │                                                              │
│   │    HIE)      │                                                              │
│   └──────┬───────┘                                                              │
│          │                                                                       │
│          │  FHIR R4 REST API                                                    │
│          │  (Standard FHIR Operations)                                          │
│          ▼                                                                       │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                    OpenELIS Global Application                           │   │
│   │  ┌───────────────────────────────────────────────────────────────────┐  │   │
│   │  │                 FHIR FACADE LAYER (NEW)                           │  │   │
│   │  │  ┌─────────────────────────────────────────────────────────────┐  │  │   │
│   │  │  │           HAPI FHIR Plain Server (Embedded)                 │  │  │   │
│   │  │  │                                                             │  │  │   │
│   │  │  │   Endpoint: /fhir/*                                         │  │  │   │
│   │  │  │                                                             │  │  │   │
│   │  │  │   Capabilities:                                             │  │  │   │
│   │  │  │   • FHIR R4 Conformance                                     │  │  │   │
│   │  │  │   • Transaction Bundles                                     │  │  │   │
│   │  │  │   • Search Parameters                                       │  │  │   │
│   │  │  │   • _include, _revinclude                                   │  │  │   │
│   │  │  │   • Pagination                                              │  │  │   │
│   │  │  │   • SMART on FHIR (future)                                  │  │  │   │
│   │  │  └────────────────────────┬────────────────────────────────────┘  │  │   │
│   │  │                           │                                       │  │   │
│   │  │  ┌────────────────────────▼────────────────────────────────────┐  │  │   │
│   │  │  │              FHIR Resource Providers                        │  │  │   │
│   │  │  │                                                             │  │  │   │
│   │  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │  │  │   │
│   │  │  │  │PatientResource│ │ServiceRequest│ │DiagnosticRpt │        │  │  │   │
│   │  │  │  │Provider      │ │Provider      │ │Provider      │        │  │  │   │
│   │  │  │  └──────────────┘ └──────────────┘ └──────────────┘        │  │  │   │
│   │  │  │                                                             │  │  │   │
│   │  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │  │  │   │
│   │  │  │  │Observation   │ │Specimen      │ │Practitioner  │        │  │  │   │
│   │  │  │  │Provider      │ │Provider      │ │Provider      │        │  │  │   │
│   │  │  │  └──────────────┘ └──────────────┘ └──────────────┘        │  │  │   │
│   │  │  │                                                             │  │  │   │
│   │  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │  │  │   │
│   │  │  │  │Task          │ │Organization  │ │Location      │        │  │  │   │
│   │  │  │  │Provider      │ │Provider      │ │Provider      │        │  │  │   │
│   │  │  │  └──────────────┘ └──────────────┘ └──────────────┘        │  │  │   │
│   │  │  └────────────────────────┬────────────────────────────────────┘  │  │   │
│   │  └───────────────────────────┼───────────────────────────────────────┘  │   │
│   │                              │                                          │   │
│   │  ┌───────────────────────────▼───────────────────────────────────────┐  │   │
│   │  │                FHIR Transform Service (Enhanced)                  │  │   │
│   │  │                                                                   │  │   │
│   │  │  • Bidirectional transformation                                   │  │   │
│   │  │  • FHIR validation                                                │  │   │
│   │  │  • Extension handling                                             │  │   │
│   │  │  • CodeSystem/ValueSet resolution                                 │  │   │
│   │  └───────────────────────────┬───────────────────────────────────────┘  │   │
│   │                              │                                          │   │
│   │  ┌───────────────────────────▼───────────────────────────────────────┐  │   │
│   │  │                    OpenELIS Services                              │  │   │
│   │  │  PatientService | SampleService | AnalysisService | ResultService │  │   │
│   │  └───────────────────────────┬───────────────────────────────────────┘  │   │
│   │                              │                                          │   │
│   │  ┌───────────────────────────▼───────────────────────────────────────┐  │   │
│   │  │                         DAOs                                      │  │   │
│   │  └───────────────────────────┬───────────────────────────────────────┘  │   │
│   └──────────────────────────────┼──────────────────────────────────────────┘   │
│                                  │                                              │
│                                  │ JPA/Hibernate                                │
│                                  ▼                                              │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                       PostgreSQL Database                               │   │
│   │                                                                         │   │
│   │   ┌─────────────────────────────────────────────────────────────────┐   │   │
│   │   │                   OpenELIS Schema (clinlims)                    │   │   │
│   │   │                                                                 │   │   │
│   │   │  • patient (with fhir_uuid)                                     │   │   │
│   │   │  • sample (with fhir_uuid)                                      │   │   │
│   │   │  • analysis (with fhir_uuid)                                    │   │   │
│   │   │  • result (with fhir_uuid)                                      │   │   │
│   │   │  • provider (with fhir_uuid)                                    │   │   │
│   │   │  • organization (with fhir_uuid)                                │   │   │
│   │   │  • storage_* (with fhir_uuid)                                   │   │   │
│   │   │                                                                 │   │   │
│   │   │  ┌─────────────────────────────────────────────────────────┐    │   │   │
│   │   │  │  NEW: FHIR Search Index Tables (Optional Optimization)  │    │   │   │
│   │   │  │  • fhir_string_idx                                      │    │   │   │
│   │   │  │  • fhir_token_idx                                       │    │   │   │
│   │   │  │  • fhir_date_idx                                        │    │   │   │
│   │   │  │  • fhir_reference_idx                                   │    │   │   │
│   │   │  └─────────────────────────────────────────────────────────┘    │   │   │
│   │   └─────────────────────────────────────────────────────────────────┘   │   │
│   │                                                                         │   │
│   │   ┌─────────────────────────────────────────────────────────────────┐   │   │
│   │   │              HAPI FHIR Schema (REMOVED)                         │   │   │
│   │   │                    ████████████████                             │   │   │
│   │   │                       NOT NEEDED                                │   │   │
│   │   └─────────────────────────────────────────────────────────────────┘   │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│                    ┌─────────────────────────────────────────┐                  │
│                    │     HAPI FHIR Container (REMOVED)       │                  │
│                    │           ████████████████              │                  │
│                    │              NOT NEEDED                 │                  │
│                    └─────────────────────────────────────────┘                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### New Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      NEW SIMPLIFIED DATA FLOW                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   1. FHIR READ OPERATION (GET /fhir/Patient/123)                                │
│   ──────────────────────────────────────────────                                │
│                                                                                  │
│   ┌─────────────┐    ┌───────────────┐    ┌───────────────┐    ┌─────────────┐ │
│   │ External    │───►│ FHIR Plain   │───►│ Patient       │───►│ Patient     │ │
│   │ System      │    │ Server       │    │ Resource      │    │ Service     │ │
│   │             │    │ (/fhir/*)    │    │ Provider      │    │             │ │
│   └─────────────┘    └───────────────┘    └───────┬───────┘    └──────┬──────┘ │
│          ▲                                        │                   │        │
│          │                                        │                   ▼        │
│          │           ┌───────────────┐            │           ┌─────────────┐  │
│          └───────────│ FHIR Patient  │◄───────────┘           │ PostgreSQL  │  │
│            Response  │ Resource      │  Transform             │ (patient    │  │
│                      └───────────────┘  to FHIR               │  table)     │  │
│                                                               └─────────────┘  │
│                                                                                │
│   2. FHIR WRITE OPERATION (POST /fhir/Patient)                                 │
│   ────────────────────────────────────────────                                 │
│                                                                                │
│   ┌─────────────┐    ┌───────────────┐    ┌───────────────┐    ┌─────────────┐ │
│   │ External    │───►│ FHIR Plain   │───►│ Patient       │───►│ Patient     │ │
│   │ System      │    │ Server       │    │ Resource      │    │ Service     │ │
│   │ (FHIR       │    │              │    │ Provider      │    │             │ │
│   │  Patient)   │    │              │    │ @Create       │    └──────┬──────┘ │
│   └─────────────┘    └───────────────┘    └───────┬───────┘           │        │
│          ▲                                        │                   │        │
│          │                                        │ Transform         ▼        │
│          │                                        │ to Entity  ┌─────────────┐ │
│          │           ┌───────────────┐            │            │ PostgreSQL  │ │
│          └───────────│ FHIR Response │◄───────────┘            │ (INSERT)    │ │
│            201       │ (Location hdr)│                         └─────────────┘ │
│                      └───────────────┘                                         │
│                                                                                │
│   3. FHIR TRANSACTION BUNDLE                                                   │
│   ──────────────────────────                                                   │
│                                                                                │
│   ┌─────────────┐    ┌───────────────┐    ┌───────────────┐    ┌─────────────┐ │
│   │ Transaction │───►│ FHIR Plain   │───►│ Transaction   │───►│ @Transact.  │ │
│   │ Bundle      │    │ Server       │    │ Handler       │    │ Service     │ │
│   │ (Patient +  │    │              │    │ (validates,   │    │ (all-or-    │ │
│   │  Specimen + │    │              │    │  orders ops)  │    │  nothing)   │ │
│   │  ServiceReq)│    │              │    │               │    │             │ │
│   └─────────────┘    └───────────────┘    └───────┬───────┘    └──────┬──────┘ │
│          ▲                                        │                   │        │
│          │                                        │                   ▼        │
│          │           ┌───────────────┐            │           ┌─────────────┐  │
│          └───────────│ Transaction   │◄───────────┘           │ PostgreSQL  │  │
│            Response  │ Response      │                        │ (Single     │  │
│                      │ Bundle        │                        │  Transaction│  │
│                      └───────────────┘                        └─────────────┘  │
│                                                                                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Component Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        FHIR FACADE COMPONENT DIAGRAM                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   org.openelisglobal.fhir.facade                                                │
│   ├── config/                                                                   │
│   │   ├── FhirServerConfig.java          # HAPI Plain Server configuration      │
│   │   ├── FhirSecurityConfig.java        # FHIR endpoint security              │
│   │   └── FhirCapabilityConfig.java      # Capability statement setup          │
│   │                                                                             │
│   ├── server/                                                                   │
│   │   ├── OpenElisFhirServlet.java       # Main FHIR servlet                   │
│   │   ├── OpenElisFhirServer.java        # Server initialization               │
│   │   └── FhirServerInterceptors.java    # Request/response interceptors       │
│   │                                                                             │
│   ├── provider/                          # FHIR Resource Providers             │
│   │   ├── PatientResourceProvider.java                                         │
│   │   ├── PractitionerResourceProvider.java                                    │
│   │   ├── OrganizationResourceProvider.java                                    │
│   │   ├── ServiceRequestResourceProvider.java                                  │
│   │   ├── SpecimenResourceProvider.java                                        │
│   │   ├── ObservationResourceProvider.java                                     │
│   │   ├── DiagnosticReportResourceProvider.java                                │
│   │   ├── TaskResourceProvider.java                                            │
│   │   └── LocationResourceProvider.java                                        │
│   │                                                                             │
│   ├── search/                            # FHIR Search Implementation          │
│   │   ├── FhirSearchService.java         # Search coordination                 │
│   │   ├── PatientSearchHandler.java      # Patient-specific searches           │
│   │   ├── ObservationSearchHandler.java  # Observation-specific searches       │
│   │   └── GenericSearchHandler.java      # Common search logic                 │
│   │                                                                             │
│   ├── transaction/                       # Transaction Bundle Processing       │
│   │   ├── TransactionProcessor.java      # Bundle transaction handling         │
│   │   ├── BundleValidator.java           # Bundle validation                   │
│   │   └── ReferenceResolver.java         # Temporary reference resolution      │
│   │                                                                             │
│   └── interceptor/                       # HAPI Interceptors                   │
│       ├── AuditLoggingInterceptor.java   # Audit trail                         │
│       ├── ValidationInterceptor.java     # FHIR validation                     │
│       └── AuthorizationInterceptor.java  # RBAC enforcement                    │
│                                                                                  │
│   org.openelisglobal.dataexchange.fhir (EXISTING - Enhanced)                    │
│   ├── service/                                                                  │
│   │   ├── FhirTransformService.java      # Enhanced bidirectional transforms   │
│   │   └── FhirPersistanceService.java    # Refactored for local-only ops       │
│   └── FhirConfig.java                    # Updated configuration               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## FHIR Resources Mapping

### Detailed Entity-to-FHIR Mapping

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     OPENELLIS ENTITY TO FHIR RESOURCE MAPPING                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌─────────────────┐         FHIR Patient                                      │
│   │    patient      │◄────────────────────────────────────►                     │
│   │    person       │         {                                                 │
│   │    patient_     │           "resourceType": "Patient",                      │
│   │    identity     │           "id": "${fhir_uuid}",                           │
│   └─────────────────┘           "identifier": [...],                            │
│                                 "name": [...],                                  │
│                                 "gender": "...",                                │
│                                 "birthDate": "..."                              │
│                               }                                                 │
│                                                                                  │
│   ┌─────────────────┐         FHIR Practitioner                                 │
│   │    provider     │◄────────────────────────────────────►                     │
│   │    person       │         {                                                 │
│   └─────────────────┘           "resourceType": "Practitioner",                 │
│                                 "id": "${fhir_uuid}",                           │
│                                 "identifier": [...],                            │
│                                 "name": [...]                                   │
│                               }                                                 │
│                                                                                  │
│   ┌─────────────────┐         FHIR Organization                                 │
│   │   organization  │◄────────────────────────────────────►                     │
│   └─────────────────┘         {                                                 │
│                                 "resourceType": "Organization",                 │
│                                 "id": "${fhir_uuid}",                           │
│                                 "name": "...",                                  │
│                                 "type": [...]                                   │
│                               }                                                 │
│                                                                                  │
│   ┌─────────────────┐         FHIR Specimen                                     │
│   │   sample_item   │◄────────────────────────────────────►                     │
│   │   sample        │         {                                                 │
│   │   type_of_      │           "resourceType": "Specimen",                     │
│   │   sample        │           "id": "${fhir_uuid}",                           │
│   └─────────────────┘           "type": {...},                                  │
│                                 "subject": {"reference": "Patient/..."},        │
│                                 "collection": {...}                             │
│                               }                                                 │
│                                                                                  │
│   ┌─────────────────┐         FHIR ServiceRequest                               │
│   │   analysis      │◄────────────────────────────────────►                     │
│   │   test          │         {                                                 │
│   │   sample        │           "resourceType": "ServiceRequest",               │
│   └─────────────────┘           "id": "${fhir_uuid}",                           │
│                                 "status": "active|completed",                   │
│                                 "intent": "order",                              │
│                                 "code": {...},                                  │
│                                 "subject": {"reference": "Patient/..."},        │
│                                 "specimen": [{"reference": "Specimen/..."}]     │
│                               }                                                 │
│                                                                                  │
│   ┌─────────────────┐         FHIR Observation                                  │
│   │   result        │◄────────────────────────────────────►                     │
│   │   analysis      │         {                                                 │
│   │   test          │           "resourceType": "Observation",                  │
│   └─────────────────┘           "id": "${fhir_uuid}",                           │
│                                 "status": "final",                              │
│                                 "code": {...},                                  │
│                                 "subject": {"reference": "Patient/..."},        │
│                                 "valueQuantity": {...} | "valueString": "..."   │
│                               }                                                 │
│                                                                                  │
│   ┌─────────────────┐         FHIR DiagnosticReport                             │
│   │   analysis      │◄────────────────────────────────────►                     │
│   │   (finalized)   │         {                                                 │
│   │   result        │           "resourceType": "DiagnosticReport",             │
│   └─────────────────┘           "id": "${fhir_uuid}",                           │
│                                 "status": "final",                              │
│                                 "code": {...},                                  │
│                                 "result": [{"reference": "Observation/..."}]    │
│                               }                                                 │
│                                                                                  │
│   ┌─────────────────┐         FHIR Task                                         │
│   │   sample        │◄────────────────────────────────────►                     │
│   │   electronic_   │         {                                                 │
│   │   order         │           "resourceType": "Task",                         │
│   └─────────────────┘           "id": "${fhir_uuid}",                           │
│                                 "status": "requested|accepted|completed",       │
│                                 "intent": "order",                              │
│                                 "basedOn": [{"reference": "ServiceRequest/..."}]│
│                               }                                                 │
│                                                                                  │
│   ┌─────────────────┐         FHIR Location                                     │
│   │   storage_room  │◄────────────────────────────────────►                     │
│   │   storage_      │         {                                                 │
│   │   device        │           "resourceType": "Location",                     │
│   │   storage_      │           "id": "${fhir_uuid}",                           │
│   │   shelf/rack/   │           "name": "...",                                  │
│   │   box           │           "physicalType": {...},                          │
│   └─────────────────┘           "partOf": {"reference": "Location/..."}         │
│                               }                                                 │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Implementation Strategy

### GSOC Implementation Strategy

#### Priority Resource Selection

Resources are prioritized based on:

1. **Core Lab Workflow Impact** - Essential for order-to-result flow
2. **Integration Frequency** - Most commonly used by external systems (OpenMRS,
   OpenHIM)
3. **Implementation Complexity** - Balance quick wins with complex features
4. **Existing Transform Coverage** - Leverage existing FhirTransformService code

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     GSOC RESOURCE PRIORITY MATRIX                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   PRIORITY 1 (Must Have - GSOC Core Deliverables)                               │
│   ═══════════════════════════════════════════════                               │
│   ┌────────────────────┬──────────────────┬─────────────────────────────────┐   │
│   │ Resource           │ Operations       │ Rationale                       │   │
│   ├────────────────────┼──────────────────┼─────────────────────────────────┤   │
│   │ Patient            │ Read, Search,    │ Foundation for all lab orders   │   │
│   │                    │ Create, Update   │ Most external queries           │   │
│   ├────────────────────┼──────────────────┼─────────────────────────────────┤   │
│   │ Task               │ Read, Search,    │ Order workflow orchestration    │   │
│   │                    │ Create, Update   │ Critical for OpenMRS/OpenHIM    │   │
│   ├────────────────────┼──────────────────┼─────────────────────────────────┤   │
│   │ ServiceRequest     │ Read, Search     │ Lab order representation        │   │
│   │                    │                  │ Linked to Task workflow         │   │
│   ├────────────────────┼──────────────────┼─────────────────────────────────┤   │
│   │ DiagnosticReport   │ Read, Search     │ Final lab results               │   │
│   │                    │                  │ Primary query by external sys   │   │
│   ├────────────────────┼──────────────────┼─────────────────────────────────┤   │
│   │ Observation        │ Read, Search     │ Individual test results         │   │
│   │                    │                  │ Referenced by DiagnosticReport  │   │
│   └────────────────────┴──────────────────┴─────────────────────────────────┘   │
│                                                                                  │
│   PRIORITY 2 (Should Have - If Time Permits)                                    │
│   ══════════════════════════════════════════                                    │
│   ┌────────────────────┬──────────────────┬─────────────────────────────────┐   │
│   │ Specimen           │ Read, Search     │ Sample tracking                 │   │
│   │ Practitioner       │ Read, Search     │ Provider lookup                 │   │
│   │ Organization       │ Read, Search     │ Facility information            │   │
│   └────────────────────┴──────────────────┴─────────────────────────────────┘   │
│                                                                                  │
│   PRIORITY 3 (Nice to Have - Post-GSOC)                                         │
│   ═════════════════════════════════════                                         │
│   ┌────────────────────┬──────────────────┬─────────────────────────────────┐   │
│   │ Location           │ Full CRUD        │ Storage hierarchy (IHE mCSD)    │   │
│   │ Bundle/Transaction │ Transaction ops  │ Complex atomic operations       │   │
│   │ QuestionnaireResp  │ Create           │ Intake forms                    │   │
│   └────────────────────┴──────────────────┴─────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Phase-by-Phase Approach (GSOC Timeline)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                   GSOC IMPLEMENTATION PHASES (350 Hours)                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   PHASE 1: Foundation & Infrastructure (80 hours)                               │
│   ═══════════════════════════════════════════════                               │
│                                                                                  │
│   Week 1-2: HAPI Plain Server Setup (Parallel Mode)                             │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  • Configure HAPI FHIR Plain Server as Spring @Bean                  │       │
│   │  • Register servlet at /api/fhir/* endpoint (different from HAPI)   │       │
│   │  • Implement CapabilityStatement provider (partial capabilities)     │       │
│   │  • Set up server interceptors (logging, validation)                  │       │
│   │  • Configure security integration with Spring Security               │       │
│   │  • Create base ResourceProvider abstract class                       │       │
│   │  • Implement FhirRouterService for traffic routing decisions         │       │
│   │  • Configuration: fhir.facade.enabled.resources=Patient,Task,...     │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   Deliverables:                                                                 │
│   • GET /api/fhir/metadata returns valid CapabilityStatement                    │
│   • Basic authentication/authorization working                                   │
│   • Logging and audit trail functional                                          │
│   • Router can direct traffic to facade or HAPI based on config                 │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   PHASE 2: Priority 1 Resource Providers (120 hours)                           │
│   ═══════════════════════════════════════════════════                           │
│                                                                                  │
│   Week 3-4: Patient Resource Provider (Core Foundation)                         │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  PatientResourceProvider:                                            │       │
│   │  • @Read: GET /api/fhir/Patient/{id}                                 │       │
│   │  • @Search: GET /api/fhir/Patient?name=...&identifier=...            │       │
│   │  • @Create: POST /api/fhir/Patient                                   │       │
│   │  • @Update: PUT /api/fhir/Patient/{id}                               │       │
│   │  • Enhanced bidirectional transforms (reuse FhirTransformService)    │       │
│   │  • Search params: identifier, name, birthdate, gender                │       │
│   │                                                                      │       │
│   │  Validation:                                                         │       │
│   │  • Compare facade response vs HAPI response for same queries         │       │
│   │  • Ensure data parity before enabling in production                  │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   Week 5-6: Task & ServiceRequest Providers (Order Workflow)                    │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  TaskResourceProvider:                                               │       │
│   │  • Order orchestration support (critical for OpenMRS integration)    │       │
│   │  • Status-based searching (requested, accepted, completed)           │       │
│   │  • @Read, @Search, @Create, @Update                                  │       │
│   │  • basedOn reference resolution to ServiceRequest                    │       │
│   │                                                                      │       │
│   │  ServiceRequestResourceProvider:                                     │       │
│   │  • Complex mapping to Analysis with Test info                        │       │
│   │  • @Read, @Search (read-heavy, write via Task workflow)              │       │
│   │  • Search params: identifier, status, patient, code                  │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   Week 7-8: DiagnosticReport & Observation (Results)                            │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  DiagnosticReportResourceProvider:                                   │       │
│   │  • Aggregate finalized Analysis results                              │       │
│   │  • @Read, @Search (primarily read-only)                              │       │
│   │  • _include for Observation references                               │       │
│   │  • Search params: patient, status, code, date                        │       │
│   │                                                                      │       │
│   │  ObservationResourceProvider:                                        │       │
│   │  • Map Result entity with value types handling                       │       │
│   │  • Numeric, coded, string value support                              │       │
│   │  • @Read, @Search                                                    │       │
│   │  • Search params: patient, code, date, status                        │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   PHASE 3: Priority 2 Resources & Hardening (80 hours)                         │
│   ═════════════════════════════════════════════════════                         │
│                                                                                  │
│   Week 9-10: Priority 2 Resources (If Time Permits)                             │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  SpecimenResourceProvider:                                           │       │
│   │  • Mapped to SampleItem with Sample context                          │       │
│   │  • @Read, @Search                                                    │       │
│   │  • Support _include for Patient reference                            │       │
│   │                                                                      │       │
│   │  PractitionerResourceProvider:                                       │       │
│   │  • Map to Provider entity                                            │       │
│   │  • @Read, @Search (read-only for GSOC)                               │       │
│   │                                                                      │       │
│   │  OrganizationResourceProvider:                                       │       │
│   │  • Map to Organization entity                                        │       │
│   │  • @Read, @Search (read-only for GSOC)                               │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   Week 11: Error Handling & Edge Cases                                          │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  Hardening:                                                          │       │
│   │  • Comprehensive error handling (OperationOutcome)                   │       │
│   │  • Graceful fallback to HAPI when facade fails                       │       │
│   │  • Input validation and sanitization                                 │       │
│   │  • Rate limiting consideration                                       │       │
│   │  • Logging and monitoring integration                                │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   PHASE 4: Testing & Integration (50 hours)                                     │
│   ══════════════════════════════════════════                                    │
│                                                                                  │
│   Week 12: Testing & Validation                                                 │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  Testing Strategy (Parallel Validation):                             │       │
│   │  • Unit tests for all resource providers (JUnit 4 + Mockito)         │       │
│   │  • Integration tests comparing facade vs HAPI responses              │       │
│   │  • FHIR validation using HAPI FHIR Validator                         │       │
│   │  • Performance benchmarking (facade should be faster)                │       │
│   │                                                                      │       │
│   │  Comparison Test Suite:                                              │       │
│   │  • Run same queries against facade and HAPI                          │       │
│   │  • Compare response structure, data completeness                     │       │
│   │  • Flag discrepancies for investigation                              │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   PHASE 5: Documentation & Handoff (20 hours)                                   │
│   ═══════════════════════════════════════════                                   │
│                                                                                  │
│   Week 13: Documentation & Future Roadmap                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  Documentation:                                                      │       │
│   │  • API documentation for implemented resources                       │       │
│   │  • Configuration guide for parallel operation                        │       │
│   │  • Troubleshooting guide                                             │       │
│   │  • Developer guide for adding new resource providers                 │       │
│   │                                                                      │       │
│   │  Future Roadmap:                                                     │       │
│   │  • Document remaining resources to implement post-GSOC               │       │
│   │  • Transaction bundle implementation plan                            │       │
│   │  • Full HAPI removal migration path                                  │       │
│   │  • Performance optimization opportunities                            │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Constraints & Challenges

### Technical Constraints

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        CONSTRAINTS & MITIGATION STRATEGIES                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   CONSTRAINT 1: Backward Compatibility                                          │
│   ════════════════════════════════════                                          │
│                                                                                  │
│   Challenge:                                                                    │
│   • Existing integrations rely on HAPI FHIR JPA server endpoints                │
│   • External systems configured to hit fhir.openelis.org:8443                   │
│                                                                                  │
│   Mitigation:                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  • Expose facade at same /fhir/* path (oe.openelis.org/fhir/)       │       │
│   │  • Maintain identical FHIR R4 API contract                           │       │
│   │  • Support same search parameters                                    │       │
│   │  • Transition period: run both, redirect traffic gradually           │       │
│   │  • Nginx config update for routing                                   │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   CONSTRAINT 2: Transaction Support                                             │
│   ═════════════════════════════════                                             │
│                                                                                  │
│   Challenge:                                                                    │
│   • FHIR Transaction bundles require atomic all-or-nothing semantics            │
│   • Multiple entities across different services must be coordinated             │
│                                                                                  │
│   Mitigation:                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  • Use Spring @Transactional with REQUIRES_NEW propagation          │       │
│   │  • Implement custom TransactionProcessor service                     │       │
│   │  • Order operations: references resolved before dependents           │       │
│   │  • Rollback on any failure with meaningful error response            │       │
│   │  • Temporary ID resolution (urn:uuid → actual IDs)                   │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   CONSTRAINT 3: Search Performance                                              │
│   ════════════════════════════════                                              │
│                                                                                  │
│   Challenge:                                                                    │
│   • HAPI JPA has optimized search indexes (HFJ_SPIDX_* tables)                  │
│   • OpenELIS tables not designed for FHIR search patterns                       │
│                                                                                  │
│   Mitigation:                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  Phase 1 (Initial):                                                  │       │
│   │  • Use existing OpenELIS indexes where possible                      │       │
│   │  • Add composite indexes for common FHIR queries                     │       │
│   │  • Implement search via HQL with dynamic criteria                    │       │
│   │                                                                      │       │
│   │  Phase 2 (If needed):                                                │       │
│   │  • Create lightweight FHIR search index tables                       │       │
│   │  • Populate via Hibernate event listeners (async)                    │       │
│   │  • fhir_string_idx, fhir_token_idx, fhir_date_idx                   │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   CONSTRAINT 4: Complex FHIR Operations                                         │
│   ══════════════════════════════════════                                        │
│                                                                                  │
│   Challenge:                                                                    │
│   • FHIR operations like $everything, $validate, $expand not trivial            │
│   • IHE profile operations (mCSD) require specific implementations              │
│                                                                                  │
│   Mitigation:                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  • Prioritize operations based on actual usage                       │       │
│   │  • Patient/$everything: Aggregate related resources                  │       │
│   │  • $validate: Use HAPI FHIR validation module                        │       │
│   │  • IHE mCSD: Implement required Location/Organization queries        │       │
│   │  • Document unsupported operations in CapabilityStatement            │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   CONSTRAINT 5: Data Consistency During Migration                               │
│   ═══════════════════════════════════════════════                               │
│                                                                                  │
│   Challenge:                                                                    │
│   • Existing HAPI FHIR database has historical data                             │
│   • fhir_uuid columns may not match HAPI resource IDs                           │
│                                                                                  │
│   Mitigation:                                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  • Audit existing fhir_uuid population completeness                  │       │
│   │  • Migration script to ensure all entities have fhir_uuid            │       │
│   │  • Support lookup by both legacy HAPI ID and fhir_uuid               │       │
│   │  • Deprecation period for old resource IDs                           │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   CONSTRAINT 6: Constitution Compliance                                         │
│   ═════════════════════════════════════                                         │
│                                                                                  │
│   OpenELIS Constitution Requirements (must follow):                             │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  • Principle III: FHIR/IHE Standards Compliance                      │       │
│   │    → Must use HAPI FHIR R4 (v6.6.2)                                  │       │
│   │    → IHE Lab profiles for clinical resources                         │       │
│   │    → All entities need fhir_uuid UUID column                         │       │
│   │                                                                      │       │
│   │  • Principle IV: Layered Architecture                                │       │
│   │    → ResourceProvider → Service → DAO → Valueholder                  │       │
│   │    → @Transactional in services only                                 │       │
│   │    → Services compile all data within transaction                    │       │
│   │                                                                      │       │
│   │  • Principle V: Test-Driven Development                              │       │
│   │    → Unit tests for all providers                                    │       │
│   │    → Integration tests with FHIR validation                          │       │
│   │    → ORM validation tests                                            │       │
│   │                                                                      │       │
│   │  • Principle VI: Database Schema Management                          │       │
│   │    → Liquibase for any new tables/indexes                            │       │
│   │    → Rollback scripts required                                       │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Deployment Strategy

### GSOC Deployment: Parallel Operation

During and immediately after GSOC, the facade runs alongside HAPI with
configurable routing.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    GSOC DEPLOYMENT: PARALLEL OPERATION                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   CONFIGURATION-DRIVEN ROUTING                                                  │
│   ════════════════════════════                                                  │
│                                                                                  │
│   common.properties:                                                            │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  # Enable facade for specific resources (GSOC deliverables)         │       │
│   │  fhir.facade.enabled=true                                           │       │
│   │  fhir.facade.resources=Patient,Task,ServiceRequest,                 │       │
│   │                        DiagnosticReport,Observation                 │       │
│   │                                                                     │       │
│   │  # HAPI server still required for other resources                   │       │
│   │  org.openelisglobal.fhirstore.uri=https://fhir.openelis.org:8443/   │       │
│   │                                                                     │       │
│   │  # Fallback to HAPI if facade fails                                 │       │
│   │  fhir.facade.fallback.enabled=true                                  │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   REQUEST ROUTING FLOW                                                          │
│   ════════════════════                                                          │
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │                                                                     │       │
│   │   GET /fhir/Patient/123                                             │       │
│   │         │                                                           │       │
│   │         ▼                                                           │       │
│   │   ┌─────────────────┐                                               │       │
│   │   │ FhirRouterServlet│                                              │       │
│   │   └────────┬────────┘                                               │       │
│   │            │                                                        │       │
│   │            ▼                                                        │       │
│   │   ┌─────────────────┐                                               │       │
│   │   │ Is "Patient" in │                                               │       │
│   │   │ facade.resources?│                                              │       │
│   │   └────────┬────────┘                                               │       │
│   │            │                                                        │       │
│   │       YES  │  NO                                                    │       │
│   │            │                                                        │       │
│   │   ┌────────▼────────┐    ┌─────────────────────┐                    │       │
│   │   │  FHIR Facade    │    │  Proxy to HAPI JPA  │                    │       │
│   │   │  (OpenELIS)     │    │  Server             │                    │       │
│   │   └────────┬────────┘    └──────────┬──────────┘                    │       │
│   │            │                        │                               │       │
│   │            │  On Error + Fallback   │                               │       │
│   │            │  Enabled?              │                               │       │
│   │            └────────────────────────┤                               │       │
│   │                                     │                               │       │
│   │                                     ▼                               │       │
│   │                              ┌─────────────┐                        │       │
│   │                              │  Response   │                        │       │
│   │                              └─────────────┘                        │       │
│   │                                                                     │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   GSOC END STATE                                                                │
│   ══════════════                                                                │
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  Resource          │ Served By      │ Status                       │       │
│   │  ──────────────────┼────────────────┼────────────────────────────── │       │
│   │  Patient           │ FHIR Facade    │ ✅ GSOC Complete              │       │
│   │  Task              │ FHIR Facade    │ ✅ GSOC Complete              │       │
│   │  ServiceRequest    │ FHIR Facade    │ ✅ GSOC Complete              │       │
│   │  DiagnosticReport  │ FHIR Facade    │ ✅ GSOC Complete              │       │
│   │  Observation       │ FHIR Facade    │ ✅ GSOC Complete              │       │
│   │  Specimen          │ HAPI JPA       │ ⏳ Post-GSOC                  │       │
│   │  Practitioner      │ HAPI JPA       │ ⏳ Post-GSOC                  │       │
│   │  Organization      │ HAPI JPA       │ ⏳ Post-GSOC                  │       │
│   │  Location          │ HAPI JPA       │ ⏳ Post-GSOC                  │       │
│   │  Bundle/Transaction│ HAPI JPA       │ ⏳ Post-GSOC                  │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Post-GSOC Migration Strategy

### Long-Term: Full HAPI Removal

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                 POST-GSOC MIGRATION STRATEGY (Future Work)                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   STAGE 1: Complete Resource Coverage (Post-GSOC Phase 1)                       │
│   ═══════════════════════════════════════════════════════                       │
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  • Implement remaining resource providers:                          │       │
│   │    - Specimen, Practitioner, Organization, Location                 │       │
│   │  • Implement Transaction Bundle support                             │       │
│   │  • Implement advanced search features (_include, chaining)          │       │
│   │  • Achieve 100% resource parity with HAPI                           │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   STAGE 2: Traffic Migration (Post-GSOC Phase 2)                                │
│   ══════════════════════════════════════════════                                │
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  • Route 100% traffic to facade for all resources                   │       │
│   │  • Keep HAPI as cold standby (no active traffic)                    │       │
│   │  • Monitor for edge cases and issues                                │       │
│   │  • Run parallel validation for 2-4 weeks                            │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   STAGE 3: HAPI Decommission (Post-GSOC Phase 3)                                │
│   ══════════════════════════════════════════════                                │
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  • Remove HAPI container from docker-compose                        │       │
│   │  • Archive HFJ_* tables (optional, for compliance)                  │       │
│   │  • Update all documentation                                         │       │
│   │  • Reclaim ~500MB memory footprint                                  │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   CONFIGURATION CHANGES                                                         │
│   ═════════════════════                                                         │
│                                                                                  │
│   docker-compose.yml changes:                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  BEFORE:                                                            │       │
│   │  ────────                                                           │       │
│   │  services:                                                          │       │
│   │    fhir.openelis.org:                                               │       │
│   │      image: hapiproject/hapi:v6.6.0-tomcat  # REMOVE                │       │
│   │      ports: ["8444:8443"]                                           │       │
│   │                                                                     │       │
│   │  AFTER:                                                             │       │
│   │  ───────                                                            │       │
│   │  services:                                                          │       │
│   │    # fhir.openelis.org container REMOVED                            │       │
│   │    # FHIR served directly by oe.openelis.org at /fhir/*             │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
│   common.properties changes:                                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐       │
│   │  BEFORE:                                                            │       │
│   │  ────────                                                           │       │
│   │  org.openelisglobal.fhirstore.uri=https://fhir.openelis.org:8443/fhir/│      │
│   │                                                                     │       │
│   │  AFTER:                                                             │       │
│   │  ───────                                                            │       │
│   │  org.openelisglobal.fhir.facade.enabled=true                        │       │
│   │  # org.openelisglobal.fhirstore.uri REMOVED (not needed)            │       │
│   └─────────────────────────────────────────────────────────────────────┘       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Project Timeline

### Gantt Chart Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          PROJECT TIMELINE (350 Hours)                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   Week    1    2    3    4    5    6    7    8    9   10   11   12   13         │
│          ════════════════════════════════════════════════════════════════       │
│                                                                                  │
│   Phase 1: Foundation (80h)                                                     │
│          ████████████████                                                       │
│          │ Server setup │                                                       │
│          │ Security     │                                                       │
│          │ Interceptors │                                                       │
│          └──────────────┘                                                       │
│                                                                                  │
│   Phase 2: Core Providers (120h)                                                │
│                    ████████████████████████████████████████                     │
│                    │ Patient, Practitioner, Organization   │                    │
│                    │ Specimen, ServiceRequest, Task        │                    │
│                    └───────────────────────────────────────┘                    │
│                                                                                  │
│   Phase 3: Lab Results (80h)                                                    │
│                                              ██████████████████████             │
│                                              │ Observation       │              │
│                                              │ DiagnosticReport  │              │
│                                              │ Location          │              │
│                                              └───────────────────┘              │
│                                                                                  │
│   Phase 4: Advanced Features (50h)                                              │
│                                                        ████████████████         │
│                                                        │ Transactions │         │
│                                                        │ Search opts  │         │
│                                                        └──────────────┘         │
│                                                                                  │
│   Phase 5: Migration & Testing (20h)                                            │
│                                                                  ████████       │
│                                                                  │ Test │       │
│                                                                  │ Docs │       │
│                                                                  └──────┘       │
│                                                                                  │
│   ───────────────────────────────────────────────────────────────────────────── │
│                                                                                  │
│   MILESTONES:                                                                   │
│                                                                                  │
│   M1 (Week 2):  CapabilityStatement returns valid FHIR R4 metadata              │
│   M2 (Week 5):  Patient/Practitioner/Organization CRUD operational              │
│   M3 (Week 7):  Specimen/ServiceRequest/Task CRUD operational                   │
│   M4 (Week 10): All resource providers complete                                 │
│   M5 (Week 11): Transaction bundle support working                              │
│   M6 (Week 13): Migration complete, HAPI container removed                      │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Success Criteria

### GSOC Acceptance Criteria

| Criterion                | Description                                              | Validation Method       |
| ------------------------ | -------------------------------------------------------- | ----------------------- |
| **FHIR R4 Compliance**   | Priority 1 resources pass FHIR validation                | HAPI FHIR Validator     |
| **Functional Parity**    | Facade returns same data as HAPI for supported resources | Comparison test suite   |
| **Performance**          | Response time ≤ 200ms for single resource read           | Performance benchmarks  |
| **Parallel Operation**   | Facade and HAPI run simultaneously without conflicts     | Integration tests       |
| **Configurable Routing** | Traffic can be routed per-resource type via config       | Configuration tests     |
| **Fallback Mechanism**   | Graceful fallback to HAPI when facade errors             | Failure injection tests |
| **Search Functionality** | Core search parameters work for Priority 1 resources     | Search test matrix      |
| **Test Coverage**        | >70% code coverage on facade layer                       | JaCoCo report           |
| **Documentation**        | Developer guide for adding new providers                 | Documentation review    |
| **Handoff Ready**        | Clear roadmap for remaining resources                    | Roadmap document        |

### Post-GSOC Success Criteria (Future)

| Criterion                | Description                            | Validation Method         |
| ------------------------ | -------------------------------------- | ------------------------- |
| **Full Resource Parity** | All FHIR resources served by facade    | Feature matrix            |
| **Transaction Support**  | Transaction bundles work correctly     | Transaction test suite    |
| **HAPI Removal**         | HAPI container removed from deployment | Docker compose validation |
| **Resource Efficiency**  | Memory footprint reduced by ~500MB     | Container metrics         |

### GSOC Key Deliverables

1. **Code Deliverables**

   - HAPI FHIR Plain Server configuration (embedded in OpenELIS)
   - FhirRouterService for configurable traffic routing
   - 5 Priority 1 Resource Providers:
     - PatientResourceProvider
     - TaskResourceProvider
     - ServiceRequestResourceProvider
     - DiagnosticReportResourceProvider
     - ObservationResourceProvider
   - Enhanced FhirTransformService (bidirectional transforms)
   - Search handlers for Priority 1 resources

2. **Test Deliverables**

   - Unit tests for all implemented providers (JUnit 4 + Mockito)
   - Comparison test suite (facade vs HAPI response validation)
   - FHIR validation tests using HAPI FHIR Validator
   - Performance benchmark suite

3. **Documentation Deliverables**

   - API documentation for implemented resources
   - Configuration guide for parallel operation
   - Developer guide for adding new resource providers
   - Post-GSOC roadmap for remaining resources

4. **Infrastructure Deliverables**
   - Updated docker-compose files (facade enabled alongside HAPI)
   - Configuration properties for routing control
   - Logging and monitoring integration

### Post-GSOC Deliverables (Future Work)

1. **Additional Resource Providers**

   - SpecimenResourceProvider
   - PractitionerResourceProvider
   - OrganizationResourceProvider
   - LocationResourceProvider

2. **Advanced Features**

   - Transaction bundle processor
   - Advanced search (\_include, \_revinclude, chaining)
   - FHIR Operations ($everything, $validate)

3. **Migration Completion**
   - Full traffic cutover to facade
   - HAPI container removal
   - HFJ\_\* table archival/removal

---

## Appendix: Sample Code Structure

### Resource Provider Example (PatientResourceProvider.java)

```java
package org.openelisglobal.fhir.facade.provider;

import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.*;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatientResourceProvider implements IResourceProvider {

    @Autowired
    private PatientService patientService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }

    @Read
    public Patient read(@IdParam IdType id) {
        org.openelisglobal.patient.valueholder.Patient oePatient =
            patientService.getPatientByFhirUuid(id.getIdPart());
        return fhirTransformService.transformToFhirPatient(oePatient);
    }

    @Search
    public List<Patient> searchByIdentifier(
            @RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam identifier) {
        // Implementation
    }

    @Create
    public MethodOutcome create(@ResourceParam Patient patient) {
        // Transform FHIR → OpenELIS, save, return outcome
    }

    @Update
    public MethodOutcome update(@IdParam IdType id, @ResourceParam Patient patient) {
        // Transform FHIR → OpenELIS, update, return outcome
    }
}
```

---

## Conclusion

This proposal outlines a comprehensive plan to create a native FHIR facade layer
for OpenELIS Global, eliminating the dependency on an external HAPI FHIR JPA
server. The benefits include:

- **Simplified Architecture**: Single source of truth (OpenELIS database)
- **Improved Data Consistency**: No sync issues between databases
- **Reduced Operational Overhead**: One less container to manage
- **Better Performance**: No HTTP overhead for local FHIR operations
- **Maintained Interoperability**: Full FHIR R4 compliance for external systems

The 350-hour timeline is achievable with the phased approach, with clear
milestones and deliverables at each stage.
