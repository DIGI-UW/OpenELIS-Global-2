# M8: Integration, Testing & Deployment — LLM Implementation Prompt

> Run from: `tools/astm-http-bridge/` (bridge submodule root)  
> Branch: `feat/universal-bridge-integration` (create from `develop` **after M7
> is merged**)  
> Depends on: M7 merged (bridge PR:
> [openelis-analyzer-bridge#11](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/11))

---

## Goal

Make the Universal Analyzer Bridge production-ready by adding:

- **Observability**: Prometheus metrics via Micrometer + new health indicators
- **Docker-based E2E**: exercise all 5 transports end-to-end in a repeatable
  compose environment
- **Documentation**: README + configuration reference updates

Tag **v3.0.0** when complete.

---

## Current Reality (What Exists vs What’s Missing)

### Exists already

- `spring-boot-starter-actuator` is present
- Docker compose files exist (`docker-compose.yml`, `docker-compose.test.yml`,
  dev variants)
- One health indicator exists: `health/HTTPForwardServerHealthIndicator.java`
- M7 unified routing is implemented and tested (see `UnifiedRoutingTest`)

### Missing (M8 scope)

- `micrometer-registry-prometheus` dependency (needed for
  `/actuator/prometheus`)
- Metrics instrumentation (recommended single instrumentation point:
  `MessageNormalizer.process()`)
- Health indicators for MLLP, Serial, and File Watcher
- Docker E2E runners that validate:
  - ASTM TCP → route → HTTP forward
  - HL7 MLLP → route → HTTP forward
  - File drop (CSV) → route → HTTP forward
  - HTTP `/input` → route → HTTP forward

---

## M8 Tasks (T12–T25)

### T12: Add Prometheus Registry

**File**: `pom.xml`

Add:

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Do **not** specify a version (Spring Boot BOM manages it).

---

### T13: Create `MetricsService` + wire into `MessageNormalizer`

**New file**: `src/main/java/org/itech/ahb/metrics/MetricsService.java`

Expose at minimum:

- `bridge.messages.received` (Counter; tags: `protocol`, `transport`)
- `bridge.messages.routed` (Counter; tags: `protocol`, `transport`, `result`)
- `bridge.messages.routing.duration` (Timer; tags: `protocol`, `transport`)

**Wire point**: `MessageNormalizer.process(MessageEnvelope)`

Implementation guidance:

- Inject `MetricsService` into `MessageNormalizer` as
  `@Autowired(required = false)` (so tests/configs that don’t include Micrometer
  still work).
- Use `Timer.Sample` to measure end-to-end routing latency inside `process()`.

---

### T14–T16: Add Health Indicators

Create new health indicators (all should degrade gracefully when the listener
bean is absent):

- **T14**: `health/MLLPHealthIndicator.java` (uses
  `HapiMLLPListener.isRunning()`)
- **T15**: `health/SerialHealthIndicator.java` (uses
  `SerialPortListener.getOpenPorts()` + `getPortStatus()`)
- **T16**: `health/FileWatcherHealthIndicator.java`
  - Add `public boolean isRunning()` accessor to `file/FileWatcher.java`
    (currently `running` is private)

Use `@ConditionalOnEnabledHealthIndicator("<name>")` and
`@Autowired(required = false)`.

---

### T17–T18: Docker Compose Updates

- **T17** (`docker-compose.yml`): expose MLLP port, add file watcher volumes,
  document serial device mount placeholder.
- **T18** (`docker-compose.test.yml`): ensure a repeatable E2E environment
  exists for all 5 transports.

Pragmatic approach for M8 E2E:

- Use a **capture server** as “OpenELIS” target (already exists) and verify:
  - HTTP request path (`/analyzer/{hl7|csv|astm|raw}`)
  - Headers `X-Source-Protocol`, `X-Source-Transport`, `X-Source-Id`, optional
    `X-Analyzer-Id`
  - Body contains original payload

If the capture server does not currently provide a query API, enhance it
minimally (store last N requests and expose via `GET /requests`).

---

### T19–T22: E2E Tests (Docker)

Implement E2E tests that run in Docker (scripts or a small test container):

- **T19**: MLLP HL7 → expect forward to `/analyzer/hl7` with headers
- **T20**: File drop CSV → expect forward to `/analyzer/csv`
- **T21**: HTTP `/input` POST → expect forward to `/analyzer/{protocol}`
- **T22**: ASTM TCP → expect forward to `/analyzer/astm` and include source IP
  header

---

### T23–T24: README + `configuration.yml`

- Update README with:
  - architecture diagram (include metrics + health endpoints)
  - full configuration reference (`bridge.*`, plus legacy `org.itech.ahb.*`
    where still required)
  - Docker deployment instructions (ports/volumes)
  - monitoring section (`/actuator/prometheus`, suggested dashboards)
- Update `configuration.yml` to:
  - expose `prometheus` actuator endpoint
  - enable the new health indicators
  - document MLLP config keys

---

### T25: Version Bump + Tag

Update versions to `3.0.0` (main + `astm-http-lib`), then tag `v3.0.0` **after
CI is green**.

---

## Verification Commands

From `tools/astm-http-bridge/`:

```bash
mvn clean test
mvn verify
```

Local actuator sanity check:

```bash
mvn spring-boot:run
curl http://localhost:8443/actuator/health
curl http://localhost:8443/actuator/prometheus | head
```

Docker E2E:

```bash
docker compose -f docker-compose.test.yml up --build
```
